import { useMemo } from 'react';
import { motion } from 'framer-motion';
import type { GamificationProfile } from '../lib/db';
import { levelTitle, levelProgress, XP_REVIEW_TRANSACTION } from '../lib/engine';

interface BadgeData {
  id: string;
  name: string;
  description: string;
  icon: string;
  isSecret: boolean;
  unlockCondition: string;
}

interface Props {
  gamificationProfile: GamificationProfile | null;
  onBack: () => void;
}

const BADGES: BadgeData[] = [
  {
    id: 'first-blood',
    name: 'First Blood',
    description: 'Log your first transaction',
    icon: '\uD83E\uDE78',
    isSecret: false,
    unlockCondition: 'Log your first transaction',
  },
  {
    id: 'detective',
    name: 'Detective',
    description: 'Correct 10 mis-categorized transactions',
    icon: '\uD83D\uDD0D',
    isSecret: false,
    unlockCondition: 'Correct 10 mis-categorized transactions',
  },
  {
    id: 'under-budget',
    name: 'Under Budget',
    description: 'Full month under budget in 3+ categories',
    icon: '\uD83C\uDFAF',
    isSecret: false,
    unlockCondition: 'Stay under budget in 3+ categories for a full month',
  },
  {
    id: 'no-spend-day',
    name: 'No-Spend Day',
    description: 'Zero discretionary spend day',
    icon: '\uD83C\uDF34',
    isSecret: false,
    unlockCondition: 'Complete a day with zero discretionary spending',
  },
  {
    id: 'no-spend-week',
    name: 'No-Spend Week',
    description: '7 consecutive no-spend days',
    icon: '\uD83C\uDFDD\uFE0F',
    isSecret: false,
    unlockCondition: 'Complete 7 consecutive days with zero discretionary spending',
  },
  {
    id: 'subscription-slayer',
    name: 'Subscription Slayer',
    description: 'Cancel a recurring subscription',
    icon: '\uD83D\uDDE1\uFE0F',
    isSecret: false,
    unlockCondition: 'Cancel or pause a recurring subscription',
  },
  {
    id: 'early-bird',
    name: 'Early Bird',
    description: 'Review before 9am, 5 days running',
    icon: '\uD83C\uDF05',
    isSecret: false,
    unlockCondition: 'Open the app and review transactions before 9am for 5 consecutive days',
  },
  {
    id: 'night-owl',
    name: 'Night Owl',
    description: 'Review after 10pm, 5 days running',
    icon: '\uD83E\uDD89',
    isSecret: true,
    unlockCondition: 'Open the app and review transactions after 10pm for 5 consecutive days',
  },
  {
    id: 'centurion',
    name: 'Centurion',
    description: '100 transactions',
    icon: '\uD83D\uDCAF',
    isSecret: false,
    unlockCondition: 'Log 100 total transactions',
  },
  {
    id: 'half-millionaire',
    name: 'Half-Millionaire',
    description: '500 transactions',
    icon: '\uD83C\uDFE6',
    isSecret: false,
    unlockCondition: 'Log 500 total transactions',
  },
  {
    id: 'thousandaire',
    name: 'Thousandaire',
    description: '1,000 transactions',
    icon: '\uD83D\uDC51',
    isSecret: false,
    unlockCondition: 'Log 1,000 total transactions',
  },
  {
    id: 'penny-pincher',
    name: 'Penny Pincher',
    description: 'Under budget in ALL categories for a month',
    icon: '\uD83E\uDE99',
    isSecret: true,
    unlockCondition: 'Stay under budget in every active category for a full month',
  },
  {
    id: 'big-spender',
    name: 'Big Spender',
    description: 'Single transaction over ₹10,000',
    icon: '\uD83D\uDC8E',
    isSecret: false,
    unlockCondition: 'Make a single transaction worth over ₹10,000',
  },
  {
    id: 'diversified',
    name: 'Diversified',
    description: 'Use 8+ different categories',
    icon: '\uD83C\uDFA8',
    isSecret: false,
    unlockCondition: 'Categorize transactions across 8 or more different categories',
  },
  {
    id: 'category-master',
    name: 'Category Master',
    description: 'Create a custom category',
    icon: '\uD83D\uDCCA',
    isSecret: false,
    unlockCondition: 'Create your first custom category',
  },
  {
    id: 'tag-team',
    name: 'Tag Team',
    description: 'Use tags on 20+ transactions',
    icon: '\uD83C\uDFF7\uFE0F',
    isSecret: false,
    unlockCondition: 'Add tags to 20 or more transactions',
  },
  {
    id: 'note-taker',
    name: 'Note Taker',
    description: 'Add notes to 30+ transactions',
    icon: '\uD83D\uDCDD',
    isSecret: false,
    unlockCondition: 'Add notes to 30 or more transactions',
  },
  {
    id: 'receipt-keeper',
    name: 'Receipt Keeper',
    description: 'Attach 10+ receipt photos',
    icon: '\uD83D\uDCF8',
    isSecret: false,
    unlockCondition: 'Attach receipt photos to 10 or more transactions',
  },
  {
    id: 'csv-wizard',
    name: 'CSV Wizard',
    description: 'Import transactions via CSV',
    icon: '\uD83D\uDCC4',
    isSecret: true,
    unlockCondition: 'Import transactions from a CSV file',
  },
  {
    id: 'backup-hero',
    name: 'Backup Hero',
    description: 'Complete a full data backup',
    icon: '\uD83D\uDCBE',
    isSecret: true,
    unlockCondition: 'Export a full backup of all your financial data',
  },
  {
    id: 'manual-maven',
    name: 'Manual Maven',
    description: '50 manual transactions',
    icon: '\u270D\uFE0F',
    isSecret: false,
    unlockCondition: 'Manually enter 50 transactions',
  },
  {
    id: 'sms-sniper',
    name: 'SMS Sniper',
    description: '20+ SMS-captured transactions',
    icon: '\uD83D\uDCF2',
    isSecret: false,
    unlockCondition: 'Capture 20 transactions from SMS notifications',
  },
  {
    id: 'week-streak',
    name: 'Week Streak',
    description: '7-day streak',
    icon: '\uD83D\uDD25',
    isSecret: false,
    unlockCondition: 'Maintain an activity streak for 7 consecutive days',
  },
  {
    id: 'month-streak',
    name: 'Month Streak',
    description: '30-day streak',
    icon: '\uD83D\uDD25\uD83D\uDD25',
    isSecret: false,
    unlockCondition: 'Maintain an activity streak for 30 consecutive days',
  },
  {
    id: 'century-streak',
    name: 'Century Streak',
    description: '100-day streak',
    icon: '\uD83D\uDD25\uD83D\uDD25\uD83D\uDD25',
    isSecret: false,
    unlockCondition: 'Maintain an activity streak for 100 consecutive days',
  },
  {
    id: 'year-streak',
    name: 'Year Streak',
    description: '365-day streak',
    icon: '\uD83D\uDD25\uD83C\uDFC6',
    isSecret: false,
    unlockCondition: 'Maintain an activity streak for 365 consecutive days',
  },
  {
    id: 'budget-rookie',
    name: 'Budget Rookie',
    description: 'First budget',
    icon: '\uD83C\uDF31',
    isSecret: false,
    unlockCondition: 'Create your first budget',
  },
  {
    id: 'budget-pro',
    name: 'Budget Pro',
    description: '5+ active budgets',
    icon: '\uD83D\uDCC8',
    isSecret: false,
    unlockCondition: 'Have 5 or more active budgets simultaneously',
  },
  {
    id: 'rollover-king',
    name: 'Rollover King',
    description: '3 months rollover budgets',
    icon: '\uD83D\uDD04',
    isSecret: false,
    unlockCondition: 'Use rollover budgets for 3 consecutive months',
  },
  {
    id: 'goal-getter',
    name: 'Goal Getter',
    description: 'Complete a savings goal',
    icon: '\uD83C\uDFAF',
    isSecret: false,
    unlockCondition: 'Reach the target amount for a savings goal',
  },
  {
    id: 'goal-crusher',
    name: 'Goal Crusher',
    description: '5 savings goals',
    icon: '\uD83D\uDCAA',
    isSecret: false,
    unlockCondition: 'Complete 5 savings goals',
  },
  {
    id: 'challenge-accepted',
    name: 'Challenge Accepted',
    description: 'Complete a weekly challenge',
    icon: '\uD83C\uDFC5',
    isSecret: false,
    unlockCondition: 'Complete one weekly challenge',
  },
  {
    id: 'challenge-champion',
    name: 'Challenge Champion',
    description: '10 challenges',
    icon: '\uD83C\uDFC6',
    isSecret: false,
    unlockCondition: 'Complete 10 weekly challenges',
  },
  {
    id: 'accountable',
    name: 'Accountable',
    description: '3+ accounts',
    icon: '\uD83C\uDFE6',
    isSecret: false,
    unlockCondition: 'Link 3 or more financial accounts',
  },
  {
    id: 'multi-currency',
    name: 'Multi-Currency',
    description: 'Multiple currencies',
    icon: '\uD83C\uDF0D',
    isSecret: false,
    unlockCondition: 'Log transactions in more than one currency',
  },
  {
    id: 'dark-mode-dweller',
    name: 'Dark Mode Dweller',
    description: 'Dark mode 7 days',
    icon: '\uD83C\uDF19',
    isSecret: true,
    unlockCondition: 'Use dark mode for 7 consecutive days',
  },
  {
    id: 'data-detective',
    name: 'Data Detective',
    description: 'Analytics 10 times',
    icon: '\uD83D\uDCCA',
    isSecret: false,
    unlockCondition: 'Visit the analytics page 10 times',
  },
  {
    id: 'envelope-master',
    name: 'Envelope Master',
    description: 'Envelope budgeting full month',
    icon: '\u2709\uFE0F',
    isSecret: true,
    unlockCondition: 'Use envelope budgeting for a full calendar month',
  },
  {
    id: 'subscription-watcher',
    name: 'Subscription Watcher',
    description: '3+ recurring subscriptions',
    icon: '\uD83D\uDC40',
    isSecret: false,
    unlockCondition: 'Track 3 or more recurring subscriptions',
  },
  {
    id: 'clean-slate',
    name: 'Clean Slate',
    description: 'Review all pending in one session',
    icon: '\uD83E\uDDF9',
    isSecret: true,
    unlockCondition: 'Review every pending transaction in a single session',
  },
  {
    id: 'weekend-warrior',
    name: 'Weekend Warrior',
    description: '7 weekends in a row',
    icon: '\u2694\uFE0F',
    isSecret: true,
    unlockCondition: 'Log or review at least one transaction every weekend for 7 weeks',
  },
  {
    id: 'payday-pro',
    name: 'Payday Pro',
    description: 'Income in 6 different months',
    icon: '\uD83D\uDCB0',
    isSecret: false,
    unlockCondition: 'Record income transactions in 6 different calendar months',
  },
];

