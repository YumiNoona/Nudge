package com.nudge.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName
import kotlinx.datetime.Instant

@Serializable
data class Transaction(
    val id: String,
    val amount: Long, // stored in cents — always positive, sign determined by `type`
    val type: TransactionType,
    val currency: String = "INR",
    @SerialName("merchant_raw")
    val merchantRaw: String,
    @SerialName("merchant_normalized")
    val merchantNormalized: String? = null,
    @SerialName("category_id")
    val categoryId: String? = null,
    @SerialName("subcategory_id")
    val subcategoryId: String? = null,
    @SerialName("account_id")
    val accountId: String,
    val source: TransactionSource,
    @SerialName("source_raw_text")
    val sourceRawText: String? = null,
    @SerialName("confidence_score")
    val confidenceScore: Float = 1.0f,
    @SerialName("is_reviewed")
    val isReviewed: Boolean = false,
    @SerialName("is_recurring")
    val isRecurring: Boolean = false,
    @SerialName("recurring_group_id")
    val recurringGroupId: String? = null,
    val note: String? = null,
    val tags: List<String> = emptyList(),
    val timestamp: Instant,
    @SerialName("created_at")
    val createdAt: Instant? = null,
    @SerialName("updated_at")
    val updatedAt: Instant? = null
)

@Serializable
enum class TransactionType {
    @SerialName("debit") DEBIT,
    @SerialName("credit") CREDIT,
    @SerialName("refund") REFUND,
    @SerialName("transfer") TRANSFER
}

@Serializable
enum class TransactionSource {
    @SerialName("sms") SMS,
    @SerialName("notification") NOTIFICATION,
    @SerialName("manual") MANUAL,
    @SerialName("csv_import") CSV_IMPORT,
    @SerialName("recurring_rule") RECURRING_RULE
}
