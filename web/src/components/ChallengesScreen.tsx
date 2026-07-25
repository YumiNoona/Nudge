import { useState, useMemo } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import type { Transaction, Category } from '../lib/db';
import { formatAmount } from '../lib/db';
import { XP_REVIEW_TRANSACTION } from '../lib/engine';
import { CategoryChip } from './ui/CategoryChip';

interface Challenge {
  id: string;
  name: string;
  description: string;
  emoji: string;
  target: number;
  current: number;
  xpReward: number;
  completed: boolean;
}

interface CustomChallenge {
  id: string;
  name: string;
  description: string;
  target: number;
  xpReward: number;
}

interface Props {
  categories: Category[];
  transactions: Transaction[];
  onBack: () => void;
}

const containerVariants = {
  hidden: { opacity: 0 },
  visible: {
    opacity: 1,
    transition: { staggerChildren: 0.08 },
  },
};

const itemVariants = {
  hidden: { opacity: 0, y: 24 },
  visible: { opacity: 1, y: 0, transition: { duration: 0.4 } },
};

let customIdCounter = 0;
function nextCustomId(): string {
  customIdCounter++;
  return `custom-${Date.now()}-${customIdCounter}`;
}

export default function ChallengesScreen({
  categories,
  transactions,
  onBack,
}: Props) {
  const [customChallenges, setCustomChallenges] = useState<CustomChallenge[]>([]);
  const [showModal, setShowModal] = useState(false);
  const [modalName, setModalName] = useState('');
  const [modalDesc, setModalDesc] = useState('');
  const [modalTarget, setModalTarget] = useState('');

  const autoChallenges = useMemo((): Challenge[] => {
    const now = Date.now();
    const weekMs = 7 * 24 * 60 * 60 * 1000;
    const weekStart = now - weekMs;

    const thisWeekTxns = transactions.filter(
      (t) => t.timestampEpoch >= weekStart,
    );
    const thisWeekDebits = thisWeekTxns.filter((t) => t.type === 'debit');

    const challenges: Challenge[] = [];

    // Count days with activity this week
    const daysActive = new Set<number>();
    thisWeekTxns.forEach((t) => {
      const d = new Date(t.timestampEpoch);
      d.setHours(0, 0, 0, 0);
      daysActive.add(d.getTime());
    });

    // 1. Daily check-in (always)
    challenges.push({
      id: 'daily-checkin',
      name: 'Daily Check-In',
      description: 'Open the app every day this week',
      emoji: '📅',
      target: 7,
      current: Math.min(daysActive.size, 7),
      xpReward: 30,
      completed: daysActive.size >= 7,
    });

    // 2. Reduce spend in top category by 15%
    if (thisWeekDebits.length > 0) {
      const catSpend = new Map<string, number>();
      for (const t of thisWeekDebits) {
        if (t.categoryId) {
          catSpend.set(t.categoryId, (catSpend.get(t.categoryId) || 0) + t.amountCents);
        }
      }
      if (catSpend.size > 0) {
        const topEntry = [...catSpend.entries()].sort((a, b) => b[1] - a[1])[0];
        const topCatId = topEntry[0];
        const topCat = categories.find((c) => c.id === topCatId);
        const lastWeekAmount = topEntry[1];
        const targetSpend = Math.round(lastWeekAmount * 0.85);
        challenges.push({
          id: 'reduce-top-category',
          name: `Cut Back on ${topCat?.name || 'Top Category'}`,
          description: 'Reduce spend by 15% compared to last week',
          emoji: topCat?.icon || '📉',
          target: targetSpend,
          current: targetSpend,
          xpReward: 40,
          completed: false,
        });
      }
    }

    // 3. No-spend days target (aim for 3)
    const noSpendDaysCount = 7 - daysActive.size;
    challenges.push({
      id: 'no-spend-days',
      name: 'No-Spend Days',
      description: 'Aim for 3 days with zero discretionary spend this week',
      emoji: '🌴',
      target: 3,
      current: Math.min(noSpendDaysCount, 3),
      xpReward: 45,
      completed: noSpendDaysCount >= 3,
    });

    // 4. Keep total under 90% of last week's daily average
    if (thisWeekDebits.length > 0) {
      const dailyTotals = new Map<number, number>();
      for (const t of thisWeekDebits) {
        const d = new Date(t.timestampEpoch);
        d.setHours(0, 0, 0, 0);
        const key = d.getTime();
        dailyTotals.set(key, (dailyTotals.get(key) || 0) + t.amountCents);
      }
      const avgSpend =
        dailyTotals.size > 0
          ? [...dailyTotals.values()].reduce((a, b) => a + b, 0) / dailyTotals.size
          : 0;
      const targetTotal = Math.round(avgSpend * 0.9 * 7);
      const weekTotal = thisWeekDebits.reduce((s, t) => s + t.amountCents, 0);
      challenges.push({
        id: 'under-average',
        name: 'Stay Under Average',
        description: 'Keep total spend under 90% of your daily average pace',
        emoji: '📊',
        target: targetTotal,
        current: Math.max(0, targetTotal - weekTotal),
        xpReward: 35,
        completed: weekTotal <= targetTotal,
      });
    }

    // 5. Same-day logging streak
    const today = new Date();
    today.setHours(0, 0, 0, 0);
    const todayTxns = thisWeekTxns.filter((t) => {
      const d = new Date(t.timestampEpoch);
      d.setHours(0, 0, 0, 0);
      return d.getTime() === today.getTime() && t.source === 'manual';
    });
    challenges.push({
      id: 'same-day-logging',
      name: 'Same-Day Logger',
      description: 'Log at least one transaction on the same day it happened',
      emoji: '⏱️',
      target: 5,
      current: Math.min(todayTxns.length > 0 ? daysActive.size : 0, 5),
      xpReward: 25,
      completed: todayTxns.length > 0 && daysActive.size >= 5,
    });

    // 6. Subscription review (if applicable)
    const hasRecurring = thisWeekTxns.some((t) => t.isRecurring);
    if (hasRecurring) {
      challenges.push({
        id: 'subscription-review',
        name: 'Subscription Review',
        description: 'Review all your recurring subscriptions this week',
        emoji: '👀',
        target: 1,
        current: 0,
        xpReward: 50,
        completed: false,
      });
    }

    // 7. Log income
    const hasIncome = thisWeekTxns.some(
      (t) => t.type === 'credit' || t.type === 'refund',
    );
    challenges.push({
      id: 'log-income',
      name: 'Track Your Income',
      description: 'Log at least one income transaction this week',
      emoji: '💰',
      target: 1,
      current: hasIncome ? 1 : 0,
      xpReward: 20,
      completed: hasIncome,
    });

    // 8. Try unused category
    const usedCategoryIds = new Set(
      transactions.filter((t) => t.categoryId).map((t) => t.categoryId),
    );
    const expenseCategories = categories.filter(
      (c) => c.type === 'expense' && !c.isArchived,
    );
    const unusedCategories = expenseCategories.filter(
      (c) => !usedCategoryIds.has(c.id),
    );
    if (unusedCategories.length > 0) {
      const pick = unusedCategories[Math.floor(Math.random() * unusedCategories.length)];
      challenges.push({
        id: `try-category-${pick.id}`,
        name: 'Explore a New Category',
        description: `Use the "${pick.name}" category for the first time`,
        emoji: pick.icon || '🎨',
        target: 1,
        current: 0,
        xpReward: 15,
        completed: false,
      });
    }

    return challenges;
  }, [categories, transactions]);

  const combinedChallenges = useMemo(
    () =>
      autoChallenges.map((c) => ({ ...c, isCustom: false })).concat(
        customChallenges.map((c) => ({
          id: c.id,
          name: c.name,
          description: c.description,
          emoji: '🎯',
          target: c.target,
          current: 0,
          xpReward: c.xpReward,
          completed: false,
          isCustom: true,
        })),
      ),
    [autoChallenges, customChallenges],
  );

  const completedCount = combinedChallenges.filter((c) => c.completed).length;

  const handleAddCustom = () => {
    if (!modalName.trim() || !modalDesc.trim() || !modalTarget) return;
    setCustomChallenges((prev) => [
      ...prev,
      {
        id: nextCustomId(),
        name: modalName.trim(),
        description: modalDesc.trim(),
        target: Number(modalTarget),
        xpReward: 25,
      },
    ]);
    setModalName('');
    setModalDesc('');
    setModalTarget('');
    setShowModal(false);
  };

  return (
    <motion.div
      className="min-h-screen bg-lavender-bg"
      initial="hidden"
      animate="visible"
      variants={containerVariants}
    >
      <div className="max-w-2xl mx-auto p-4">
        {/* Header */}
        <div className="flex items-center justify-between mb-6">
          <button
            onClick={onBack}
            className="text-ink-soft text-[13px] hover:text-ink-1 transition-colors"
          >
            ← Back
          </button>
          <h1 className="text-lg font-bold text-ink-1">
            Challenges
          </h1>
          <button
            onClick={() => setShowModal(true)}
            className="text-[13px] font-medium text-purple-1 hover:opacity-80 transition-opacity"
          >
            + Custom
          </button>
        </div>

        {/* Progress summary */}
        <div className="rounded-card shadow-card bg-[var(--surface)] p-4 mb-4">
          <p className="text-[13px] text-ink-1">
            {completedCount} of {combinedChallenges.length} challenges completed this week
          </p>
          <div className="h-1.5 bg-[var(--lavender-bg)] rounded-full overflow-hidden mt-2">
            <div
              className="h-full bg-gradient-to-r from-purple-1 to-purple-2 rounded-full transition-all duration-500"
              style={{
                width: combinedChallenges.length > 0
                  ? `${(completedCount / combinedChallenges.length) * 100}%`
                  : '0%',
              }}
            />
          </div>
        </div>

        {/* Section */}
        <h2 className="text-sm font-bold text-ink-soft uppercase tracking-wide mb-3">
          This week
        </h2>

        {/* Challenge list */}
        {combinedChallenges.length === 0 ? (
          <div className="text-center py-16">
            <span className="text-4xl block mb-2">🏅</span>
            <p className="text-[13px] text-ink-soft">No challenges yet</p>
            <p className="text-[11px] text-ink-mute">
              Add transactions to generate challenges, or create a custom one
            </p>
          </div>
        ) : (
          <motion.div className="space-y-3" variants={containerVariants}>
            {combinedChallenges.map((challenge) => {
              const progressPct =
                challenge.target > 0
                  ? Math.min(challenge.current / challenge.target, 1)
                  : 0;

              return (
                <motion.div
                  key={challenge.id}
                  variants={itemVariants}
                  className="relative rounded-card shadow-card bg-[var(--surface)] p-4 overflow-hidden"
                >
                  {/* Completion overlay */}
                  {challenge.completed && (
                    <div className="absolute inset-0 bg-green-bg/60 flex items-center justify-center z-10 pointer-events-none">
                      <div className="flex items-center gap-2 bg-green-1/15 px-4 py-2 rounded-pill">
                        <span className="text-lg">✅</span>
                        <span className="text-[13px] font-semibold text-green-1">
                          Completed
                        </span>
                      </div>
                    </div>
                  )}

                  <div
                    className={`transition-opacity duration-300 ${
                      challenge.completed ? 'opacity-50' : ''
                    }`}
                  >
                    {/* Top row */}
                    <div className="flex items-start justify-between mb-2">
                      <div className="flex items-center gap-3 min-w-0">
                        <CategoryChip icon={challenge.emoji} size="md" />
                        <div className="min-w-0">
                          <h3
                            className={`text-[13px] font-semibold truncate ${
                              challenge.completed
                                ? 'text-ink-mute line-through'
                                : 'text-ink-1'
                            }`}
                          >
                            {challenge.name}
                          </h3>
                          <p className="text-[11px] text-ink-mute line-clamp-2">
                            {challenge.description}
                          </p>
                        </div>
                      </div>
                      {/* XP badge */}
                      <span className="flex-shrink-0 px-2 py-0.5 bg-purple-bg text-purple-1 text-[11px] font-semibold rounded-pill ml-2">
                        +{challenge.xpReward} XP
                      </span>
                    </div>

                    {/* Progress bar */}
                    <div className="h-1.5 bg-[var(--lavender-bg)] rounded-full overflow-hidden">
                      <div
                        className={`h-full rounded-full transition-all duration-500 ${
                          challenge.completed
                            ? 'bg-green-1'
                            : 'bg-gradient-to-r from-purple-1 to-purple-2'
                        }`}
                        style={{
                          width: `${progressPct * 100}%`,
                        }}
                      />
                    </div>
                    <p className="text-[11px] text-ink-mute mt-1 text-right font-mono tabular-nums">
                      {challenge.current} / {challenge.target}
                    </p>
                  </div>
                </motion.div>
              );
            })}
          </motion.div>
        )}

        <div className="h-8" />
      </div>

      {/* Custom challenge modal */}
      <AnimatePresence>
        {showModal && (
          <motion.div
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            exit={{ opacity: 0 }}
            className="fixed inset-0 bg-ink-1/40 flex items-end lg:items-center justify-center z-50"
            onClick={() => setShowModal(false)}
          >
            <motion.div
              initial={{ y: 100, opacity: 0 }}
              animate={{ y: 0, opacity: 1 }}
              exit={{ y: 100, opacity: 0 }}
              className="bg-[var(--surface)] w-full max-w-md rounded-t-3xl lg:rounded-3xl p-6 border border-ink-mute/5"
              onClick={(e) => e.stopPropagation()}
            >
              <div className="w-9 h-1 bg-ink-mute rounded-full mx-auto mb-4" />
              <h3 className="text-[14px] font-bold text-ink-1 mb-4">
                Create Custom Challenge
              </h3>

              <div className="space-y-3 mb-4">
                <div>
                  <label className="text-[11px] text-ink-mute block mb-1">
                    Name
                  </label>
                  <input
                    type="text"
                    placeholder="e.g. Walk 10,000 Steps"
                    value={modalName}
                    onChange={(e) => setModalName(e.target.value)}
                    className="w-full p-3 bg-[var(--lavender-bg)] rounded-chip outline-none text-[13px] text-ink-1 placeholder:text-ink-mute"
                  />
                </div>
                <div>
                  <label className="text-[11px] text-ink-mute block mb-1">
                    Description
                  </label>
                  <input
                    type="text"
                    placeholder="e.g. Walk every day this week"
                    value={modalDesc}
                    onChange={(e) => setModalDesc(e.target.value)}
                    className="w-full p-3 bg-[var(--lavender-bg)] rounded-chip outline-none text-[13px] text-ink-1 placeholder:text-ink-mute"
                  />
                </div>
                <div>
                  <label className="text-[11px] text-ink-mute block mb-1">
                    Target Number
                  </label>
                  <input
                    type="number"
                    min="1"
                    placeholder="e.g. 7"
                    value={modalTarget}
                    onChange={(e) => setModalTarget(e.target.value)}
                    className="w-full p-3 bg-[var(--lavender-bg)] rounded-chip outline-none text-[13px] text-ink-1 placeholder:text-ink-mute"
                  />
                </div>
              </div>

              <div className="flex gap-2">
                <button
                  onClick={handleAddCustom}
                  disabled={!modalName.trim() || !modalDesc.trim() || !modalTarget}
                  className={`flex-1 py-2.5 rounded-chip text-[13px] font-medium ${
                    modalName.trim() && modalDesc.trim() && modalTarget
                      ? 'bg-gradient-to-r from-purple-1 to-purple-2 text-ink-inv'
                      : 'bg-[var(--lavender-bg)] text-ink-mute cursor-not-allowed'
                  }`}
                >
                  Create
                </button>
                <button
                  onClick={() => setShowModal(false)}
                  className="px-4 py-2 text-[13px] text-ink-soft hover:text-ink-1 transition-colors"
                >
                  Cancel
                </button>
              </div>
            </motion.div>
          </motion.div>
        )}
      </AnimatePresence>
    </motion.div>
  );
}
