package com.nudge.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName
import kotlinx.datetime.Instant

@Serializable
data class RecurringRule(
    val id: String,
    @SerialName("merchant_pattern")
    val merchantPattern: String,
    @SerialName("expected_amount_range")
    val expectedAmountRange: AmountRange? = null,
    @SerialName("expected_day_of_month")
    val expectedDayOfMonth: Int? = null,
    val interval: RecurringInterval? = null,
    @SerialName("category_id")
    val categoryId: String? = null,
    @SerialName("next_expected_date")
    val nextExpectedDate: Instant? = null,
    val status: RecurringStatus = RecurringStatus.ACTIVE
)

@Serializable
data class AmountRange(
    val min: Long, // cents
    val max: Long  // cents
)

@Serializable
enum class RecurringStatus {
    @SerialName("active") ACTIVE,
    @SerialName("paused") PAUSED
}

@Serializable
enum class RecurringInterval {
    @SerialName("daily") DAILY,
    @SerialName("weekly") WEEKLY,
    @SerialName("biweekly") BIWEEKLY,
    @SerialName("monthly") MONTHLY,
    @SerialName("quarterly") QUARTERLY,
    @SerialName("yearly") YEARLY
}

@Serializable
data class Budget(
    val id: String,
    @SerialName("category_id")
    val categoryId: String? = null, // null = overall budget
    val amount: Long, // cents
    val period: BudgetPeriod,
    @SerialName("rollover_enabled")
    val rolloverEnabled: Boolean = false,
    @SerialName("start_date")
    val startDate: Instant
)

@Serializable
enum class BudgetPeriod {
    @SerialName("weekly") WEEKLY,
    @SerialName("monthly") MONTHLY,
    @SerialName("custom") CUSTOM
}
