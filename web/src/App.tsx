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
import { motion, AnimatePresence } from 'framer-motion';
import {
  IconLayoutDashboard, IconChecklist, IconChartBar, IconTrophy, IconTarget,
  IconPigMoney, IconFileImport, IconDatabase, IconTag, IconPlus,
  IconSun, IconMoon, IconFlame, IconWallet,
} from './lib/icons';

type Tab = 'dashboard' | 'review' | 'analytics' | 'achievements' | 'challenges' | 'goals' | 'csv' | 'backup' | 'merchants';

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

  useEffect(() => { document.documentElement.classList.toggle('dark', dark); }, [dark]);

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
    <div className="min-h-screen bg-surface-base flex">
      {/* Desktop sidebar */}
      <div className="hidden lg:block">{sidebar}</div>

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
      <div className="flex-1 flex flex-col min-h-screen">
        {/* Mobile header */}
        <header className="lg:hidden flex items-center justify-between px-4 py-3 border-b border-content-tertiary/10 bg-surface-base/90 backdrop-blur-sm sticky top-0 z-30">
          <button onClick={() => setMobileNav(true)} className="p-1.5 -ml-1.5 rounded-lg hover:bg-surface-raised text-content-secondary">
            <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round"><path d="M4 6h16M4 12h16M4 18h16"/></svg>
          </button>
          <div className="flex items-center gap-2">
            <div className="w-7 h-7 rounded-lg bg-[#FF6B5B] flex items-center justify-center"><IconWallet size={14} className="text-white" /></div>
            <span className="font-semibold text-content-primary">Nudge</span>
          </div>
          <button onClick={() => setDark(!dark)} className="p-1.5 rounded-lg hover:bg-surface-raised text-content-secondary">
            {dark ? <IconSun size={18} /> : <IconMoon size={18} />}
          </button>
        </header>

        {/* Screen content */}
        <div className="flex-1 overflow-auto">
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
            </div>
          )}

          {tab === 'dashboard' && <DashboardView {...screenProps} onAddTransaction={handleAddTransaction} showAddModal={showAddModal} setShowAddModal={setShowAddModal} />}
        </div>
      </div>
    </div>
  );
}

// ─── Dashboard View ────────────────────────────────────────────────

