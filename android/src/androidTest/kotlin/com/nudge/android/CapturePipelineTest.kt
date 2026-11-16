package com.nudge.android

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.nudge.android.data.*
import com.nudge.android.service.TransactionCaptureProcessor
import kotlinx.coroutines.*
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CapturePipelineTest {
    private lateinit var context: Context
    private lateinit var db: NudgeDatabase
    @Before fun setup() = runBlocking {
        context = InstrumentationRegistry.getInstrumentation().targetContext
        Assume.assumeTrue("Use -PcaptureScreenshots for isolated device tests", context.packageName.endsWith(".screenshots"))
        context.getSharedPreferences("nudge_prefs", Context.MODE_PRIVATE).edit().putBoolean("auto_capture_enabled", true).putBoolean("save_transaction_messages", false).commit()
        db = Room.inMemoryDatabaseBuilder(context, NudgeDatabase::class.java).build()
        DefaultsSeeder.seedIfEmpty(db)
        db.accountDao().getAllOnce().forEach { db.accountDao().delete(it) }
        db.accountDao().insertAll(listOf(
            AccountEntity("primary", "Everyday", "HDFC", "upi", "1234", null, isDefault = true),
            AccountEntity("other", "Other", "HDFC", "upi", "5678", null),
        ))
    }
    @After fun close() { if (::db.isInitialized) db.close() }
    private fun message(reference: String = "123456789012", account: String = "1234") = "Rs. 100 debited from HDFC a/c XX$account at SWIGGY. UTR $reference"
    private suspend fun capture(text: String = message(), source: String = "sms", time: Long = 1700000000000) =
        TransactionCaptureProcessor(context, db).process(text, if (source == "sms") "HDFCBK" else "com.google.android.apps.nbu.paisa.user", source, time)

    @Test fun repeatPaymentsWithDifferentReferencesSurvive() = runBlocking {
        assertTrue(capture() is TransactionCaptureProcessor.Outcome.Added)
        assertTrue(capture(message("123456789013"), time = 1700000001000) is TransactionCaptureProcessor.Outcome.Added)
        assertEquals(2, db.transactionDao().getAllOnce().size)
    }
    @Test fun separateAccountsSurvive() = runBlocking {
        capture()
        assertTrue(capture(message(account = "5678")) is TransactionCaptureProcessor.Outcome.Added)
        assertEquals(2, db.transactionDao().getAllOnce().size)
    }
    @Test fun matchingSmsAndNotificationKeepBothSources() = runBlocking {
        val added = capture() as TransactionCaptureProcessor.Outcome.Added
        assertTrue(capture(source = "notification", time = 1700000002000) is TransactionCaptureProcessor.Outcome.Duplicate)
        assertEquals(1, db.transactionDao().getAllOnce().size)
        val sources = db.savedSourceMessageDao().getAllByTransaction(added.transaction.id)
        assertEquals(2, sources.size)
        assertTrue(sources.all { it.encryptedBody == null && it.referenceHash != null })
    }
    @Test fun concurrentCaptureIsIdempotent() = runBlocking {
        coroutineScope { List(8) { async(Dispatchers.IO) { capture() } }.awaitAll() }
        assertEquals(1, db.transactionDao().getAllOnce().size)
    }
    @Test fun uncertainAccountRequiresReviewAndStoresReasons() = runBlocking {
        val added = capture("Rs. 100 paid to SWIGGY via UPI") as TransactionCaptureProcessor.Outcome.Added
        assertFalse(added.transaction.isReviewed)
        val assessment = CaptureAssessment.fromJson(db.savedSourceMessageDao().getByTransaction(added.transaction.id)?.assessmentJson)!!
        assertTrue("Confirm the account" in assessment.reasons)
        assertTrue(assessment.overall < .8f)
    }
    @Test fun sameSourceRepeatWithoutReferenceIsFlaggedNotDiscarded() = runBlocking {
        val text = "Rs. 100 debited from HDFC a/c XX1234 at SWIGGY"
        capture(text)
        val added = capture(text, time = 1700000001000) as TransactionCaptureProcessor.Outcome.Added
        val assessment = CaptureAssessment.fromJson(db.savedSourceMessageDao().getByTransaction(added.transaction.id)?.assessmentJson)!!
        assertNotNull(assessment.possibleDuplicateId)
        assertFalse(added.transaction.isReviewed)
    }
    @Test fun completedPartialRefundFindsUniqueOriginalExpense() = runBlocking {
        val original = (capture() as TransactionCaptureProcessor.Outcome.Added).transaction
        val refund = capture("Refund of Rs. 50 credited to HDFC a/c XX1234 from SWIGGY. UTR 123456789099", time = 1700000005000) as TransactionCaptureProcessor.Outcome.Added
        val assessment = CaptureAssessment.fromJson(db.savedSourceMessageDao().getByTransaction(refund.transaction.id)?.assessmentJson)!!
        assertEquals(original.id, assessment.relatedExpenseId)
    }
    @Test fun diagnosticsNeverIncludeMessageOrAccountDetails() = runBlocking {
        capture()
        val events = CaptureDiagnostics.read(context)
        assertTrue(events.isNotEmpty())
        assertFalse(events.toString().contains("SWIGGY"))
        assertFalse(events.toString().contains("123456789012"))
        assertFalse(events.toString().contains("XX1234"))
    }
    @Test fun versionSixMigrationPreservesExistingDataAndAllowsAdditionalSources() = runBlocking {
        val name = "capture-migration-test.db"
        context.deleteDatabase(name)
        var disk = Room.databaseBuilder(context, NudgeDatabase::class.java, name).build()
        val txn = TransactionEntity("old-transaction", 10000, "debit", merchantRaw = "Cafe", merchantNormalized = "Cafe", categoryId = null, accountId = "primary", source = "sms", timestampEpoch = 1)
        disk.transactionDao().insert(txn)
        disk.savedSourceMessageDao().upsert(SavedSourceMessageEntity("old-source", txn.id, "sms", "BANK", null, "42", "content://sms/42", null, 1, 1, .8f))
        disk.close()
        SQLiteDatabase.openDatabase(context.getDatabasePath(name).path, null, SQLiteDatabase.OPEN_READWRITE).use { sql ->
            sql.execSQL("ALTER TABLE saved_source_messages RENAME TO saved_source_messages_v7")
            sql.execSQL("CREATE TABLE saved_source_messages (id TEXT NOT NULL PRIMARY KEY, transaction_id TEXT NOT NULL, source_type TEXT NOT NULL, sender TEXT, package_name TEXT, original_message_id TEXT, original_message_uri TEXT, encrypted_body TEXT, message_timestamp INTEGER NOT NULL, captured_at INTEGER NOT NULL, confidence REAL NOT NULL, created_at INTEGER NOT NULL, FOREIGN KEY(transaction_id) REFERENCES transactions(id) ON DELETE CASCADE)")
            sql.execSQL("INSERT INTO saved_source_messages SELECT id,transaction_id,source_type,sender,package_name,original_message_id,original_message_uri,encrypted_body,message_timestamp,captured_at,confidence,created_at FROM saved_source_messages_v7")
            sql.execSQL("DROP TABLE saved_source_messages_v7")
            sql.execSQL("CREATE UNIQUE INDEX index_saved_source_messages_transaction_id ON saved_source_messages(transaction_id)")
            sql.execSQL("CREATE INDEX index_saved_source_messages_message_timestamp ON saved_source_messages(message_timestamp)")
            sql.execSQL("CREATE INDEX index_saved_source_messages_source_type ON saved_source_messages(source_type)")
            sql.version = 6
        }
        disk = Room.databaseBuilder(context, NudgeDatabase::class.java, name).addMigrations(NudgeDatabase.MIGRATION_6_7).build()
        try {
            assertEquals(10000L, disk.transactionDao().getById(txn.id)?.amountCents)
            val old = disk.savedSourceMessageDao().getByOriginalMessageUri("content://sms/42")!!
            assertNull(old.assessmentJson)
            disk.savedSourceMessageDao().upsert(old.copy(id = "notification-source", sourceType = "notification", originalMessageUri = null, fingerprint = "new-fingerprint"))
            assertEquals(2, disk.savedSourceMessageDao().getAllByTransaction(txn.id).size)
        } finally { disk.close(); context.deleteDatabase(name) }
    }
}
