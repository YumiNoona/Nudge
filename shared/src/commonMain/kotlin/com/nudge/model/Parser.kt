package com.nudge.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName

@Serializable
data class ParserRule(
    val id: String,
    @SerialName("bank_name")
    val bankName: String,
    @SerialName("regex_pattern")
    val regexPattern: String,
    @SerialName("field_mapping")
    val fieldMapping: FieldMapping,
    @SerialName("sample_matches")
    val sampleMatches: List<String> = emptyList(),
    @SerialName("is_user_created")
    val isUserCreated: Boolean = false,
    @SerialName("is_verified")
    val isVerified: Boolean = false
)

@Serializable
data class FieldMapping(
    @SerialName("amount_group")
    val amountGroup: Int = 1,
    @SerialName("merchant_group")
    val merchantGroup: Int = 2,
    @SerialName("date_group")
    val dateGroup: Int? = null,
    @SerialName("account_group")
    val accountGroup: Int? = null,
    @SerialName("transaction_type_hint")
    val transactionTypeHint: TransactionType? = null
)

@Serializable
data class MerchantAlias(
    val id: String,
    @SerialName("raw_pattern")
    val rawPattern: String, // regex or exact text to match
    @SerialName("normalized_name")
    val normalizedName: String,
    val category: String? = null, // suggested category id
    val icon: String? = null // suggested icon
)

@Serializable
data class SenderWhitelist(
    val id: String,
    @SerialName("sender_id")
    val senderId: String, // SMS sender code or notification package name
    @SerialName("bank_name")
    val bankName: String,
    val country: String = "IN",
    @SerialName("is_active")
    val isActive: Boolean = true
)
