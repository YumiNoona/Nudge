package com.nudge.android.data

import com.nudge.util.DesignTokens
import com.nudge.util.IdGenerator

object DefaultsSeeder {
    suspend fun seedIfEmpty(db: NudgeDatabase) {
        // Count only categories that the UI can actually display. Archived rows used to make the
        // old `COUNT(*)` check pass while Add Transaction received an empty list and showed only +.
        val visibleCategories = db.categoryDao().getAllOnce()
        val defaultCategories = listOf(
            CategoryEntity(IdGenerator.generate(), "Food & Dining", "\uD83C\uDF54", DesignTokens.Colors.categoryColor(0), "expense", true, false, null, 0),
            CategoryEntity(IdGenerator.generate(), "Transport", "\uD83D\uDE97", DesignTokens.Colors.categoryColor(1), "expense", true, false, null, 1),
            CategoryEntity(IdGenerator.generate(), "Groceries", "\uD83D\uDED2", DesignTokens.Colors.categoryColor(2), "expense", true, false, null, 2),
            CategoryEntity(IdGenerator.generate(), "Shopping", "\uD83D\uDECD\uFE0F", DesignTokens.Colors.categoryColor(3), "expense", true, false, null, 3),
            CategoryEntity(IdGenerator.generate(), "Entertainment", "\uD83C\uDFAC", DesignTokens.Colors.categoryColor(4), "expense", true, false, null, 4),
            CategoryEntity(IdGenerator.generate(), "Utilities", "\uD83D\uDCA1", DesignTokens.Colors.categoryColor(5), "expense", true, false, null, 5),
            CategoryEntity(IdGenerator.generate(), "Rent", "\uD83C\uDFE0", DesignTokens.Colors.categoryColor(6), "expense", true, false, null, 6),
            CategoryEntity(IdGenerator.generate(), "Healthcare", "\uD83C\uDFE5", DesignTokens.Colors.categoryColor(7), "expense", true, false, null, 7),
            CategoryEntity(IdGenerator.generate(), "Education", "\uD83D\uDCDA", DesignTokens.Colors.categoryColor(8), "expense", true, false, null, 8),
            CategoryEntity(IdGenerator.generate(), "Subscriptions", "\uD83D\uDCF1", DesignTokens.Colors.categoryColor(9), "expense", true, false, null, 9),
            CategoryEntity(IdGenerator.generate(), "Travel", "\u2708\uFE0F", DesignTokens.Colors.categoryColor(10), "expense", true, false, null, 10),
            CategoryEntity(IdGenerator.generate(), "Personal Care", "\uD83D\uDC87", DesignTokens.Colors.categoryColor(11), "expense", true, false, null, 11),
            CategoryEntity(IdGenerator.generate(), "Gifts", "\uD83C\uDF81", DesignTokens.Colors.categoryColor(12), "expense", true, false, null, 12),
            CategoryEntity(IdGenerator.generate(), "Investments", "\uD83D\uDCC8", DesignTokens.Colors.categoryColor(13), "expense", true, false, null, 13),
            CategoryEntity(IdGenerator.generate(), "Other", "\uD83D\uDCE6", DesignTokens.Colors.categoryColor(14), "expense", true, false, null, 14),
            CategoryEntity(IdGenerator.generate(), "Salary", "\uD83D\uDCB0", DesignTokens.Colors.categoryColor(7), "income", true, false, null, 0),
            CategoryEntity(IdGenerator.generate(), "Freelance", "\uD83D\uDCBB", DesignTokens.Colors.categoryColor(8), "income", true, false, null, 1),
            CategoryEntity(IdGenerator.generate(), "Interest", "\uD83C\uDFE6", DesignTokens.Colors.categoryColor(9), "income", true, false, null, 2),
            CategoryEntity(IdGenerator.generate(), "Refunds", "\u21A9\uFE0F", DesignTokens.Colors.categoryColor(10), "income", true, false, null, 3),
            CategoryEntity(IdGenerator.generate(), "Other Income", "\uD83D\uDCB5", DesignTokens.Colors.categoryColor(11), "income", true, false, null, 4),
        )

        val missingCategoryTypes = setOf("expense", "income") - visibleCategories.map { it.type }.toSet()
        if (missingCategoryTypes.isNotEmpty()) {
            db.categoryDao().insertAll(defaultCategories.filter { it.type in missingCategoryTypes })
        }

        // Likewise, inactive/archived accounts are not selectable and must not block recovery.
        if (db.accountDao().getAllOnce().isEmpty()) {
            val defaultAccounts = listOf(
                AccountEntity(IdGenerator.generate(), "Cash", null, "cash", null, "#10B981", "\uD83D\uDCB5", isActive = true, isDefault = true),
                AccountEntity(IdGenerator.generate(), "Savings", null, "savings", null, "#3E6F8E", "\uD83C\uDFE6", isActive = true),
                AccountEntity(IdGenerator.generate(), "Credit Card", null, "credit_card", null, "#F97316", "\uD83D\uDCB3", isActive = true),
                AccountEntity(IdGenerator.generate(), "UPI", null, "upi", null, "#22D3EE", "\uD83D\uDCF2", isActive = true),
            )
            db.accountDao().insertAll(defaultAccounts)
        }
    }
}
