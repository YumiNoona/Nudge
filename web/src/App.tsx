import { useState, useEffect } from 'react';
import { useLiveQuery } from 'dexie-react-hooks';
import { db, generateId, formatAmount } from './lib/db';
import type { Transaction, Account, Category, TransactionType } from './lib/db';
import { levelTitle, levelProgress, categoryColor } from './lib/engine';
import { seedDefaults } from './lib/seed';
import { categorizationEngine } from './lib/categorization';
import { initSecureStorage } from './lib/secureDb';
import NeedsReviewScreen from './components/NeedsReviewScreen';
import MerchantAliasScreen from './components/MerchantAliasScreen';
import AnalyticsPage from './components/AnalyticsPage';
import AchievementsScreen from './components/AchievementsScreen';
import ChallengesScreen from './components/ChallengesScreen';
import SavingsGoalsScreen from './components/SavingsGoalsScreen';
import CSVImportScreen from './components/CSVImportScreen';
import BackupScreen from './components/BackupScreen';
import SyncSettingsScreen from './components/SyncSettingsScreen';
import Onboarding from './components/Onboarding';
import ProfileScreen from './components/ProfileScreen';
import { motion, AnimatePresence } from 'framer-motion';
import {
  IconLayoutDashboard, IconChecklist, IconChartBar, IconTrophy, IconTarget,
  IconPigMoney, IconFileImport, IconDatabase, IconTag, IconPlus,
  IconSun, IconMoon, IconFlame, IconWallet, IconUser,
} from './lib/icons';
import { GradientHeroCard } from './components/ui/GradientHeroCard';
import { RingStatCard } from './components/ui/RingStatCard';
import { TransactionRow } from './components/ui/TransactionRow';

type Tab = 'dashboard' | 'review' | 'analytics' | 'profile' | 'achievements' | 'challenges' | 'goals' | 'csv' | 'backup' | 'merchants' | 'sync';

const NAV_ITEMS: { id: Tab; label: string; icon: typeof IconLayoutDashboard; section?: string }[] = [
  { id: 'dashboard', label: 'Dashboard', icon: IconLayoutDashboard, section: 'Main' },
  { id: 'review', label: 'Review', icon: IconChecklist },
  { id: 'analytics', label: 'Analytics', icon: IconChartBar },
  { id: 'achievements', label: 'Badges', icon: IconTrophy, section: 'Gamify' },
  { id: 'challenges', label: 'Challenges', icon: IconTarget },
  { id: 'goals', label: 'Goals', icon: IconPigMoney },
  { id: 'csv', label: 'Import', icon: IconFileImport, section: 'Data' },
  { id: 'backup', label: 'Backup', icon: IconDatabase },
  { id: 'merchants', label: 'Merchants', icon: IconTag },
];

