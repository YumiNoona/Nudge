package com.nudge.android.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {
    @Query("SELECT * FROM transactions ORDER BY timestamp_epoch DESC")
    fun getAll(): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions ORDER BY timestamp_epoch DESC")
    suspend fun getAllOnce(): List<TransactionEntity>

    @Query("SELECT * FROM transactions WHERE amount_cents = :amount AND type = :type AND timestamp_epoch BETWEEN :startEpoch AND :endEpoch ORDER BY timestamp_epoch DESC")
    suspend fun findPotentialDuplicates(amount: Long, type: String, startEpoch: Long, endEpoch: Long): List<TransactionEntity>

    @Query("SELECT * FROM transactions WHERE is_reviewed = 0 ORDER BY timestamp_epoch DESC")
    fun getNeedsReview(): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE id = :id")
    suspend fun getById(id: String): TransactionEntity?

    @Query("""
        SELECT * FROM transactions 
        WHERE category_id = :categoryId 
        AND timestamp_epoch >= :startEpoch 
        AND timestamp_epoch <= :endEpoch
        ORDER BY timestamp_epoch DESC
    """)
    fun getByCategoryAndDateRange(
        categoryId: String,
        startEpoch: Long,
        endEpoch: Long
    ): Flow<List<TransactionEntity>>

    @Query("""
        SELECT * FROM transactions 
        WHERE timestamp_epoch >= :startEpoch 
        AND timestamp_epoch <= :endEpoch
        ORDER BY timestamp_epoch DESC
    """)
    fun getByDateRange(startEpoch: Long, endEpoch: Long): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE is_recurring = 1")
    fun getRecurring(): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE recurring_group_id = :groupId")
    fun getByRecurringGroup(groupId: String): Flow<List<TransactionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(transaction: TransactionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(transactions: List<TransactionEntity>)

    @Update
    suspend fun update(transaction: TransactionEntity)

    @Delete
    suspend fun delete(transaction: TransactionEntity)

    @Query("DELETE FROM transactions WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("""
        SELECT category_id, SUM(amount_cents) as total
        FROM transactions 
        WHERE type = 'debit'
        AND timestamp_epoch >= :startEpoch
        AND timestamp_epoch <= :endEpoch
        GROUP BY category_id
    """)
    fun getCategorySpending(startEpoch: Long, endEpoch: Long): Flow<List<CategorySpending>>

    @Query("SELECT COUNT(*) FROM transactions WHERE is_reviewed = 0")
    fun getNeedsReviewCount(): Flow<Int>
}

data class CategorySpending(
    @ColumnInfo(name = "category_id") val categoryId: String,
    val total: Long
)
