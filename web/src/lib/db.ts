import Dexie, { Table } from 'dexie';
import { v4 as uuidv4 } from 'uuid';

// --- Shared types mirroring KMP data model ---

export type TransactionType = 'debit' | 'credit' | 'refund' | 'transfer';
export type TransactionSource = 'sms' | 'notification' | 'manual' | 'csv_import' | 'recurring_rule';
export type AccountType = 'savings' | 'credit_card' | 'upi' | 'wallet' | 'cash';
export type CategoryType = 'expense' | 'income';
export type RecurringStatus = 'active' | 'paused';
export type RecurringInterval = 'daily' | 'weekly' | 'biweekly' | 'monthly' | 'quarterly' | 'yearly';
export type BudgetPeriod = 'weekly' | 'monthly' | 'custom';

export interface Transaction {
  id: string;
  amountCents: number; // always positive, sign via type
  type: TransactionType;
  currency: string;
  merchantRaw: string;
  merchantNormalized: string | null;
  categoryId: string | null;
  subcategoryId: string | null;
  accountId: string;
  source: TransactionSource;
  sourceRawText: string | null;
  confidenceScore: number;
  isReviewed: boolean;
  isRecurring: boolean;
  recurringGroupId: string | null;
  note: string | null;
  tags: string[];
  timestampEpoch: number;
  createdAt: number;
  updatedAt: number;
}

export interface Account {
  id: string;
  name: string;
  bankName: string | null;
  accountType: AccountType;
  last4Digits: string | null;
  color: string | null;
  icon: string | null;
  isActive: boolean;
}

export interface Category {
  id: string;
  name: string;
  icon: string | null;
  color: string | null;
  type: CategoryType;
  isDefault: boolean;
  isArchived: boolean;
  monthlyBudgetCents: number | null;
  sortOrder: number;
}

export interface Budget {
  id: string;
  categoryId: string | null;
  amountCents: number;
  period: BudgetPeriod;
  rolloverEnabled: boolean;
  startDateEpoch: number;
}

export interface GamificationProfile {
  userId: string;
  xpTotal: number;
  level: number;
  currentStreakDays: number;
  longestStreakDays: number;
  lastActivityDate: number | null;
  badgesJson: string;
  challengesJson: string;
  streakFreezes: number;
  consistentDays: number;
}

export interface MerchantAlias {
  id: string;
  rawPattern: string;
  normalizedName: string;
  suggestedCategoryId: string | null;
  createdAt: number;
}

// --- Dexie.js Database ---

class NudgeWebDB extends Dexie {
  transactions!: Table<Transaction, string>;
  accounts!: Table<Account, string>;
  categories!: Table<Category, string>;
  budgets!: Table<Budget, string>;
  gamificationProfile!: Table<GamificationProfile, string>;
  merchantAliases!: Table<MerchantAlias, string>;

  constructor() {
    super('NudgeDB');
    this.version(2).stores({
      transactions: 'id, accountId, categoryId, type, timestampEpoch, isReviewed, isRecurring, source',
      accounts: 'id, accountType, isActive',
      categories: 'id, type, isArchived, sortOrder',
      budgets: 'id, categoryId, period',
      gamificationProfile: 'userId',
      merchantAliases: 'id, rawPattern',
    });
  }
}

export const db = new NudgeWebDB();

// --- Helpers ---

export function generateId(): string {
  return uuidv4();
}

export function formatCurrency(cents: number, currency: string = 'INR'): string {
  const formatter = new Intl.NumberFormat('en-IN', {
    style: 'currency',
    currency,
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  });
  return formatter.format(cents / 100);
}

export function formatAmount(cents: number): string {
  return (cents / 100).toLocaleString('en-IN', {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  });
}