export default function App() {
  const [dark, setDark] = useState(() => window.matchMedia('(prefers-color-scheme: dark)').matches);
  const [showAddModal, setShowAddModal] = useState(false);
  const [tab, setTab] = useState<Tab>('dashboard');
  const [mobileNav, setMobileNav] = useState(false);
  const [onboarded, setOnboarded] = useState(() => localStorage.getItem('nudge_onboarded') === '1');
  const [profileName, setProfileName] = useState(() => localStorage.getItem('nudge_profile_name') || 'Friend');

  useEffect(() => { document.documentElement.classList.toggle('dark', dark); }, [dark]);

  useEffect(() => {
    const navigate = (event: Event) => setTab((event as CustomEvent<Tab>).detail);
    window.addEventListener('nudge-nav', navigate);
    return () => window.removeEventListener('nudge-nav', navigate);
  }, []);

  useEffect(() => { window.scrollTo({ top: 0, behavior: 'smooth' }); }, [tab, onboarded]);

  useEffect(() => {
    initSecureStorage().then(() => {
      seedDefaults().then(() => {
        db.gamificationProfile.toCollection().first().then((profile) => {
          if (!profile) {
            db.gamificationProfile.add({
              userId: generateId(), xpTotal: 0, level: 1, currentStreakDays: 0,
              longestStreakDays: 0, lastActivityDate: Date.now(), badgesJson: '[]',
              challengesJson: '[]', streakFreezes: 0, consistentDays: 0,
            });
          }
        });
      });
    });
  }, []);

  const transactions = useLiveQuery(() => db.transactions.orderBy('timestampEpoch').reverse().toArray()) ?? [];
  const categories = useLiveQuery(() => db.categories.filter(c => !c.isArchived).toArray()) ?? [];
  const accounts = useLiveQuery(() => db.accounts.filter(a => a.isActive).toArray()) ?? [];
  const budgets = useLiveQuery(() => db.budgets.toArray()) ?? [];
  const gamification = useLiveQuery(() => db.gamificationProfile.toCollection().first());
  const merchantAliases = useLiveQuery(() => db.merchantAliases.toArray()) ?? [];
  const needsReviewCount = transactions.filter(t => !t.isReviewed).length;

  const handleAddTransaction = async (amountCents: number, type: TransactionType, merchantRaw: string, accountId: string, categoryId: string | null, note: string | null) => {
    await db.transactions.add({
      id: generateId(), amountCents, type, currency: 'INR', merchantRaw,
      merchantNormalized: null, categoryId, subcategoryId: null, accountId,
      source: 'manual', sourceRawText: null, confidenceScore: 1, isReviewed: true,
      isRecurring: false, recurringGroupId: null, note, tags: [],
      timestampEpoch: Date.now(), createdAt: Date.now(), updatedAt: Date.now(),
    });
    if (gamification) {
      await db.gamificationProfile.update(gamification.userId, { xpTotal: gamification.xpTotal + 5 });
    }
  };

  const handleAddAlias = async (rawPattern: string, normalizedName: string) => {
    await db.merchantAliases.add({ id: generateId(), rawPattern, normalizedName, suggestedCategoryId: null, createdAt: Date.now() });
  };
  const handleDeleteAlias = async (id: string) => { await db.merchantAliases.delete(id); };

  const completeOnboarding = ({ name, theme }: { name: string; currency: string; theme: 'system' | 'light' | 'dark' }) => {
    localStorage.setItem('nudge_onboarded', '1');
    localStorage.setItem('nudge_profile_name', name);
    setProfileName(name);
    if (theme !== 'system') setDark(theme === 'dark');
    setOnboarded(true);
  };

  if (!onboarded) return <Onboarding onComplete={completeOnboarding} />;

  // --- Sidebar ---
  const sidebar = (
    <aside className="w-60 flex-shrink-0 border-r border-content-tertiary/10 flex flex-col h-screen sticky top-0 bg-surface-base/80 backdrop-blur-sm">
      <div className="p-5 flex items-center gap-3">
        <div className="w-9 h-9 rounded-xl bg-[#FF6B5B] flex items-center justify-center">
          <IconWallet size={18} className="text-white" />
        </div>
        <div>
          <h1 className="text-lg font-bold text-content-primary leading-tight">Nudge</h1>
          <p className="text-micro text-content-tertiary">Finance, warmly</p>
        </div>
      </div>

      <nav className="flex-1 px-3 py-2 space-y-6 overflow-y-auto">
        {['Main', 'Gamify', 'Data'].map(section => {
          const items = NAV_ITEMS.filter(i => i.section === section || (!i.section && section === 'Main' && i.id === 'dashboard') || (!i.section && section === 'Gamify' && ['review', 'analytics'].includes(i.id)) || (!i.section && section === 'Gamify' && ['achievements', 'challenges', 'goals'].includes(i.id)) || (!i.section && section === 'Data' && ['csv', 'backup', 'merchants'].includes(i.id)));
          if (items.length === 0) return null;
          return (
            <div key={section}>
              <p className="px-3 mb-1 text-micro font-semibold text-content-tertiary uppercase tracking-wider">{section}</p>
              <div className="space-y-0.5">
                {items.map(({ id, label, icon: Icon }) => {
                  const active = tab === id;
                  const badge = id === 'review' ? needsReviewCount : 0;
                  return (
                    <button
                      key={id}
                      onClick={() => { setTab(id); setMobileNav(false); }}
                      className={`w-full flex items-center gap-3 px-3 py-2.5 rounded-lg text-sm font-medium transition-all duration-200 ${
                        active
                          ? 'bg-[#FFE8E4] text-[#FF6B5B]'
                          : 'text-content-secondary hover:bg-surface-raised hover:text-content-primary'
                      }`}
                    >
                      <Icon size={19} stroke={active ? 2 : 1.6} />
                      <span className="flex-1 text-left">{label}</span>
                      {badge > 0 && (
                        <span className="px-2 py-0.5 rounded-full bg-[#FF6B5B] text-white text-xs font-bold">{badge}</span>
                      )}
                    </button>
                  );
                })}
              </div>
            </div>
          );
        })}
      </nav>

      <div className="p-4 border-t border-content-tertiary/10 space-y-3">
        {gamification && (
          <div className="flex items-center gap-3 px-2">
            <div className="w-8 h-8 rounded-lg bg-[#FFE8E4] flex items-center justify-center">
              <IconFlame size={16} className="text-[#FF6B5B]" />
            </div>
            <div className="flex-1 min-w-0">
              <div className="text-xs font-semibold text-content-primary">Lv.{gamification.level}</div>
              <div className="w-full h-1 bg-surface-raised rounded-full mt-1">
                <div className="h-full bg-[#FF6B5B] rounded-full transition-all" style={{ width: `${levelProgress(gamification.xpTotal) * 100}%` }} />
              </div>
            </div>
          </div>
        )}
        <button
          onClick={() => setDark(!dark)}
          className="w-full flex items-center gap-3 px-3 py-2 rounded-lg text-sm text-content-secondary hover:bg-surface-raised transition-colors"
        >
          {dark ? <IconSun size={18} /> : <IconMoon size={18} />}
          {dark ? 'Light mode' : 'Dark mode'}
        </button>
      </div>
    </aside>
  );

  // --- Screen routing ---
  const screenProps = { transactions, categories, accounts, budgets, merchantAliases, gamification, needsReviewCount };

  return (
    <div className="min-h-screen app-canvas flex">
      {/* Desktop sidebar */}
      <div className="hidden">{sidebar}</div>

      {/* Mobile overlay */}
      <AnimatePresence>
        {mobileNav && (
          <motion.div
            initial={{ opacity: 0 }} animate={{ opacity: 1 }} exit={{ opacity: 0 }}
            className="fixed inset-0 z-50 lg:hidden bg-black/40" onClick={() => setMobileNav(false)}
          >
            <motion.div
              initial={{ x: -280 }} animate={{ x: 0 }} exit={{ x: -280 }}
              transition={{ type: 'spring', damping: 25, stiffness: 300 }}
              className="w-60 h-full" onClick={e => e.stopPropagation()}
            >
              {sidebar}
            </motion.div>
          </motion.div>
        )}
      </AnimatePresence>

      {/* Main content */}
      <div className="flex-1 flex flex-col min-h-screen pb-28">
        {/* Mobile header */}
        <header className="app-header flex items-center justify-between px-5 py-4 sticky top-0 z-30">
          <div className="flex items-center gap-2">
            <div className="w-7 h-7 rounded-lg bg-[#FF6B5B] flex items-center justify-center"><IconWallet size={14} className="text-white" /></div>
            <span className="font-bold text-content-primary tracking-tight">Nudge</span>
          </div>
          <button onClick={() => setTab('profile')} className="header-avatar" aria-label="Open profile">{profileName.slice(0, 1).toUpperCase()}</button>
        </header>

        {/* Screen content */}
        <div className="flex-1">
          {tab !== 'dashboard' && (
            <div className="p-4 lg:p-6">
              {tab === 'review' && <NeedsReviewScreen onBack={() => setTab('dashboard')} />}
              {tab === 'analytics' && <AnalyticsPage transactions={transactions} categories={categories} budgets={budgets} onBack={() => setTab('dashboard')} />}
              {tab === 'achievements' && <AchievementsScreen gamificationProfile={gamification ?? null} onBack={() => setTab('dashboard')} />}
              {tab === 'challenges' && <ChallengesScreen categories={categories} transactions={transactions} onBack={() => setTab('dashboard')} />}
              {tab === 'goals' && <SavingsGoalsScreen onBack={() => setTab('dashboard')} />}
              {tab === 'csv' && <CSVImportScreen categories={categories} accounts={accounts} onBack={() => setTab('dashboard')} onImport={async (txns) => {
                for (const t of txns) await db.transactions.add({ id: generateId(), amountCents: t.amountCents, type: t.type, currency: 'INR', merchantRaw: t.merchantRaw, merchantNormalized: null, categoryId: t.categoryId, subcategoryId: null, accountId: t.accountId, source: 'csv_import', sourceRawText: null, confidenceScore: 1, isReviewed: !!t.categoryId, isRecurring: false, recurringGroupId: null, note: t.note, tags: [], timestampEpoch: t.dateEpoch, createdAt: Date.now(), updatedAt: Date.now() });
                setTab('dashboard');
              }} />}
              {tab === 'backup' && <BackupScreen onBack={() => setTab('dashboard')} />}
              {tab === 'merchants' && <MerchantAliasScreen aliases={merchantAliases} onAdd={handleAddAlias} onDelete={handleDeleteAlias} onBack={() => setTab('dashboard')} />}
              {tab === 'sync' && <SyncSettingsScreen onBack={() => setTab('profile')} />}
              {tab === 'profile' && <ProfileScreen name={profileName} dark={dark} level={gamification?.level ?? 1} xp={gamification?.xpTotal ?? 0} onToggleTheme={() => setDark(!dark)} onNavigate={setTab} onResetOnboarding={() => { localStorage.removeItem('nudge_onboarded'); setOnboarded(false); }} />}
            </div>
          )}

          {tab === 'dashboard' && <DashboardView {...screenProps} onAddTransaction={handleAddTransaction} showAddModal={showAddModal} setShowAddModal={setShowAddModal} />}
        </div>
      </div>
      <FloatingDock tab={tab} reviewCount={needsReviewCount} onNavigate={setTab} onAdd={() => { setTab('dashboard'); setShowAddModal(true); }} />
    </div>
  );
}

