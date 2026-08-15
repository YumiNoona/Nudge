package com.nudge.android.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface SharedExpenseDao {
    @Query("SELECT * FROM friends WHERE is_archived = 0 ORDER BY name COLLATE NOCASE")
    fun observeFriends(): Flow<List<FriendEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertFriend(friend: FriendEntity)

    @Query("UPDATE friends SET is_archived = 1 WHERE id = :id")
    suspend fun archiveFriend(id: String)

    @Query("SELECT * FROM transaction_splits ORDER BY transaction_id")
    fun observeAllSplits(): Flow<List<TransactionSplitEntity>>

    @Query("SELECT * FROM transaction_splits WHERE transaction_id = :transactionId")
    suspend fun getSplits(transactionId: String): List<TransactionSplitEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSplits(splits: List<TransactionSplitEntity>)

    @Query("DELETE FROM transaction_splits WHERE transaction_id = :transactionId")
    suspend fun deleteSplits(transactionId: String)

    @Query("UPDATE transaction_splits SET settled_cents = :settledCents WHERE id = :id")
    suspend fun settle(id: String, settledCents: Long)
}

@Dao
interface RecurringTransactionDao {
    @Query("SELECT * FROM recurring_transactions WHERE active = 1 ORDER BY next_run_epoch")
    fun observeActive(): Flow<List<RecurringTransactionEntity>>

    @Query("SELECT * FROM recurring_transactions WHERE active = 1 AND next_run_epoch <= :now ORDER BY next_run_epoch")
    suspend fun getDue(now: Long): List<RecurringTransactionEntity>

    @Query("SELECT * FROM recurring_transactions WHERE template_transaction_id = :transactionId LIMIT 1")
    suspend fun getForTemplate(transactionId: String): RecurringTransactionEntity?

    @Query("SELECT * FROM recurring_transactions WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): RecurringTransactionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(rule: RecurringTransactionEntity)

    @Update
    suspend fun update(rule: RecurringTransactionEntity)

    @Query("DELETE FROM recurring_transactions WHERE template_transaction_id = :transactionId")
    suspend fun deleteForTemplate(transactionId: String)
}