const containerVariants = {
  hidden: { opacity: 0 },
  visible: {
    opacity: 1,
    transition: { staggerChildren: 0.03 },
  },
};

const itemVariants = {
  hidden: { opacity: 0, scale: 0.8 },
  visible: {
    opacity: 1,
    scale: 1,
    transition: { type: 'spring', stiffness: 260, damping: 20 },
  },
};

export default function AchievementsScreen({ gamificationProfile, onBack }: Props) {
  const badgesEarned = useMemo(() => {
    if (!gamificationProfile?.badgesJson) return new Set<string>();
    try {
      return new Set<string>(JSON.parse(gamificationProfile.badgesJson) as string[]);
    } catch {
      return new Set<string>();
    }
  }, [gamificationProfile]);

  const earnedCount = useMemo(
    () => BADGES.filter((b) => badgesEarned.has(b.id)).length,
    [badgesEarned],
  );

  const level = gamificationProfile?.level ?? 1;
  const xp = gamificationProfile?.xpTotal ?? 0;
  const progress = gamificationProfile ? levelProgress(xp) : 0;

  if (!gamificationProfile) {
    return (
      <div className="min-h-screen bg-surface-base flex items-center justify-center">
        <div className="text-center p-4">
          <span className="text-5xl block mb-4">🏆</span>
          <h2 className="text-title font-bold text-content-primary mb-2">
            No Gamification Profile
          </h2>
          <p className="text-caption text-content-secondary mb-6">
            Start using Nudge to unlock achievements and earn XP
          </p>
          <button
            onClick={onBack}
            className="px-6 py-2 bg-accent-primary text-white rounded-pill text-body font-medium hover:bg-accent-primary/90 transition-colors"
          >
            Go Back
          </button>
        </div>
      </div>
    );
  }

  return (
    <motion.div
      className="min-h-screen bg-surface-base"
      initial="hidden"
      animate="visible"
      variants={containerVariants}
    >
      <div className="max-w-4xl mx-auto p-4">
        {/* Header */}
        <div className="flex items-center justify-between mb-4">
          <button
            onClick={onBack}
            className="text-content-secondary text-body hover:text-content-primary transition-colors"
          >
            ← Back
          </button>
          <h1 className="text-title font-display font-bold text-content-primary">
            Achievements
          </h1>
          <div className="w-16" />
        </div>

        {/* Level & XP */}
        <div className="p-4 bg-surface-raised rounded-xl mb-6" style={{ boxShadow: 'var(--shadow-md)' }}>
          <div className="flex items-center justify-between mb-2">
            <div>
              <p className="text-caption text-content-secondary">
                Level {level} · {levelTitle(level)}
              </p>
              <p className="text-display font-display font-bold text-accent-primary mt-1">
                {earnedCount} <span className="text-title text-content-tertiary">/ 42</span>
              </p>
            </div>
            <div className="text-right">
              <p className="text-micro text-content-tertiary">{xp.toLocaleString('en-IN')} XP</p>
              <p className="text-caption text-content-secondary mt-1">
                +{XP_REVIEW_TRANSACTION} XP per review
              </p>
            </div>
          </div>
          {/* XP progress bar */}
          <div className="h-2 bg-surface-base rounded-full overflow-hidden">
            <div
              className="h-full bg-accent-primary rounded-full transition-all duration-500"
              style={{ width: `${progress * 100}%` }}
            />
          </div>
        </div>

        {/* Badge count */}
        <p className="text-caption text-content-secondary mb-4">
          {earnedCount} of {BADGES.length} badges unlocked
        </p>

        {/* Badge grid */}
        <motion.div
          className="grid grid-cols-3 sm:grid-cols-4 md:grid-cols-5 gap-3"
          variants={containerVariants}
          initial="hidden"
          animate="visible"
        >
          {BADGES.map((badge) => {
            const isEarned = badgesEarned.has(badge.id);

            if (!isEarned && badge.isSecret) {
              return (
                <motion.div
                  key={badge.id}
                  variants={itemVariants}
                  className="flex flex-col items-center p-3 bg-surface-raised rounded-xl border border-content-tertiary/20 opacity-40"
                  style={{ boxShadow: 'var(--shadow-sm)' }}
                >
                  <span className="text-3xl mb-1">❓</span>
                  <span className="text-micro text-content-tertiary text-center leading-tight">
                    ???
                  </span>
                </motion.div>
              );
            }

            return (
              <motion.div
                key={badge.id}
                variants={itemVariants}
                className={`flex flex-col items-center p-3 rounded-xl transition-colors ${
                  isEarned
                    ? 'bg-surface-raised ring-2 ring-accent-primary'
                    : 'bg-surface-raised opacity-40'
                }`}
                style={{ boxShadow: 'var(--shadow-sm)' }}
              >
                <div className="relative">
                  <span className={`text-3xl ${!isEarned ? 'grayscale' : ''}`}>
                    {badge.icon}
                  </span>
                  {!isEarned && (
                    <span className="absolute -bottom-1 -right-1 text-xs">🔒</span>
                  )}
                </div>
                <span
                  className={`text-micro text-center leading-tight mt-1 font-medium ${
                    isEarned ? 'text-content-primary' : 'text-content-tertiary'
                  }`}
                >
                  {isEarned ? badge.name : '???'}
                </span>
                {isEarned && (
                  <span className="text-micro text-content-tertiary text-center leading-tight mt-0.5 line-clamp-2">
                    {badge.description}
                  </span>
                )}
              </motion.div>
            );
          })}
        </motion.div>

        <div className="h-8" />
      </div>
    </motion.div>
  );
}
