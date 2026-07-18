package com.nudge.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName

@Serializable
data class Account(
    val id: String,
    val name: String,
    @SerialName("bank_name")
    val bankName: String? = null,
    @SerialName("account_type")
    val accountType: AccountType,
    @SerialName("last4_digits")
    val last4Digits: String? = null,
    val color: String? = null, // hex color for this account
    val icon: String? = null, // icon identifier
    @SerialName("is_active")
    val isActive: Boolean = true
)

@Serializable
enum class AccountType {
    @SerialName("savings") SAVINGS,
    @SerialName("credit_card") CREDIT_CARD,
    @SerialName("upi") UPI,
    @SerialName("wallet") WALLET,
    @SerialName("cash") CASH
}
