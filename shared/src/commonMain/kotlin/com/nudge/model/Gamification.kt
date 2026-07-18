package com.nudge.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName
import kotlinx.datetime.Instant

@Serializable
data class GamificationProfile(
    @SerialName("user_id")
    val userId: String,
    @SerialName("xp_total")
    val xpTotal: Long = 0,
    val level: Int = 1,
    @SerialName("current_streak_days")
    val currentStreakDays: Int = 0,
    @SerialName("longest_streak_days")
    val longestStreakDays: Int = 0,
    @SerialName("last_activity_date")
    val lastActivityDate: Instant? = null,
    @SerialName("badges_earned")
    val badgesEarned: List<String> = emptyList(),
    @SerialName("challenges_active")
    val challengesActive: List<String> = emptyList()
)

@Serializable
data class Badge(
    val id: String,
    val name: String,
    val description: String,
    val icon: String, // icon identifier
    @SerialName("is_secret")
    val isSecret: Boolean = false,
    @SerialName("unlock_condition")
    val unlockCondition: String // human-readable unlock condition
)

@Serializable
data class Challenge(
    val id: String,
    val name: String,
    val description: String,
    @SerialName("target_progress")
    val targetProgress: Double, // 0.0 to 100.0
    @SerialName("current_progress")
    val currentProgress: Double = 0.0,
    @SerialName("reward_xp")
    val rewardXp: Long,
    @SerialName("category_id")
    val categoryId: String? = null,
    @SerialName("is_completed")
    val isCompleted: Boolean = false,
    @SerialName("is_custom")
    val isCustom: Boolean = false
)

@Serializable
data class SavingsGoal(
    val id: String,
    val name: String,
    @SerialName("target_amount")
    val targetAmount: Long, // cents
    @SerialName("current_amount")
    val currentAmount: Long = 0, // cents
    @SerialName("visual_metaphor")
    val visualMetaphor: VisualMetaphor = VisualMetaphor.GROWING_PLANT,
    @SerialName("target_date")
    val targetDate: Instant? = null,
    @SerialName("monthly_contribution")
    val monthlyContribution: Long? = null, // cents
    val color: String? = null
)

@Serializable
enum class VisualMetaphor {
    @SerialName("growing_plant") GROWING_PLANT,
    @SerialName("filling_jar") FILLING_JAR,
    @SerialName("building_house") BUILDING_HOUSE,
    @SerialName("launching_rocket") LAUNCHING_ROCKET
}
