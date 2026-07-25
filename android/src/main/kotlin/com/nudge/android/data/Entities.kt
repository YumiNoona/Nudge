package com.nudge.android.data

import androidx.room.*

@Entity(tableName = "accounts")
data class AccountEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "bank_name") val bankName: String?,
    @ColumnInfo(name = "account_type") val accountType: String, // AccountType name
    @ColumnInfo(name = "last4_digits") val last4Digits: String?,
    @ColumnInfo(name = "color") val color: String?,
    @ColumnInfo(name = "icon") val icon: String?,
    @ColumnInfo(name = "is_active") val isActive: Boolean = true
)

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "icon") val icon: String? = null,
    @ColumnInfo(name = "color") val color: String? = null,
    @ColumnInfo(name = "type") val type: String, // CategoryType name
    @ColumnInfo(name = "is_default") val isDefault: Boolean = false,
    @ColumnInfo(name = "is_archived") val isArchived: Boolean = false,
    @ColumnInfo(name = "monthly_budget_cents") val monthlyBudgetCents: Long? = null,
    @ColumnInfo(name = "sort_order") val sortOrder: Int = 0
)

@Entity(
    tableName = "subcategories",
    foreignKeys = [ForeignKey(
        entity = CategoryEntity::class,
        parentColumns = ["id"],
        childColumns = ["category_id"],
        onDelete = ForeignKey.CASCADE
    )]
)
data class SubcategoryEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "category_id") val categoryId: String,
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "icon") val icon: String? = null
)

@Entity(tableName = "budgets")
data class BudgetEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "category_id") val categoryId: String?,
    @ColumnInfo(name = "amount_cents") val amountCents: Long,
    @ColumnInfo(name = "period") val period: String,
    @ColumnInfo(name = "rollover_enabled") val rolloverEnabled: Boolean = false,
    @ColumnInfo(name = "start_date_epoch") val startDateEpoch: Long
)

@Entity(tableName = "recurring_rules")
data class RecurringRuleEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "merchant_pattern") val merchantPattern: String,
    @ColumnInfo(name = "min_amount_cents") val minAmountCents: Long?,
    @ColumnInfo(name = "max_amount_cents") val maxAmountCents: Long?,
    @ColumnInfo(name = "expected_day") val expectedDay: Int?,
    @ColumnInfo(name = "interval") val interval: String?,
    @ColumnInfo(name = "category_id") val categoryId: String?,
    @ColumnInfo(name = "next_expected_date") val nextExpectedDate: Long?,
    @ColumnInfo(name = "status") val status: String = "active"
)
