package com.nudge.android.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {
    @Query("SELECT * FROM categories WHERE is_archived = 0 ORDER BY sort_order ASC, name ASC")
    fun getAll(): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories WHERE is_archived = 0 ORDER BY sort_order ASC, name ASC")
    suspend fun getAllOnce(): List<CategoryEntity>

    @Query("SELECT COUNT(*) FROM categories")
    suspend fun count(): Int

    @Query("SELECT * FROM categories WHERE id = :id")
    suspend fun getById(id: String): CategoryEntity?

    @Query("SELECT * FROM categories WHERE type = :type AND is_archived = 0 ORDER BY sort_order ASC")
    fun getByType(type: String): Flow<List<CategoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(category: CategoryEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(categories: List<CategoryEntity>)

    @Update
    suspend fun update(category: CategoryEntity)

    @Delete
    suspend fun delete(category: CategoryEntity)

    @Query("UPDATE categories SET is_archived = 1 WHERE id = :id")
    suspend fun archive(id: String)

    @Query("DELETE FROM categories WHERE id = :id")
    suspend fun deleteById(id: String)
}

@Dao
interface AccountDao {
    @Query("SELECT * FROM accounts WHERE is_active = 1 ORDER BY is_default DESC, name ASC")
    fun getAll(): Flow<List<AccountEntity>>

    @Query("SELECT * FROM accounts WHERE is_active = 1 AND is_archived = 0 ORDER BY is_default DESC, name ASC")
    suspend fun getAllOnce(): List<AccountEntity>

    @Query("SELECT * FROM accounts WHERE is_archived = 0 ORDER BY is_default DESC, name ASC")
    fun getActive(): Flow<List<AccountEntity>>

    @Query("SELECT * FROM accounts WHERE is_archived = 1 ORDER BY name ASC")
    fun getArchived(): Flow<List<AccountEntity>>

    @Query("SELECT COUNT(*) FROM accounts")
    suspend fun count(): Int

    @Query("SELECT * FROM accounts WHERE id = :id")
    suspend fun getById(id: String): AccountEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(account: AccountEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(accounts: List<AccountEntity>)

    @Update
    suspend fun update(account: AccountEntity)

    @Delete
    suspend fun delete(account: AccountEntity)

    @Query("UPDATE accounts SET is_default = 0")
    suspend fun clearDefault()

    @Query("UPDATE accounts SET is_default = 1 WHERE id = :id")
    suspend fun setDefault(id: String)

    @Query("UPDATE accounts SET is_archived = 1 WHERE id = :id")
    suspend fun archive(id: String)

    @Query("UPDATE accounts SET is_archived = 0 WHERE id = :id")
    suspend fun restore(id: String)
}

@Dao
interface BudgetDao {
    @Query("SELECT * FROM budgets")
    fun getAll(): Flow<List<BudgetEntity>>

    @Query("SELECT * FROM budgets WHERE category_id = :categoryId")
    fun getByCategory(categoryId: String): Flow<BudgetEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(budget: BudgetEntity)

    @Update
    suspend fun update(budget: BudgetEntity)

    @Delete
    suspend fun delete(budget: BudgetEntity)
}

@Dao
interface RecurringRuleDao {
    @Query("SELECT * FROM recurring_rules WHERE status = 'active'")
    fun getActive(): Flow<List<RecurringRuleEntity>>

    @Query("SELECT * FROM recurring_rules")
    fun getAll(): Flow<List<RecurringRuleEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(rule: RecurringRuleEntity)

    @Update
    suspend fun update(rule: RecurringRuleEntity)

    @Delete
    suspend fun delete(rule: RecurringRuleEntity)
}

@Dao
interface GamificationDao {
    @Query("SELECT * FROM gamification_profile LIMIT 1")
    fun getProfile(): Flow<GamificationProfileEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(profile: GamificationProfileEntity)

    @Query("UPDATE gamification_profile SET xp_total = xp_total + :xp WHERE userId = :userId")
    suspend fun addXp(userId: String, xp: Long)

    @Query("UPDATE gamification_profile SET current_streak_days = :days, last_activity_date = :date WHERE userId = :userId")
    suspend fun updateStreak(userId: String, days: Int, date: Long)
}
