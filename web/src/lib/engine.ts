// --- Gamification Math (mirrors KMP GamificationMath) ---

export function xpForLevel(level: number): number {
  return Math.round(100 * Math.pow(level, 1.5));
}

export function levelFromXp(totalXp: number): number {
  let level = 1;
  while (xpRequiredUpToLevel(level) <= totalXp) {
    level++;
  }
  return Math.max(1, level);
}

function xpRequiredUpToLevel(level: number): number {
  let total = 0;
  for (let i = 1; i <= level; i++) {
    total += xpForLevel(i);
  }
  return total;
}

export function levelProgress(totalXp: number): number {
  const level = levelFromXp(totalXp);
  const xpIntoLevel = totalXp - xpRequiredUpToLevel(level - 1);
  const xpNeeded = xpForLevel(level);
  return xpNeeded > 0 ? xpIntoLevel / xpNeeded : 1;
}

export function levelTitle(level: number): string {
  if (level <= 5) return 'Budget Rookie';
  if (level <= 10) return 'Coin Collector';
  if (level <= 18) return 'Saving Scout';
  if (level <= 28) return 'Spending Sensei';
  if (level <= 40) return 'Finance Ninja';
  if (level <= 55) return 'Wealth Wizard';
  if (level <= 75) return 'Money Mogul';
  return 'Nudge Legend';
}

// --- Budget Math (mirrors KMP BudgetMath) ---

import type { Transaction, Budget, BudgetPeriod } from './db';

export function totalSpend(
  transactions: Transaction[],
  categoryId: string,
  startEpoch: number,
  endEpoch: number
): number {
  return transactions
    .filter(
      (t) =>
        t.categoryId === categoryId &&
        t.type === 'debit' &&
        t.timestampEpoch >= startEpoch &&
        t.timestampEpoch <= endEpoch
    )
    .reduce((sum, t) => sum + t.amountCents, 0);
}

export function budgetProgress(spent: number, budgetAmount: number): number {
  if (budgetAmount === 0) return 0;
  return spent / budgetAmount;
}

export function dailySuggestedSpend(
  spent: number,
  budgetAmount: number,
  remainingDays: number
): number {
  if (remainingDays <= 0) return budgetAmount - spent;
  return Math.floor((budgetAmount - spent) / remainingDays);
}

// --- Category color palette (mirrors KMP DesignTokens) ---

export const CATEGORY_COLORS = [
  '#6366F1', '#10B981', '#F59E0B', '#EF4444',
  '#8B5CF6', '#EC4899', '#06B6D4', '#F97316',
  '#84CC16', '#14B8A6', '#E11D48', '#7C3AED',
  '#0EA5E9', '#D946EF', '#22D3EE', '#A855F7',
];

export function categoryColor(index: number): string {
  return CATEGORY_COLORS[index % CATEGORY_COLORS.length];
}

// --- XP Rewards ---

export const XP_REVIEW_TRANSACTION = 5;
export const XP_MANUAL_ENTRY_SAME_DAY = 5;
export const XP_UNDER_BUDGET_WEEKLY = 20;
export const XP_SAVINGS_MILESTONE = 50;
export const XP_DAILY_CHECKIN = 2;