function FloatingDock({ tab, reviewCount, onNavigate, onAdd }: { tab: Tab; reviewCount: number; onNavigate: (tab: Tab) => void; onAdd: () => void }) {
  const items = [
    ['dashboard', 'Home', IconLayoutDashboard],
    ['review', 'Review', IconChecklist],
    ['add', 'Add', IconPlus],
    ['analytics', 'Insights', IconChartBar],
    ['profile', 'Profile', IconUser],
  ] as const;
  return (
    <nav className="floating-dock" aria-label="Primary navigation">
      {items.map(([id, label, Icon]) => {
        const active = id !== 'add' && tab === id;
        if (id === 'add') return <button key={id} onClick={onAdd} className="dock-add" aria-label="Add transaction"><Icon size={23} stroke={2.2} /><span>{label}</span></button>;
        return (
          <button key={id} onClick={() => onNavigate(id)} className={active ? 'active' : ''}>
            {active && <motion.span layoutId="dock-active" className="dock-active-bg" transition={{ type: 'spring', stiffness: 420, damping: 32 }} />}
            <Icon size={20} stroke={active ? 2.1 : 1.6} /><span>{label}</span>
            {id === 'review' && reviewCount > 0 && <b className="dock-badge">{reviewCount}</b>}
          </button>
        );
      })}
    </nav>
  );
}

