package com.nudge.android.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import net.sqlcipher.database.SupportFactory

@Database(
    entities = [
        TransactionEntity::class,
        AccountEntity::class,
        CategoryEntity::class,
        SubcategoryEntity::class,
        BudgetEntity::class,
        RecurringRuleEntity::class,
        GamificationProfileEntity::class,
        ParserRuleEntity::class,
        MerchantAliasEntity::class,
        SenderWhitelistEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class NudgeDatabase : RoomDatabase() {

    abstract fun transactionDao(): TransactionDao
    abstract fun categoryDao(): CategoryDao
    abstract fun accountDao(): AccountDao
    abstract fun budgetDao(): BudgetDao
    abstract fun recurringRuleDao(): RecurringRuleDao
    abstract fun gamificationDao(): GamificationDao

    companion object {
        private var INSTANCE: NudgeDatabase? = null

        fun getInstance(context: Context, passphrase: ByteArray): NudgeDatabase {
            return INSTANCE ?: synchronized(this) {
                val factory = SupportFactory(passphrase)
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    NudgeDatabase::class.java,
                    "nudge.db"
                )
                    .openHelperFactory(factory)
                    .build()
                INSTANCE = instance
                instance
            }
        }

        fun destroyInstance() {
            INSTANCE?.close()
            INSTANCE = null
        }
    }
}
