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
        SavedSourceMessageEntity::class,
        FriendEntity::class,
        TransactionSplitEntity::class,
        RecurringTransactionEntity::class,
        ReceiptEntity::class,
        ReceiptPageEntity::class,
        ReceiptLineItemEntity::class,
        ReceiptTransactionLinkEntity::class
    ],
    version = 5,
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
    abstract fun sharedExpenseDao(): SharedExpenseDao
    abstract fun recurringTransactionDao(): RecurringTransactionDao
    abstract fun receiptDao(): ReceiptDao

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

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""CREATE TABLE IF NOT EXISTS `friends` (`id` TEXT NOT NULL, `name` TEXT NOT NULL, `color_hex` TEXT, `is_archived` INTEGER NOT NULL, `created_at` INTEGER NOT NULL, PRIMARY KEY(`id`))""")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_friends_name` ON `friends` (`name`)")
                db.execSQL("""CREATE TABLE IF NOT EXISTS `transaction_splits` (`id` TEXT NOT NULL, `transaction_id` TEXT NOT NULL, `friend_id` TEXT, `participant_name` TEXT NOT NULL, `share_cents` INTEGER NOT NULL, `paid_cents` INTEGER NOT NULL, `settled_cents` INTEGER NOT NULL, `split_method` TEXT NOT NULL, PRIMARY KEY(`id`), FOREIGN KEY(`transaction_id`) REFERENCES `transactions`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE)""")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_transaction_splits_transaction_id` ON `transaction_splits` (`transaction_id`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_transaction_splits_friend_id` ON `transaction_splits` (`friend_id`)")
                db.execSQL("""CREATE TABLE IF NOT EXISTS `recurring_transactions` (`id` TEXT NOT NULL, `template_transaction_id` TEXT NOT NULL, `interval` TEXT NOT NULL, `next_run_epoch` INTEGER NOT NULL, `end_epoch` INTEGER, `active` INTEGER NOT NULL, `created_at` INTEGER NOT NULL, PRIMARY KEY(`id`), FOREIGN KEY(`template_transaction_id`) REFERENCES `transactions`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE)""")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_recurring_transactions_template_transaction_id` ON `recurring_transactions` (`template_transaction_id`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_recurring_transactions_next_run_epoch` ON `recurring_transactions` (`next_run_epoch`)")
            }
        }

        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""CREATE TABLE IF NOT EXISTS `receipts` (`id` TEXT NOT NULL, `merchant` TEXT NOT NULL, `printed_total_cents` INTEGER NOT NULL, `calculated_total_cents` INTEGER NOT NULL, `subtotal_cents` INTEGER, `discount_cents` INTEGER NOT NULL, `tax_cents` INTEGER NOT NULL, `fee_cents` INTEGER NOT NULL, `tip_cents` INTEGER NOT NULL, `rounding_cents` INTEGER NOT NULL, `purchase_timestamp` INTEGER NOT NULL, `raw_text` TEXT NOT NULL, `confidence` REAL NOT NULL, `save_mode` TEXT NOT NULL, `created_at` INTEGER NOT NULL, PRIMARY KEY(`id`))""")
                db.execSQL("""CREATE TABLE IF NOT EXISTS `receipt_pages` (`id` TEXT NOT NULL, `receipt_id` TEXT NOT NULL, `page_index` INTEGER NOT NULL, `local_uri` TEXT NOT NULL, `ocr_text` TEXT NOT NULL, `warning` TEXT, PRIMARY KEY(`id`), FOREIGN KEY(`receipt_id`) REFERENCES `receipts`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE)""")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_receipt_pages_receipt_id` ON `receipt_pages` (`receipt_id`)")
                db.execSQL("""CREATE TABLE IF NOT EXISTS `receipt_line_items` (`id` TEXT NOT NULL, `receipt_id` TEXT NOT NULL, `transaction_id` TEXT, `name` TEXT NOT NULL, `quantity` REAL NOT NULL, `unit_price_cents` INTEGER, `line_total_cents` INTEGER NOT NULL, `allocated_total_cents` INTEGER NOT NULL, `category_id` TEXT, `confidence` REAL NOT NULL, `sort_order` INTEGER NOT NULL, PRIMARY KEY(`id`), FOREIGN KEY(`receipt_id`) REFERENCES `receipts`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE)""")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_receipt_line_items_receipt_id` ON `receipt_line_items` (`receipt_id`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_receipt_line_items_transaction_id` ON `receipt_line_items` (`transaction_id`)")
                db.execSQL("""CREATE TABLE IF NOT EXISTS `receipt_transaction_links` (`receipt_id` TEXT NOT NULL, `transaction_id` TEXT NOT NULL, PRIMARY KEY(`receipt_id`, `transaction_id`), FOREIGN KEY(`receipt_id`) REFERENCES `receipts`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE, FOREIGN KEY(`transaction_id`) REFERENCES `transactions`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE)""")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_receipt_transaction_links_receipt_id` ON `receipt_transaction_links` (`receipt_id`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_receipt_transaction_links_transaction_id` ON `receipt_transaction_links` (`transaction_id`)")
            }
        }

        fun getInstance(context: Context): NudgeDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    NudgeDatabase::class.java,
                    "nudge.db"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
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