function DashboardView(props: {
  transactions: Transaction[]; categories: Category[]; accounts: Account[];
  budgets: any[]; gamification: any; needsReviewCount: number;
  onAddTransaction: (a: number, t: TransactionType, m: string, ac: string, c: string | null, n: string | null) => void;
  showAddModal: boolean; setShowAddModal: (v: boolean) => void;
}) {
  const { transactions, categories, gamification, needsReviewCount, showAddModal, setShowAddModal, onAddTransaction, accounts } = props;
  const thisMonth = new Date().getMonth();
  const thisYear = new Date().getFullYear();
  const monthTxns = transactions.filter(t => {
    const d = new Date(t.timestampEpoch);
    return d.getMonth() === thisMonth && d.getFullYear() === thisYear;
  });
  const spend = monthTxns.filter(t => t.type === 'debit').reduce((s, t) => s + t.amountCents, 0);
  const income = monthTxns.filter(t => t.type === 'credit').reduce((s, t) => s + t.amountCents, 0);

  // Weekly breakdown for mini chart
  const weekLabels = ['W1', 'W2', 'W3', 'W4'];
  const weekData = [0, 1, 2, 3].map(w => {
    const start = new Date(thisYear, thisMonth, w * 7 + 1).getTime();
    const end = new Date(thisYear, thisMonth, (w + 1) * 7 + 1).getTime();
    return monthTxns.filter(t => t.type === 'debit' && t.timestampEpoch >= start && t.timestampEpoch < end).reduce((s, t) => s + t.amountCents, 0);
  });
  const maxWeek = Math.max(...weekData, 1);

  return (
    <div className="p-4 lg:p-8 space-y-6 max-w-4xl mx-auto">
      {/* Greeting + quick stats */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <p className="text-sm text-content-secondary">Good {new Date().getHours() < 12 ? 'morning' : new Date().getHours() < 17 ? 'afternoon' : 'evening'}</p>
          <h2 className="text-2xl font-bold text-content-primary">Your wallet</h2>
        </div>
        {gamification && (
          <div className="flex items-center gap-3 px-4 py-2.5 bg-surface-raised rounded-xl border border-content-tertiary/10">
            <div className="w-9 h-9 rounded-lg bg-[#FFE8E4] flex items-center justify-center">
              <IconFlame size={18} className="text-[#FF6B5B]" />
            </div>
            <div>
              <div className="text-sm font-bold text-content-primary">{gamification.currentStreakDays}d streak</div>
              <div className="text-xs text-content-secondary">Level {gamification.level} · {levelTitle(gamification.level)}</div>
            </div>
          </div>
        )}
      </div>

      {/* Big spend + income cards */}
      <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
        <div className="sm:col-span-2 bg-surface-raised rounded-2xl p-6 border border-content-tertiary/5" style={{ boxShadow: 'var(--shadow-md)' }}>
          <p className="text-xs font-semibold text-content-tertiary uppercase tracking-wider mb-1">Spent this month</p>
          <motion.p
            key={spend}
            initial={{ opacity: 0, y: 8 }}
            animate={{ opacity: 1, y: 0 }}
            className="text-4xl font-bold font-mono tabular-nums text-content-primary"
          >
            ₹{formatAmount(spend)}
          </motion.p>
          <div className="flex items-end gap-1 mt-4 h-16">
            {weekData.map((v, i) => (
              <div key={i} className="flex-1 flex flex-col items-center gap-1">
                <motion.div
                  initial={{ height: 0 }}
                  animate={{ height: `${(v / maxWeek) * 48}px` }}
                  transition={{ duration: 0.6, delay: i * 0.1 }}
                  className="w-full max-w-[32px] bg-[#FF6B5B] rounded-md opacity-80"
                />
                <span className="text-micro text-content-tertiary">{weekLabels[i]}</span>
              </div>
            ))}
          </div>
        </div>
        <div className="bg-surface-raised rounded-2xl p-6 border border-content-tertiary/5 flex flex-col justify-center" style={{ boxShadow: 'var(--shadow-md)' }}>
          <p className="text-xs font-semibold text-content-tertiary uppercase tracking-wider mb-1">Income</p>
          <p className="text-3xl font-bold font-mono tabular-nums text-[#7CB69E]">₹{formatAmount(income)}</p>
          <p className="text-xs text-content-tertiary mt-2">
            {spend > income ? `−${formatAmount(spend - income)} net` : `+${formatAmount(income - spend)} net`}
          </p>
        </div>
      </div>

      {/* Needs review callout */}
      {needsReviewCount > 0 && (
        <motion.button
          whileHover={{ scale: 1.01 }}
          whileTap={{ scale: 0.99 }}
          onClick={() => {
            const ev = new CustomEvent('nudge-nav', { detail: 'review' });
            window.dispatchEvent(ev);
          }}
          className="w-full flex items-center gap-4 p-4 bg-[#FFF3E4] rounded-2xl border border-[#F4A261]/20 text-left"
        >
          <div className="w-10 h-10 rounded-xl bg-[#F4A261]/20 flex items-center justify-center flex-shrink-0">
            <IconChecklist size={20} className="text-[#F4A261]" />
          </div>
          <div className="flex-1">
            <p className="text-sm font-semibold text-[#A66B2A]">{needsReviewCount} transaction{needsReviewCount > 1 ? 's' : ''} need review</p>
            <p className="text-xs text-[#C49050]">Swipe to categorize and earn XP</p>
          </div>
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="#A66B2A" strokeWidth="2" strokeLinecap="round"><path d="M5 12h14M13 18l6-6M13 6l6 6"/></svg>
        </motion.button>
      )}

      {/* Recent transactions */}
      <div className="bg-surface-raised rounded-2xl border border-content-tertiary/5 overflow-hidden" style={{ boxShadow: 'var(--shadow-sm)' }}>
        <div className="flex items-center justify-between px-6 py-4 border-b border-content-tertiary/5">
          <h3 className="text-sm font-semibold text-content-primary">Recent transactions</h3>
          <span className="text-xs text-content-tertiary">{transactions.length} total</span>
        </div>
        {transactions.length === 0 ? (
          <div className="flex flex-col items-center justify-center py-16 gap-3">
            <div className="w-14 h-14 rounded-2xl bg-surface-base flex items-center justify-center">
              <IconWallet size={26} className="text-content-tertiary" />
            </div>
            <p className="text-sm font-medium text-content-secondary">No transactions yet</p>
            <p className="text-xs text-content-tertiary">Tap + to add your first entry</p>
          </div>
        ) : (
          <div className="divide-y divide-content-tertiary/5">
            {transactions.slice(0, 15).map(txn => {
              const cat = categories.find(c => c.id === txn.categoryId);
              return (
                <div key={txn.id} className="flex items-center gap-4 px-6 py-3.5 hover:bg-surface-base/50 transition-colors">
                  <div className="w-10 h-10 rounded-xl flex items-center justify-center flex-shrink-0" style={{ backgroundColor: (cat?.color || categoryColor(categories.indexOf(cat!))) + '18' }}>
                    <span className="text-base">{cat?.icon || '💳'}</span>
                  </div>
                  <div className="flex-1 min-w-0">
                    <p className="text-sm font-medium text-content-primary truncate">{txn.merchantRaw}</p>
                    <p className="text-xs text-content-tertiary">
                      {cat?.name || 'Uncategorized'} · {new Date(txn.timestampEpoch).toLocaleDateString('en-IN', { day: 'numeric', month: 'short' })}
                    </p>
                  </div>
                  <span className={`text-sm font-semibold font-mono tabular-nums ${txn.type === 'debit' ? 'text-[#FF6B5B]' : 'text-[#7CB69E]'}`}>
                    {txn.type === 'debit' ? '−' : '+'}₹{formatAmount(txn.amountCents)}
                  </span>
                </div>
              );
            })}
          </div>
        )}
      </div>

      {/* FAB */}
      <button
        onClick={() => setShowAddModal(true)}
        className="fixed bottom-6 right-6 w-14 h-14 bg-[#FF6B5B] text-white rounded-2xl shadow-lg shadow-[#FF6B5B]/25 hover:shadow-xl hover:shadow-[#FF6B5B]/30 hover:scale-105 active:scale-95 transition-all duration-200 z-40 flex items-center justify-center"
      >
        <IconPlus size={24} stroke={2.5} />
      </button>

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
        className="bg-surface-base w-full max-w-md rounded-t-3xl lg:rounded-3xl p-6 max-h-[85vh] overflow-y-auto border border-content-tertiary/5"
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
