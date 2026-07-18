import { db, generateId } from './db';
import { categoryColor } from './engine';
import type { Category, Account } from './db';

const DEFAULT_CATEGORIES: Omit<Category, 'id' | 'color'>[] = [
  { name: 'Food & Dining', icon: '🍔', type: 'expense', isDefault: true, isArchived: false, monthlyBudgetCents: null, sortOrder: 0 },
  { name: 'Transport', icon: '🚗', type: 'expense', isDefault: true, isArchived: false, monthlyBudgetCents: null, sortOrder: 1 },
  { name: 'Groceries', icon: '🛒', type: 'expense', isDefault: true, isArchived: false, monthlyBudgetCents: null, sortOrder: 2 },
  { name: 'Shopping', icon: '🛍️', type: 'expense', isDefault: true, isArchived: false, monthlyBudgetCents: null, sortOrder: 3 },
  { name: 'Entertainment', icon: '🎬', type: 'expense', isDefault: true, isArchived: false, monthlyBudgetCents: null, sortOrder: 4 },
  { name: 'Utilities', icon: '💡', type: 'expense', isDefault: true, isArchived: false, monthlyBudgetCents: null, sortOrder: 5 },
  { name: 'Rent', icon: '🏠', type: 'expense', isDefault: true, isArchived: false, monthlyBudgetCents: null, sortOrder: 6 },
  { name: 'Healthcare', icon: '🏥', type: 'expense', isDefault: true, isArchived: false, monthlyBudgetCents: null, sortOrder: 7 },
  { name: 'Education', icon: '📚', type: 'expense', isDefault: true, isArchived: false, monthlyBudgetCents: null, sortOrder: 8 },
  { name: 'Subscriptions', icon: '📱', type: 'expense', isDefault: true, isArchived: false, monthlyBudgetCents: null, sortOrder: 9 },
  { name: 'Travel', icon: '✈️', type: 'expense', isDefault: true, isArchived: false, monthlyBudgetCents: null, sortOrder: 10 },
  { name: 'Personal Care', icon: '💇', type: 'expense', isDefault: true, isArchived: false, monthlyBudgetCents: null, sortOrder: 11 },
  { name: 'Gifts', icon: '🎁', type: 'expense', isDefault: true, isArchived: false, monthlyBudgetCents: null, sortOrder: 12 },
  { name: 'Investments', icon: '📈', type: 'expense', isDefault: true, isArchived: false, monthlyBudgetCents: null, sortOrder: 13 },
  { name: 'Other', icon: '📦', type: 'expense', isDefault: true, isArchived: false, monthlyBudgetCents: null, sortOrder: 14 },
  { name: 'Salary', icon: '💰', type: 'income', isDefault: true, isArchived: false, monthlyBudgetCents: null, sortOrder: 0 },
  { name: 'Freelance', icon: '💻', type: 'income', isDefault: true, isArchived: false, monthlyBudgetCents: null, sortOrder: 1 },
  { name: 'Interest', icon: '🏦', type: 'income', isDefault: true, isArchived: false, monthlyBudgetCents: null, sortOrder: 2 },
  { name: 'Refunds', icon: '↩️', type: 'income', isDefault: true, isArchived: false, monthlyBudgetCents: null, sortOrder: 3 },
  { name: 'Other Income', icon: '💵', type: 'income', isDefault: true, isArchived: false, monthlyBudgetCents: null, sortOrder: 4 },
];

const DEFAULT_ACCOUNTS: Omit<Account, 'id'>[] = [
  { name: 'Cash', bankName: null, accountType: 'cash', last4Digits: null, color: '#10B981', icon: '💵', isActive: true },
  { name: 'Savings', bankName: null, accountType: 'savings', last4Digits: null, color: '#6366F1', icon: '🏦', isActive: true },
  { name: 'Credit Card', bankName: null, accountType: 'credit_card', last4Digits: null, color: '#F97316', icon: '💳', isActive: true },
  { name: 'UPI', bankName: null, accountType: 'upi', last4Digits: null, color: '#22D3EE', icon: '📲', isActive: true },
];

/**
 * Seed default categories and accounts if the database is empty.
 * Safe to call multiple times — only inserts if empty.
 */
export async function seedDefaults(): Promise<void> {
  const categoryCount = await db.categories.count();
  if (categoryCount === 0) {
    const categories: Category[] = DEFAULT_CATEGORIES.map((cat, i) => ({
      ...cat,
      id: generateId(),
      color: categoryColor(i),
    }));
    await db.categories.bulkAdd(categories);
  }

  const accountCount = await db.accounts.count();
  if (accountCount === 0) {
    const accounts: Account[] = DEFAULT_ACCOUNTS.map((acct) => ({
      ...acct,
      id: generateId(),
    }));
    await db.accounts.bulkAdd(accounts);
  }
}
