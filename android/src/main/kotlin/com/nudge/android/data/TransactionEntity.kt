package com.nudge.android.data

import androidx.room.*

@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "amount_cents") val amountCents: Long,
    @ColumnInfo(name = "type") val type: String, // TransactionType name
    @ColumnInfo(name = "currency") val currency: String = "INR",
    @ColumnInfo(name = "merchant_raw") val merchantRaw: String,
    @ColumnInfo(name = "merchant_normalized") val merchantNormalized: String?,
    @ColumnInfo(name = "category_id") val categoryId: String?,
    @ColumnInfo(name = "subcategory_id") val subcategoryId: String?,
    @ColumnInfo(name = "account_id") val accountId: String,
    @ColumnInfo(name = "source") val source: String, // TransactionSource name
    @ColumnInfo(name = "source_raw_text") val sourceRawText: String?,
    @ColumnInfo(name = "confidence_score") val confidenceScore: Float = 1f,
    @ColumnInfo(name = "is_reviewed") val isReviewed: Boolean = false,
    @ColumnInfo(name = "is_recurring") val isRecurring: Boolean = false,
    @ColumnInfo(name = "recurring_group_id") val recurringGroupId: String?,
    @ColumnInfo(name = "note") val note: String?,
    @ColumnInfo(name = "tags_json") val tagsJson: String = "[]", // JSON array string
    @ColumnInfo(name = "timestamp_epoch") val timestampEpoch: Long,
    @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "updated_at") val updatedAt: Long = System.currentTimeMillis()
)
