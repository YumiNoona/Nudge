import { useState, useEffect } from 'react';
import { useLiveQuery } from 'dexie-react-hooks';
import { db, generateId, formatAmount } from './lib/db';
import type { Transaction, Account, Category, TransactionType, GamificationProfile } from './lib/db';
import { levelTitle, levelProgress, categoryColor } from './lib/engine';
import { seedDefaults } from './lib/seed';
import { categorizationEngine } from './lib/categorization';
import NeedsReviewScreen from './components/NeedsReviewScreen';
import MerchantAliasScreen from './components/MerchantAliasScreen';
import AnalyticsPage from './components/AnalyticsPage';
import AchievementsScreen from './components/AchievementsScreen';
import ChallengesScreen from './components/ChallengesScreen';
import SavingsGoalsScreen from './components/SavingsGoalsScreen';
import CSVImportScreen from './components/CSVImportScreen';
import BackupScreen from './components/BackupScreen';
import { motion, AnimatePresence } from 'framer-motion';

type Tab = 'dashboard' | 'review' | 'analytics' | 'achievements' | 'challenges' | 'goals' | 'csv' | 'backup' | 'merchants';

const TAB_LABELS: Record<Tab, string> = {
  dashboard: 'Dashboard',
  review: 'Review',
  analytics: 'Analytics',
  achievements: 'Badges',
  challenges: 'Challenges',
  goals: 'Goals',
  csv: 'Import',
  backup: 'Backup',
  merchants: 'Merchants',
};

const TAB_ICONS: Record<Tab, string> = {
  dashboard: '📊', review: '✅', analytics: '📈', achievements: '🏆',
  challenges: '🎯', goals: '🎯', csv: '📄', backup: '💾', merchants: '🏷️',
};

