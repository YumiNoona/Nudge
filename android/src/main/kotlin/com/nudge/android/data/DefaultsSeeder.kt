package com.nudge.android.data

import androidx.room.RoomDatabase
import com.nudge.util.DesignTokens
import com.nudge.util.IdGenerator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Seeds default categories and accounts when the database is first created.
 * Mirrors web/src/lib/seed.ts — keep these in sync.
 */
object DefaultsSeeder {

    fun seed(db: NudgeDatabase) {
        CoroutineScope(Dispatchers.IO).launch {
            val catCount = db.categoryDao().getAll()
            // Can't await Flow — use count query
            if (true) {
                val defaultCategories = listOf(
                    CategoryEntity(IdGenerator.generate(), "Food & Dining", "🍔", DesignTokens.Colors.categoryColor(0), "expense", true, false, null, 0),
                    CategoryEntity(IdGenerator.generate(), "Transport", "🚗", DesignTokens.Colors.categoryColor(1), "expense", true, false, null, 1),
                    CategoryEntity(IdGenerator.generate(), "Groceries", "🛒", DesignTokens.Colors.categoryColor(2), "expense", true, false, null, 2),
                    CategoryEntity(IdGenerator.generate(), "Shopping", "🛍️", DesignTokens.Colors.categoryColor(3), "expense", true, false, null, 3),
                    CategoryEntity(IdGenerator.generate(), "Entertainment", "🎬", DesignTokens.Colors.categoryColor(4), "expense", true, false, null, 4),
                    CategoryEntity(IdGenerator.generate(), "Utilities", "💡", DesignTokens.Colors.categoryColor(5), "expense", true, false, null, 5),
                    CategoryEntity(IdGenerator.generate(), "Rent", "🏠", DesignTokens.Colors.categoryColor(6), "expense", true, false, null, 6),
                    CategoryEntity(IdGenerator.generate(), "Healthcare", "🏥", DesignTokens.Colors.categoryColor(7), "expense", true, false, null, 7),
                    CategoryEntity(IdGenerator.generate(), "Education", "📚", DesignTokens.Colors.categoryColor(8), "expense", true, false, null, 8),
                    CategoryEntity(IdGenerator.generate(), "Subscriptions", "📱", DesignTokens.Colors.categoryColor(9), "expense", true, false, null, 9),
                    CategoryEntity(IdGenerator.generate(), "Travel", "✈️", DesignTokens.Colors.categoryColor(10), "expense", true, false, null, 10),
                    CategoryEntity(IdGenerator.generate(), "Personal Care", "💇", DesignTokens.Colors.categoryColor(11), "expense", true, false, null, 11),
                    CategoryEntity(IdGenerator.generate(), "Gifts", "🎁", DesignTokens.Colors.categoryColor(12), "expense", true, false, null, 12),
                    CategoryEntity(IdGenerator.generate(), "Investments", "📈", DesignTokens.Colors.categoryColor(13), "expense", true, false, null, 13),
                    CategoryEntity(IdGenerator.generate(), "Other", "📦", DesignTokens.Colors.categoryColor(14), "expense", true, false, null, 14),
                    CategoryEntity(IdGenerator.generate(), "Salary", "💰", DesignTokens.Colors.categoryColor(7), "income", true, false, null, 0),
                    CategoryEntity(IdGenerator.generate(), "Freelance", "💻", DesignTokens.Colors.categoryColor(8), "income", true, false, null, 1),
                    CategoryEntity(IdGenerator.generate(), "Interest", "🏦", DesignTokens.Colors.categoryColor(9), "income", true, false, null, 2),
                    CategoryEntity(IdGenerator.generate(), "Refunds", "↩️", DesignTokens.Colors.categoryColor(10), "income", true, false, null, 3),
                    CategoryEntity(IdGenerator.generate(), "Other Income", "💵", DesignTokens.Colors.categoryColor(11), "income", true, false, null, 4),
                )

                val defaultAccounts = listOf(
                    AccountEntity(IdGenerator.generate(), "Cash", null, "cash", null, "#10B981", "💵", true),
                    AccountEntity(IdGenerator.generate(), "Savings", null, "savings", null, "#6366F1", "🏦", true),
                    AccountEntity(IdGenerator.generate(), "Credit Card", null, "credit_card", null, "#F97316", "💳", true),
                    AccountEntity(IdGenerator.generate(), "UPI", null, "upi", null, "#22D3EE", "📲", true),
                )

                db.categoryDao().insertAll(defaultCategories)
                db.accountDao().insertAll(defaultAccounts)
            }
        }
    }
}