// ─── Dashboard View ────────────────────────────────────────────────

function DashboardView(props: {
  transactions: Transaction[]; categories: Category[]; accounts: Account[];
  budgets: any[]; gamification: any; needsReviewCount: number;
  onAddTransaction: (a: number, t: TransactionType, m: string, ac: string, c: string | null, n: string | null) => void;
  showAddModal: boolean; setShowAddModal: (v: boolean) => void;
}) {
  const { transactions, categories, gamification, needsReviewCount, showAddModal, setShowAddModal, onAddTransaction, accounts, budgets } = props;

  const now = new Date();
  const thisMonth = now.getMonth();
  const thisYear = now.getFullYear();
  const hour = now.getHours();
  const greeting = hour < 12 ? 'morning' : hour < 17 ? 'afternoon' : 'evening';
  const username = gamification ? `Lv.${gamification.level} · ${levelTitle(gamification.level)}` : '';

  const monthTxns = transactions.filter(t => {
    const d = new Date(t.timestampEpoch);
    return d.getMonth() === thisMonth && d.getFullYear() === thisYear;
  });
  const spend = monthTxns.filter(t => t.type === 'debit').reduce((s, t) => s + t.amountCents, 0);

  const lastMonth = thisMonth === 0 ? 11 : thisMonth - 1;
  const lastYear = thisMonth === 0 ? thisYear - 1 : thisYear;
  const lastMonthTxns = transactions.filter(t => {
    const d = new Date(t.timestampEpoch);
    return d.getMonth() === lastMonth && d.getFullYear() === lastYear;
  });
  const lastMonthSpend = lastMonthTxns.filter(t => t.type === 'debit').reduce((s, t) => s + t.amountCents, 0);

  let delta: string | undefined;
  let deltaDown = false;
  if (lastMonthSpend > 0) {
    const pct = Math.abs(Math.round(((spend - lastMonthSpend) / lastMonthSpend) * 100));
    if (spend < lastMonthSpend) {
      delta = `${pct}%`;
      deltaDown = false;
    } else if (spend > lastMonthSpend) {
      delta = `${pct}%`;
      deltaDown = true;
    }
  }

  const expenseCategories = categories.filter(c => c.type === 'expense').slice(0, 5);

  const expenseCategoryStats = expenseCategories.map(cat => {
    const spentInCategory = monthTxns.filter(t => t.type === 'debit' && t.categoryId === cat.id).reduce((s, t) => s + t.amountCents, 0);
    const budget = budgets.find((b: any) => b.categoryId === cat.id);
    const budgetAmt = budget?.amountCents || cat.monthlyBudgetCents || 0;
    const progress = budgetAmt > 0 ? Math.min(100, Math.max(0, Math.round((spentInCategory / budgetAmt) * 100))) : 0;
    const color: 'green' | 'amber' | 'coral' = progress < 50 ? 'green' : progress < 80 ? 'amber' : 'coral';
    return { cat, spentInCategory, budgetAmt, progress, color };
  });

  return (
    <div className="p-4 lg:p-8 space-y-5 max-w-4xl mx-auto">
      {/* 1. Greeting row */}
      <div className="flex items-center justify-between">
        <p className="text-xs text-ink-mute">Good {greeting}</p>
        {gamification && (
          <p className="text-sm text-ink-1">{username}</p>
        )}
      </div>

      {/* 2. GradientHeroCard */}
      <GradientHeroCard
        value={`₹${formatAmount(spend)}`}
        label="Spent this month"
        delta={delta}
        deltaDown={delta ? deltaDown : undefined}
        pills={[
          { label: 'Add income', onClick: () => setShowAddModal(true) },
          { label: 'View budget', onClick: () => {} },
        ]}
      />

      {/* 3. RingStatCard row */}
      {expenseCategoryStats.length > 0 && (
        <div className="flex gap-3 overflow-x-auto pb-1 -mx-1 px-1">
          {expenseCategoryStats.map(({ cat, spentInCategory, budgetAmt, progress, color }) => (
            <RingStatCard
              key={cat.id}
              progress={progress}
              label={cat.name}
              subtext={budgetAmt > 0 ? `₹${formatAmount(spentInCategory)} / ₹${formatAmount(budgetAmt)}` : `₹${formatAmount(spentInCategory)}`}
              icon={cat.icon || undefined}
              color={color}
              size="sm"
            />
          ))}
        </div>
      )}

      {/* 4. Needs review callout */}
      {needsReviewCount > 0 && (
        <motion.button
          whileHover={{ scale: 1.01 }}
          whileTap={{ scale: 0.99 }}
          onClick={() => window.dispatchEvent(new CustomEvent('nudge-nav', { detail: 'review' }))}
          className="w-full flex items-center gap-3 p-4 rounded-card bg-amber-bg text-left"
        >
          <IconChecklist size={20} className="text-amber-1" />
          <div className="flex-1">
            <p className="text-sm font-semibold text-amber-1">{needsReviewCount} transaction{needsReviewCount > 1 ? 's' : ''} need review</p>
            <p className="text-xs text-amber-1/70">Swipe to categorize and earn XP →</p>
          </div>
        </motion.button>
      )}

      {/* 5. Recent activity */}
      <div>
        <h3 className="text-sm font-bold text-ink-soft uppercase tracking-wide mb-3">Recent activity</h3>
        {transactions.length === 0 ? (
          <div className="flex flex-col items-center justify-center py-16 gap-3 rounded-card bg-[var(--surface)] shadow-card">
            <IconWallet size={32} className="text-ink-mute" />
            <p className="text-sm text-ink-soft">No transactions yet</p>
            <p className="text-xs text-ink-mute">Tap + to add your first entry</p>
          </div>
        ) : (
          <div className="rounded-card bg-[var(--surface)] shadow-card px-4">
            {transactions.slice(0, 15).map(txn => {
              const cat = categories.find(c => c.id === txn.categoryId);
              const catIndex = cat ? categories.indexOf(cat) : -1;
              return (
                <TransactionRow
                  key={txn.id}
                  icon={cat?.icon || '💳'}
                  merchant={txn.merchantRaw}
                  subtext={`${cat?.name || 'Uncategorized'} · ${new Date(txn.timestampEpoch).toLocaleDateString('en-IN', { day: 'numeric', month: 'short' })}`}
                  amount={`${txn.type === 'debit' ? '−' : '+'}₹${formatAmount(txn.amountCents)}`}
                  isExpense={txn.type === 'debit'}
                  categoryColor={cat?.color || categoryColor(catIndex >= 0 ? catIndex : 0)}
                />
              );
            })}
          </div>
        )}
      </div>

      {/* Add modal */}
      <AnimatePresence>
        {showAddModal && (
          <AddModal categories={categories} accounts={accounts} onClose={() => setShowAddModal(false)} onAdd={onAddTransaction} />
        )}
      </AnimatePresence>
    </div>
  );
}