export default function App() {
  const [dark, setDark] = useState(() => window.matchMedia('(prefers-color-scheme: dark)').matches);
  const [showAddModal, setShowAddModal] = useState(false);
  const [tab, setTab] = useState<Tab>('dashboard');

  useEffect(() => {
    document.documentElement.classList.toggle('dark', dark);
  }, [dark]);

  useEffect(() => {
    seedDefaults().then(() => {
      db.gamificationProfile.toCollection().first().then((profile) => {
        if (!profile) {
          db.gamificationProfile.add({
            userId: generateId(),
            xpTotal: 0,
            level: 1,
            currentStreakDays: 0,
            longestStreakDays: 0,
            lastActivityDate: Date.now(),
            badgesJson: '[]',
            challengesJson: '[]',
            streakFreezes: 0,
            consistentDays: 0,
          });
        }
      });
    });
  }, []);

  const transactions = useLiveQuery(() =>
    db.transactions.orderBy('timestampEpoch').reverse().toArray()
  ) ?? [];

  const categories = useLiveQuery(() =>
    db.categories.filter((c) => !c.isArchived).toArray()
  ) ?? [];

  const accounts = useLiveQuery(() =>
    db.accounts.filter((a) => a.isActive).toArray()
  ) ?? [];

  const gamification = useLiveQuery(() =>
    db.gamificationProfile.toCollection().first()
  );

  const budgets = useLiveQuery(() =>
    db.budgets.toArray()
  ) ?? [];

  const merchantAliases = useLiveQuery(() =>
    db.merchantAliases.toArray()
  ) ?? [];

  const needsReviewCount = transactions.filter((t) => !t.isReviewed).length;

  const thisMonthSpend = transactions
    .filter((t) => t.type === 'debit')
    .reduce((sum, t) => sum + t.amountCents, 0);

  const handleAddTransaction = async (
    amountCents: number,
    type: TransactionType,
    merchantRaw: string,
    accountId: string,
    categoryId: string | null,
    note: string | null
  ) => {
    const txn: Transaction = {
      id: generateId(),
      amountCents,
      type,
      currency: 'INR',
      merchantRaw,
      merchantNormalized: null,
      categoryId,
      subcategoryId: null,
      accountId,
      source: 'manual',
      sourceRawText: null,
      confidenceScore: 1,
      isReviewed: true,
      isRecurring: false,
      recurringGroupId: null,
      note,
      tags: [],
      timestampEpoch: Date.now(),
      createdAt: Date.now(),
      updatedAt: Date.now(),
    };
    await db.transactions.add(txn);

    // Award XP
    if (gamification) {
      await db.gamificationProfile.update(gamification.userId, {
        xpTotal: gamification.xpTotal + 5,
      });
    }
  };

  const handleAddAlias = async (rawPattern: string, normalizedName: string) => {
    await db.merchantAliases.add({
      id: generateId(),
      rawPattern,
      normalizedName,
      suggestedCategoryId: null,
      createdAt: Date.now(),
    });
  };

  const handleDeleteAlias = async (id: string) => {
    await db.merchantAliases.delete(id);
  };

  const handleCategorizeReview = async (txnId: string, categoryId: string) => {
    await db.transactions.update(txnId, {
      categoryId,
      isReviewed: true,
      updatedAt: Date.now(),
    });
    const txn = await db.transactions.get(txnId);
    if (txn?.merchantNormalized) {
      categorizationEngine.learn(txn.merchantNormalized, categoryId);
    }
  };

  const handleSkipReview = async () => {}; // handled in NeedsReviewScreen

  // --- Screen rendering ---
  if (tab === 'review') {
    return (
      <div className={dark ? 'dark' : ''}>
        <NeedsReviewScreen onBack={() => setTab('dashboard')} />
      </div>
    );
  }

  if (tab === 'analytics') {
    return (
      <div className={dark ? 'dark' : ''}>
        <AnalyticsPage
          transactions={transactions}
          categories={categories}
          budgets={budgets}
          onBack={() => setTab('dashboard')}
        />
      </div>
    );
  }

  if (tab === 'achievements') {
    return (
      <div className={dark ? 'dark' : ''}>
        <AchievementsScreen
          gamificationProfile={gamification ?? null}
          onBack={() => setTab('dashboard')}
        />
      </div>
    );
  }

  if (tab === 'challenges') {
    return (
      <div className={dark ? 'dark' : ''}>
        <ChallengesScreen
          categories={categories}
          transactions={transactions}
          onBack={() => setTab('dashboard')}
        />
      </div>
    );
  }

  if (tab === 'goals') {
    return (
      <div className={dark ? 'dark' : ''}>
        <SavingsGoalsScreen onBack={() => setTab('dashboard')} />
      </div>
    );
  }

  if (tab === 'csv') {
    return (
      <div className={dark ? 'dark' : ''}>
        <CSVImportScreen
          categories={categories}
          accounts={accounts}
          onBack={() => setTab('dashboard')}
          onImport={async (importTxns) => {
            for (const txn of importTxns) {
              await db.transactions.add({
                id: generateId(),
                amountCents: txn.amountCents,
                type: txn.type,
                currency: 'INR',
                merchantRaw: txn.merchantRaw,
                merchantNormalized: null,
                categoryId: txn.categoryId,
                subcategoryId: null,
                accountId: txn.accountId,
                source: 'csv_import',
                sourceRawText: null,
                confidenceScore: 1,
                isReviewed: txn.categoryId !== null,
                isRecurring: false,
                recurringGroupId: null,
                note: txn.note,
                tags: [],
                timestampEpoch: txn.dateEpoch,
                createdAt: Date.now(),
                updatedAt: Date.now(),
              });
            }
            setTab('dashboard');
          }}
        />
      </div>
    );
  }

  if (tab === 'backup') {
    return (
      <div className={dark ? 'dark' : ''}>
        <BackupScreen onBack={() => setTab('dashboard')} />
      </div>
    );
  }

  if (tab === 'merchants') {
    return (
      <div className={dark ? 'dark' : ''}>
        <MerchantAliasScreen
          aliases={merchantAliases}
          onAdd={handleAddAlias}
          onDelete={handleDeleteAlias}
          onBack={() => setTab('dashboard')}
        />
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-surface-base flex">
      {/* Left sidebar — persistent navigation per §7.6 */}
      <aside className="w-56 flex-shrink-0 border-r border-content-tertiary/10 p-4 hidden lg:flex flex-col gap-1">
        <div className="mb-4 px-3">
          <h1 className="text-title font-display font-bold text-content-primary">Nudge</h1>
          <p className="text-micro text-content-tertiary">Your money, your rules</p>
        </div>
        {Object.entries(TAB_LABELS).map(([key, label]) => {
          const t = key as Tab;
          const isActive = tab === t;
          const badge = t === 'review' && needsReviewCount > 0 ? ` (${needsReviewCount})` : '';
          return (
            <button
              key={t}
              onClick={() => setTab(t)}
              className={`flex items-center gap-2 px-3 py-2 rounded-lg text-caption transition-colors text-left ${
                isActive
                  ? 'bg-accent-primary/15 text-accent-primary font-semibold'
                  : 'text-content-secondary hover:bg-surface-raised hover:text-content-primary'
              }`}
            >
              <span className="text-sm">{TAB_ICONS[t]}</span>
              {label}{badge}
            </button>
          );
        })}
        <div className="mt-auto pt-4 border-t border-content-tertiary/10">
          <div className="flex items-center gap-2 px-3">
            {gamification && (
              <span className="px-2 py-0.5 rounded-pill bg-accent-primary/10 text-accent-primary text-micro font-semibold">
                Lv.{gamification.level} {levelTitle(gamification.level)}
              </span>
            )}
            <button
              onClick={() => setDark(!dark)}
              className="p-1.5 rounded-md hover:bg-surface-raised text-content-secondary text-sm"
            >
              {dark ? '☀️' : '🌙'}
            </button>
          </div>
        </div>
      </aside>

      {/* Mobile top nav (shown only on small screens) */}
      <div className="lg:hidden fixed top-0 left-0 right-0 z-40 bg-surface-base/90 backdrop-blur border-b border-content-tertiary/10">
        <div className="flex items-center justify-between px-4 py-2">
          <h1 className="text-heading font-display font-bold text-content-primary">Nudge</h1>
          <div className="flex items-center gap-2">
            {gamification && (
              <span className="text-micro text-accent-primary font-semibold">Lv.{gamification.level}</span>
            )}
            <button onClick={() => setDark(!dark)} className="p-1 text-content-secondary">
              {dark ? '☀️' : '🌙'}
            </button>
          </div>
        </div>
        <div className="flex gap-0.5 px-2 pb-2 overflow-x-auto">
          {(['dashboard', 'review', 'analytics', 'achievements', 'challenges'] as Tab[]).map((t) => (
            <button
              key={t}
              onClick={() => setTab(t)}
              className={`flex-shrink-0 px-3 py-1 rounded-pill text-micro font-medium whitespace-nowrap ${
                tab === t ? 'bg-accent-primary text-white' : 'text-content-secondary'
              }`}
            >
              {TAB_ICONS[t]} {TAB_LABELS[t]}{t === 'review' && needsReviewCount > 0 ? ` (${needsReviewCount})` : ''}
            </button>
          ))}
        </div>
      </div>

      {/* Main content area */}
      <div className="flex-1 lg:pt-0 pt-24">
        <div className="max-w-7xl mx-auto p-4 lg:p-8">

        <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
          {/* Left column — charts/spend overview */}
          <div className="lg:col-span-2 space-y-6">
            {/* Big spend number */}
            <motion.div
              initial={{ opacity: 0, y: 20 }}
              animate={{ opacity: 1, y: 0 }}
              className="p-8 bg-surface-raised rounded-xl text-center"
              style={{ boxShadow: 'var(--shadow-md)' }}
            >
              <p className="text-caption text-content-secondary mb-1">This Month</p>
              <p className="text-display font-bold font-mono tabular-nums text-accent-primary">
                ₹{formatAmount(thisMonthSpend)}
              </p>
              <p className="text-micro text-content-tertiary mt-1">spent so far</p>
            </motion.div>

            {/* Category budget rings */}
            {categories.length > 0 && (
              <div className="p-6 bg-surface-raised rounded-xl" style={{ boxShadow: 'var(--shadow-sm)' }}>
                <h2 className="text-heading font-semibold text-content-primary mb-4">Budgets</h2>
                <div className="flex gap-4 overflow-x-auto pb-2">
                  {categories.filter((c) => c.type === 'expense').slice(0, 8).map((cat, i) => {
                    const spent = transactions
                      .filter((t) => t.categoryId === cat.id && t.type === 'debit')
                      .reduce((sum, t) => sum + t.amountCents, 0);
                    const budget = cat.monthlyBudgetCents || 10000;
                    const progress = Math.min(spent / budget, 1.5);
                    const color = cat.color || categoryColor(i);

                    return (
                      <div key={cat.id} className="flex flex-col items-center gap-1 flex-shrink-0 w-16">
                        <svg width="48" height="48" viewBox="0 0 48 48">
                          <circle cx="24" cy="24" r="20" fill="none" stroke="var(--color-content-tertiary)" strokeWidth="3" opacity="0.2" />
                          <circle
                            cx="24" cy="24" r="20"
                            fill="none"
                            stroke={color}
                            strokeWidth="3"
                            strokeDasharray={`${Math.min(progress, 1) * 125.6} 125.6`}
                            strokeLinecap="round"
                            transform="rotate(-90 24 24)"
                          />
                        </svg>
                        <span className="text-micro text-content-secondary truncate w-full text-center">{cat.name}</span>
                      </div>
                    );
                  })}
                </div>
              </div>
            )}

            {/* Transaction list */}
            <div className="p-6 bg-surface-raised rounded-xl" style={{ boxShadow: 'var(--shadow-sm)' }}>
              <h2 className="text-heading font-semibold text-content-primary mb-4">Recent Transactions</h2>
              {transactions.length === 0 ? (
                <div className="text-center py-12">
                  <span className="text-4xl block mb-2">💰</span>
                  <p className="text-body text-content-secondary">No transactions yet</p>
                  <p className="text-caption text-content-tertiary">Add your first entry to get started</p>
                </div>
              ) : (
                <div className="space-y-2">
                  {transactions.slice(0, 20).map((txn) => (
                    <motion.div
                      key={txn.id}
                      initial={{ opacity: 0 }}
                      animate={{ opacity: 1 }}
                      className="flex items-center justify-between p-3 rounded-md hover:bg-surface-base/50 transition-colors"
                    >
                      <div>
                        <p className="text-body font-medium text-content-primary">{txn.merchantRaw}</p>
                        {txn.note && <p className="text-micro text-content-tertiary">{txn.note}</p>}
                      </div>
                      <span
                        className={`money text-body font-semibold font-mono tabular-nums ${
                          txn.type === 'debit' ? 'text-negative' : 'text-positive'
                        }`}
                      >
                        {txn.type === 'debit' ? '-' : '+'}₹{formatAmount(txn.amountCents)}
                      </span>
                    </motion.div>
                  ))}
                </div>
              )}
            </div>
          </div>

          {/* Right column — gamification & quick stats */}
          <div className="space-y-6">
            {gamification && (
              <div className="p-6 bg-surface-raised rounded-xl" style={{ boxShadow: 'var(--shadow-sm)' }}>
                <h2 className="text-heading font-semibold text-content-primary mb-4">Your Progress</h2>

                {/* Streak */}
                <div className="flex items-center gap-3 mb-4">
                  <span className="text-2xl">🔥</span>
                  <div>
                    <p className="text-title font-bold text-content-primary">{gamification.currentStreakDays}d</p>
                    <p className="text-micro text-content-tertiary">current streak</p>
                  </div>
                </div>

                {/* XP Progress */}
                <div>
                  <div className="flex justify-between text-caption mb-1">
                    <span className="text-accent-primary font-semibold">Level {gamification.level}</span>
                    <span className="text-content-secondary">{gamification.xpTotal} XP</span>
                  </div>
                  <div className="h-2 bg-accent-primary/20 rounded-full overflow-hidden">
                    <div
                      className="h-full bg-accent-primary rounded-full transition-all duration-500"
                      style={{ width: `${levelProgress(gamification.xpTotal) * 100}%` }}
                    />
                  </div>
                </div>
              </div>
            )}

            {/* Needs Review teaser */}
            {transactions.filter((t) => !t.isReviewed).length > 0 && (
              <div
                className="p-4 bg-warning/10 rounded-lg border border-warning/20 cursor-pointer hover:bg-warning/20 transition-colors"
                style={{ borderColor: 'var(--color-warning)' }}
              >
                <p className="text-body font-medium text-warning">
                  {transactions.filter((t) => !t.isReviewed).length} need review →
                </p>
              </div>
            )}
          </div>
        </div>
      </div>
      </div>

      {/* FAB — Add Transaction */}
      <button
        onClick={() => setShowAddModal(true)}
        className="fixed bottom-8 right-8 w-14 h-14 bg-accent-primary text-white text-2xl font-light rounded-full shadow-lg hover:scale-105 active:scale-95 transition-transform z-50 flex items-center justify-center"
      >
        +
      </button>

      {/* Add Transaction Modal */}
      <AnimatePresence>
        {showAddModal && (
          <AddTransactionModal
            categories={categories}
            accounts={accounts}
            onClose={() => setShowAddModal(false)}
            onAdd={handleAddTransaction}
          />
        )}
      </AnimatePresence>
    </div>
  );
}

// --- Add Transaction Modal ---

function AddTransactionModal({
  categories,
  accounts,
  onClose,
  onAdd,
}: {
  categories: Category[];
  accounts: Account[];
  onClose: () => void;
  onAdd: (
    amountCents: number,
    type: TransactionType,
    merchantRaw: string,
    accountId: string,
    categoryId: string | null,
    note: string | null
  ) => void;
}) {
  const [amountStr, setAmountStr] = useState('');
  const [merchant, setMerchant] = useState('');
  const [note, setNote] = useState('');
  const [type, setType] = useState<TransactionType>('debit');
  const [selectedCategoryId, setSelectedCategoryId] = useState<string | null>(null);
  const [selectedAccountId, setSelectedAccountId] = useState<string | null>(null);

  const amountInCents = parseInt(amountStr.replace(/\D/g, '') || '0') * 100;
  const isValid = amountInCents > 0 && merchant.trim() && selectedAccountId;

  const handleSubmit = () => {
    if (!isValid) return;
    onAdd(amountInCents, type, merchant.trim(), selectedAccountId!, selectedCategoryId, note.trim() || null);
    onClose();
  };

  return (
    <motion.div
      initial={{ opacity: 0 }}
      animate={{ opacity: 1 }}
      exit={{ opacity: 0 }}
      className="fixed inset-0 bg-surface-overlay flex items-end lg:items-center justify-center z-50"
      onClick={onClose}
    >
      <motion.div
        initial={{ y: 100, opacity: 0 }}
        animate={{ y: 0, opacity: 1 }}
        exit={{ y: 100, opacity: 0 }}
        transition={{ type: 'spring', damping: 25, stiffness: 300 }}
        className="bg-surface-base w-full max-w-md rounded-t-xl lg:rounded-xl p-6 max-h-[90vh] overflow-y-auto"
        onClick={(e) => e.stopPropagation()}
      >
        <div className="w-9 h-1 bg-content-tertiary rounded-full mx-auto mb-4" />

        <h2 className="text-title font-bold text-content-primary mb-6">Add Transaction</h2>

        {/* Amount */}
        <div className="flex items-center mb-5">
          <span className="text-title font-bold text-accent-primary mr-2">₹</span>
          <input
            type="text"
            inputMode="numeric"
            placeholder="0"
            value={amountStr}
            onChange={(e) => setAmountStr(e.target.value)}
            className="flex-1 text-title font-bold font-mono bg-transparent outline-none text-content-primary placeholder:text-content-tertiary"
          />
        </div>

        {/* Type toggle */}
        <div className="flex gap-2 mb-4">
          {(['debit', 'credit'] as TransactionType[]).map((t) => (
            <button
              key={t}
              onClick={() => setType(t)}
              className={`px-4 py-1.5 rounded-pill text-caption font-medium transition-colors ${
                type === t
                  ? 'bg-accent-primary/15 text-accent-primary'
                  : 'bg-surface-raised text-content-secondary hover:bg-surface-raised/80'
              }`}
            >
              {t === 'debit' ? 'Expense' : 'Income'}
            </button>
          ))}
        </div>

        {/* Merchant */}
        <input
          type="text"
          placeholder="What was this for?"
          value={merchant}
          onChange={(e) => setMerchant(e.target.value)}
          className="w-full p-3 bg-surface-raised rounded-md outline-none text-body text-content-primary placeholder:text-content-tertiary mb-4"
        />

        {/* Category grid */}
        <p className="text-caption font-medium text-content-secondary mb-2">Category</p>
        <div className="grid grid-cols-4 gap-2 mb-4">
          {categories
            .filter((c) => c.type === 'expense')
            .slice(0, 12)
            .map((cat, i) => (
              <button
                key={cat.id}
                onClick={() => setSelectedCategoryId(cat.id)}
                className={`flex flex-col items-center p-2 rounded-md text-center transition-colors ${
                  selectedCategoryId === cat.id
                    ? 'ring-2 ring-accent-primary bg-accent-primary/10'
                    : 'bg-surface-raised hover:bg-surface-raised/80'
                }`}
              >
                <span className="text-lg">{cat.icon || '📁'}</span>
                <span className="text-micro text-content-secondary truncate w-full">{cat.name}</span>
              </button>
            ))}
        </div>

        {/* Account */}
        <p className="text-caption font-medium text-content-secondary mb-2">Account</p>
        <div className="flex gap-2 mb-4">
          {accounts.slice(0, 4).map((acct) => (
            <button
              key={acct.id}
              onClick={() => setSelectedAccountId(acct.id)}
              className={`px-3 py-1.5 rounded-pill text-caption transition-colors ${
                selectedAccountId === acct.id
                  ? 'bg-accent-primary/15 text-accent-primary'
                  : 'bg-surface-raised text-content-secondary hover:bg-surface-raised/80'
              }`}
            >
              {acct.name}
            </button>
          ))}
        </div>

        {/* Note */}
        <input
          type="text"
          placeholder="Add a note (optional)"
          value={note}
          onChange={(e) => setNote(e.target.value)}
          className="w-full p-2 bg-surface-raised rounded-md outline-none text-caption text-content-primary placeholder:text-content-tertiary mb-6"
        />

        {/* Submit */}
        <button
          onClick={handleSubmit}
          disabled={!isValid}
          className={`w-full py-3 rounded-lg text-body font-semibold transition-colors ${
            isValid
              ? 'bg-accent-primary text-white hover:bg-accent-primary/90'
              : 'bg-content-tertiary/20 text-content-tertiary cursor-not-allowed'
          }`}
        >
          Add {type === 'debit' ? 'Expense' : 'Income'}
        </button>
      </motion.div>
    </motion.div>
  );
}
