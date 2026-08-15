package com.nudge.android.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "friends", indices = [Index(value = ["name"])])
data class FriendEntity(
    @PrimaryKey val id: String,
    val name: String,
    @ColumnInfo(name = "color_hex") val colorHex: String? = null,
    @ColumnInfo(name = "is_archived") val isArchived: Boolean = false,
    @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis(),
)

/** A participant's portion of one shared transaction. A null friendId represents the owner. */
@Entity(
    tableName = "transaction_splits",
    foreignKeys = [
        ForeignKey(
            entity = TransactionEntity::class,
            parentColumns = ["id"],
            childColumns = ["transaction_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("transaction_id"), Index("friend_id")],
)
data class TransactionSplitEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "transaction_id") val transactionId: String,
    @ColumnInfo(name = "friend_id") val friendId: String?,
    @ColumnInfo(name = "participant_name") val participantName: String,
    @ColumnInfo(name = "share_cents") val shareCents: Long,
    @ColumnInfo(name = "paid_cents") val paidCents: Long,
    @ColumnInfo(name = "settled_cents") val settledCents: Long = 0,
    @ColumnInfo(name = "split_method") val splitMethod: String = "equal",
)

@Entity(
    tableName = "recurring_transactions",
    foreignKeys = [
        ForeignKey(
            entity = TransactionEntity::class,
            parentColumns = ["id"],
            childColumns = ["template_transaction_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index(value = ["template_transaction_id"], unique = true), Index("next_run_epoch")],
)
data class RecurringTransactionEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "template_transaction_id") val templateTransactionId: String,
    val interval: String,
    @ColumnInfo(name = "next_run_epoch") val nextRunEpoch: Long,
    @ColumnInfo(name = "end_epoch") val endEpoch: Long? = null,
    val active: Boolean = true,
    @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis(),
)

data class SplitMemberDraft(
    val friendId: String?,
    val name: String,
    val shareCents: Long,
    val paidCents: Long = 0,
)

data class SplitDraft(
    val method: String,
    val members: List<SplitMemberDraft>,
)

data class RecurrenceDraft(
    val interval: String,
    val endEpoch: Long? = null,
)