// ─── Add Transaction Modal ─────────────────────────────────────────

function AddModal({ categories, accounts, onClose, onAdd }: {
  categories: Category[]; accounts: Account[];
  onClose: () => void;
  onAdd: (a: number, t: TransactionType, m: string, ac: string, c: string | null, n: string | null) => void;
}) {
  const [amountStr, setAmountStr] = useState('');
  const [merchant, setMerchant] = useState('');
  const [note, setNote] = useState('');
  const [type, setType] = useState<TransactionType>('debit');
  const [catId, setCatId] = useState<string | null>(null);
  const [acctId, setAcctId] = useState<string | null>(accounts[0]?.id ?? null);

  const amount = parseInt(amountStr.replace(/\D/g, '') || '0') * 100;
  const valid = amount > 0 && merchant.trim() && acctId;

  return (
    <motion.div
      initial={{ opacity: 0 }} animate={{ opacity: 1 }} exit={{ opacity: 0 }}
      className="fixed inset-0 bg-black/30 backdrop-blur-sm flex items-end lg:items-center justify-center z-50"
      onClick={onClose}
    >
      <motion.div
        initial={{ y: 100, opacity: 0 }} animate={{ y: 0, opacity: 1 }} exit={{ y: 100, opacity: 0 }}
        transition={{ type: 'spring', damping: 25, stiffness: 300 }}
        className="bg-[var(--surface)] w-full max-w-md rounded-t-3xl lg:rounded-3xl p-6 max-h-[85vh] overflow-y-auto border border-content-tertiary/5"
        onClick={e => e.stopPropagation()}
        style={{ boxShadow: '0 -8px 40px rgba(0,0,0,0.12)' }}
      >
        <div className="w-10 h-1 bg-content-tertiary/30 rounded-full mx-auto mb-5" />
        <h3 className="text-lg font-bold text-content-primary mb-5">Add transaction</h3>

        {/* Amount */}
        <div className="flex items-baseline gap-1 mb-1">
          <span className="text-2xl font-bold text-content-tertiary">₹</span>
          <input
            type="text" inputMode="numeric" placeholder="0"
            value={amountStr}
            onChange={e => setAmountStr(e.target.value)}
            className="flex-1 text-3xl font-bold font-mono bg-transparent outline-none text-content-primary placeholder:text-content-tertiary/50"
            autoFocus
          />
        </div>
        <div className="flex gap-2 mb-5">
          {(['debit', 'credit'] as TransactionType[]).map(t => (
            <button key={t} onClick={() => setType(t)}
              className={`px-4 py-1.5 rounded-lg text-xs font-semibold transition-all ${
                type === t ? 'bg-[#FFE8E4] text-[#FF6B5B]' : 'bg-surface-raised text-content-secondary hover:text-content-primary'
              }`}
            >{t === 'debit' ? 'Expense' : 'Income'}</button>
          ))}
        </div>

        {/* Merchant */}
        <input
          placeholder="What was this for?" value={merchant}
          onChange={e => setMerchant(e.target.value)}
          className="w-full px-4 py-3 bg-surface-raised rounded-xl outline-none text-sm text-content-primary placeholder:text-content-tertiary mb-4 border border-transparent focus:border-[#FF6B5B]/30 transition-colors"
        />

        {/* Categories */}
        <p className="text-xs font-semibold text-content-tertiary uppercase tracking-wider mb-2">Category</p>
        <div className="grid grid-cols-4 gap-2 mb-4">
          {categories.filter(c => c.type === 'expense').slice(0, 12).map(c => (
            <button key={c.id} onClick={() => setCatId(c.id)}
              className={`flex flex-col items-center gap-1 p-2.5 rounded-xl text-center transition-all ${
                catId === c.id ? 'bg-[#FFE8E4] ring-2 ring-[#FF6B5B]/30' : 'bg-surface-raised hover:bg-surface-raised/70'
              }`}
            >
              <span className="text-lg">{c.icon || '📁'}</span>
              <span className="text-micro text-content-secondary truncate w-full">{c.name}</span>
            </button>
          ))}
        </div>

        {/* Account */}
        <p className="text-xs font-semibold text-content-tertiary uppercase tracking-wider mb-2">Account</p>
        <div className="flex gap-2 mb-4">
          {accounts.slice(0, 4).map(a => (
            <button key={a.id} onClick={() => setAcctId(a.id)}
              className={`px-3 py-1.5 rounded-lg text-xs font-medium transition-all ${
                acctId === a.id ? 'bg-[#FFE8E4] text-[#FF6B5B]' : 'bg-surface-raised text-content-secondary hover:text-content-primary'
              }`}
            >{a.name}</button>
          ))}
        </div>

        {/* Note */}
        <input placeholder="Add a note (optional)" value={note} onChange={e => setNote(e.target.value)}
          className="w-full px-4 py-2.5 bg-surface-raised rounded-xl outline-none text-xs text-content-primary placeholder:text-content-tertiary mb-6 border border-transparent focus:border-[#FF6B5B]/30 transition-colors"
        />

        <button onClick={() => { onAdd(amount, type, merchant.trim(), acctId!, catId, note.trim() || null); onClose(); }}
          disabled={!valid}
          className={`w-full py-3 rounded-xl text-sm font-semibold transition-all ${
            valid ? 'bg-[#FF6B5B] text-white hover:bg-[#E85A4B] shadow-lg shadow-[#FF6B5B]/20' : 'bg-content-tertiary/10 text-content-tertiary cursor-not-allowed'
          }`}
        >
          Add {type === 'debit' ? 'expense' : 'income'}
        </button>
      </motion.div>
    </motion.div>
  );
}
