package com.nudge.android.data

import androidx.room.*

@Entity(tableName = "gamification_profile")
data class GamificationProfileEntity(
    @PrimaryKey val userId: String,
    @ColumnInfo(name = "xp_total") val xpTotal: Long = 0,
    @ColumnInfo(name = "level") val level: Int = 1,
    @ColumnInfo(name = "current_streak_days") val currentStreakDays: Int = 0,
    @ColumnInfo(name = "longest_streak_days") val longestStreakDays: Int = 0,
    @ColumnInfo(name = "last_activity_date") val lastActivityDate: Long? = null,
    @ColumnInfo(name = "badges_json") val badgesJson: String = "[]",
    @ColumnInfo(name = "challenges_json") val challengesJson: String = "[]",
    @ColumnInfo(name = "streak_freezes") val streakFreezes: Int = 0,
    @ColumnInfo(name = "consistent_days") val consistentDays: Int = 0
)

@Entity(tableName = "parser_rules")
data class ParserRuleEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "bank_name") val bankName: String,
    @ColumnInfo(name = "regex_pattern") val regexPattern: String,
    @ColumnInfo(name = "amount_group") val amountGroup: Int = 1,
    @ColumnInfo(name = "merchant_group") val merchantGroup: Int = 2,
    @ColumnInfo(name = "date_group") val dateGroup: Int? = null,
    @ColumnInfo(name = "transaction_type") val transactionType: String? = null,
    @ColumnInfo(name = "is_user_created") val isUserCreated: Boolean = false,
    @ColumnInfo(name = "is_verified") val isVerified: Boolean = false
)

@Entity(tableName = "merchant_aliases")
data class MerchantAliasEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "raw_pattern") val rawPattern: String,
    @ColumnInfo(name = "normalized_name") val normalizedName: String,
    @ColumnInfo(name = "suggested_category_id") val suggestedCategoryId: String? = null,
    @ColumnInfo(name = "icon") val icon: String? = null
)

@Entity(tableName = "sender_whitelist")
data class SenderWhitelistEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "sender_id") val senderId: String,
    @ColumnInfo(name = "bank_name") val bankName: String,
    @ColumnInfo(name = "country") val country: String = "IN",
    @ColumnInfo(name = "is_active") val isActive: Boolean = true
)

@Entity(
    tableName = "saved_source_messages",
    foreignKeys = [ForeignKey(
        entity = TransactionEntity::class,
        parentColumns = ["id"],
        childColumns = ["transaction_id"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [
        Index(value = ["transaction_id"], unique = true),
        Index(value = ["message_timestamp"]),
        Index(value = ["source_type"])
    ]
)
data class SavedSourceMessageEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "transaction_id") val transactionId: String,
    @ColumnInfo(name = "source_type") val sourceType: String,
    @ColumnInfo(name = "sender") val sender: String?,
    @ColumnInfo(name = "package_name") val packageName: String?,
    @ColumnInfo(name = "original_message_id") val originalMessageId: String?,
    @ColumnInfo(name = "original_message_uri") val originalMessageUri: String?,
    @ColumnInfo(name = "encrypted_body") val encryptedBody: String?,
    @ColumnInfo(name = "message_timestamp") val messageTimestamp: Long,
    @ColumnInfo(name = "captured_at") val capturedAt: Long,
    @ColumnInfo(name = "confidence") val confidence: Float,
    @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis()
)
