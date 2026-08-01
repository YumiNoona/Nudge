package com.nudge.android.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface CaptureRuleDao {
    @Query("SELECT * FROM merchant_aliases ORDER BY LENGTH(raw_pattern) DESC")
    suspend fun getAliases(): List<MerchantAliasEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAlias(alias: MerchantAliasEntity)

    @Query("DELETE FROM merchant_aliases WHERE id = :id")
    suspend fun deleteAlias(id: String)
}
