package com.nudge.android.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

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
        SenderWhitelistEntity::class,
        SavedSourceMessageEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class NudgeDatabase : RoomDatabase() {

    abstract fun transactionDao(): TransactionDao
    abstract fun categoryDao(): CategoryDao
    abstract fun accountDao(): AccountDao
    abstract fun budgetDao(): BudgetDao
    abstract fun recurringRuleDao(): RecurringRuleDao
    abstract fun gamificationDao(): GamificationDao
    abstract fun captureRuleDao(): CaptureRuleDao
    abstract fun savedSourceMessageDao(): SavedSourceMessageDao

    companion object {
        private var INSTANCE: NudgeDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE accounts ADD COLUMN is_default INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE accounts ADD COLUMN is_archived INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE accounts ADD COLUMN balance_cents INTEGER")
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """CREATE TABLE IF NOT EXISTS `saved_source_messages` (
                        `id` TEXT NOT NULL,
                        `transaction_id` TEXT NOT NULL,
                        `source_type` TEXT NOT NULL,
                        `sender` TEXT,
                        `package_name` TEXT,
                        `original_message_id` TEXT,
                        `original_message_uri` TEXT,
                        `encrypted_body` TEXT,
                        `message_timestamp` INTEGER NOT NULL,
                        `captured_at` INTEGER NOT NULL,
                        `confidence` REAL NOT NULL,
                        `created_at` INTEGER NOT NULL,
                        PRIMARY KEY(`id`),
                        FOREIGN KEY(`transaction_id`) REFERENCES `transactions`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )""".trimIndent()
                )
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_saved_source_messages_transaction_id` ON `saved_source_messages` (`transaction_id`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_saved_source_messages_message_timestamp` ON `saved_source_messages` (`message_timestamp`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_saved_source_messages_source_type` ON `saved_source_messages` (`source_type`)")
            }
        }

        fun getInstance(context: Context): NudgeDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    NudgeDatabase::class.java,
                    "nudge.db"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
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
