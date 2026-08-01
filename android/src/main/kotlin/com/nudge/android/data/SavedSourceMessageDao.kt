package com.nudge.android.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SavedSourceMessageDao {
    @Query("SELECT * FROM saved_source_messages ORDER BY message_timestamp DESC")
    fun observeAll(): Flow<List<SavedSourceMessageEntity>>

    @Query("SELECT * FROM saved_source_messages WHERE encrypted_body IS NOT NULL ORDER BY message_timestamp DESC")
    fun observeSaved(): Flow<List<SavedSourceMessageEntity>>

    @Query("SELECT * FROM saved_source_messages WHERE transaction_id = :transactionId LIMIT 1")
    suspend fun getByTransaction(transactionId: String): SavedSourceMessageEntity?

    @Query("SELECT * FROM saved_source_messages WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): SavedSourceMessageEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(source: SavedSourceMessageEntity)

    @Query("UPDATE saved_source_messages SET encrypted_body = NULL WHERE id = :id")
    suspend fun clearSavedBody(id: String)

    @Query("UPDATE saved_source_messages SET encrypted_body = NULL WHERE encrypted_body IS NOT NULL")
    suspend fun clearAllSavedBodies()

    @Query("UPDATE saved_source_messages SET encrypted_body = NULL WHERE encrypted_body IS NOT NULL AND message_timestamp < :cutoff")
    suspend fun clearSavedBodiesBefore(cutoff: Long)

    @Query("SELECT COUNT(*) FROM saved_source_messages WHERE encrypted_body IS NOT NULL")
    fun observeSavedCount(): Flow<Int>

    @Query("SELECT COALESCE(SUM(LENGTH(encrypted_body)), 0) FROM saved_source_messages WHERE encrypted_body IS NOT NULL")
    fun observeSavedBytes(): Flow<Long>
}
