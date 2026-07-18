package com.nudge.android.service

import android.content.Context
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import androidx.work.*
import com.nudge.android.NudgeApp
import com.nudge.android.data.*
import com.nudge.util.IdGenerator
import kotlinx.datetime.Clock
import java.util.concurrent.TimeUnit

/**
 * Notification Listener Service — captures bank/UPI app notifications
 * (GPay, PhonePe, Paytm, etc.) for automated transaction tracking.
 * Event-driven, never polls. Raw notification data never leaves the device.
 */
class NudgeNotificationListener : NotificationListenerService() {

    companion object {
        private val TARGET_PACKAGES = setOf(
            "com.google.android.apps.nbu.paisa.user", // GPay
            "com.phonepe.app",                         // PhonePe
            "net.one97.paytm",                         // Paytm
            "com.amazon.mShop.android.shopping",       // Amazon Pay
            "in.org.npci.upiapp",                      // BHIM
            "com.idfcfirstbank.optimus",               // IDFC First
            "com.kotak.neo",                           // Kotak
            "com.icici.bank.icicico",                  // iMobile
            "com.hdfc.retail.netbanking",              // HDFC Mobile
            "com.yesbank.nomad",                       // YES Bank
            "com.axis.mobile",                         // Axis Mobile
            "com.sbi.lotus"                            // YONO SBI
        )

        private var instance: NudgeNotificationListener? = null
        fun getInstance(): NudgeNotificationListener? = instance
        fun isListening(context: Context): Boolean {
            val flat = android.provider.Settings.Secure.getString(
                context.contentResolver,
                "enabled_notification_listeners"
            )
            return flat?.contains(context.packageName) == true
        }
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
    }

    override fun onDestroy() {
        super.onDestroy()
        instance = null
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        if (sbn == null) return

        val packageName = sbn.packageName
        if (packageName !in TARGET_PACKAGES) return

        val extras = sbn.notification.extras
        val title = extras.getString("android.title") ?: return
        val text = extras.getString("android.text") ?: return
        val fullText = "$title: $text"

        // Quick heuristic: does this look like a transaction notification?
        if (!looksLikeTransaction(fullText)) return

        val workData = Data.Builder()
            .putString("package_name", packageName)
            .putString("notification_text", fullText)
            .putLong("received_at", sbn.postTime)
            .build()

        val work = OneTimeWorkRequestBuilder<NotificationParsingWorker>()
            .setInputData(workData)
            .setConstraints(
                Constraints.Builder()
                    .setRequiresBatteryNotLow(true)
                    .build()
            )
            .build()

        WorkManager.getInstance(this)
            .enqueueUniqueWork(
                "notif_parse_${fullText.hashCode()}",
                ExistingWorkPolicy.KEEP,
                work
            )
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        // Not needed for transaction capture
    }

    private fun looksLikeTransaction(text: String): Boolean {
        val transactionKeywords = listOf(
            "debited", "credited", "payment", "spent", "received",
            "transferred", "paid", "balance", "purchase", "wallet",
            "₹", "Rs.", "INR", "amount"
        )
        return transactionKeywords.any { text.contains(it, ignoreCase = true) }
    }
}

class NotificationParsingWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val packageName = inputData.getString("package_name") ?: return Result.failure()
        val text = inputData.getString("notification_text") ?: return Result.failure()

        val app = applicationContext as NudgeApp
        val passphrase = app.encryptedPrefs.getString(NudgeApp.PREFS_KEY_DB_PASSPHRASE, null)
            ?: return Result.failure()

        val db = NudgeDatabase.getInstance(applicationContext, passphrase.toByteArray())

        // Use the same SMS parsing logic for notification text
        val parsed = parseNotification(text, packageName) ?: return Result.failure()

        val now = Clock.System.now()
        val txn = TransactionEntity(
            id = IdGenerator.generate(),
            amountCents = parsed.first,
            type = parsed.second,
            merchantRaw = parsed.third,
            merchantNormalized = null,
            accountId = "",
            source = "notification",
            sourceRawText = text,
            confidenceScore = parsed.fourth,
            isReviewed = false,
            timestampEpoch = now.toEpochMilliseconds()
        )

        db.transactionDao().insert(txn)
        return Result.success()
    }

    private fun parseNotification(text: String, packageName: String): Quadruple? {
        val upiDebit = Regex(
            """(?:Rs\.|INR|₹)\s*([\d,]+\.?\d*)\s*(?:debited|spent|paid|sent)""",
            RegexOption.IGNORE_CASE
        )
        upiDebit.find(text)?.let { match ->
            val amount = extractAmount(match.groupValues[1])
            val merchant = extractMerchant(text)
            return Quadruple(amount, "debit", merchant, 0.75f)
        }

        val upiCredit = Regex(
            """(?:Rs\.|INR|₹)\s*([\d,]+\.?\d*)\s*(?:credited|received|added)""",
            RegexOption.IGNORE_CASE
        )
        upiCredit.find(text)?.let { match ->
            val amount = extractAmount(match.groupValues[1])
            return Quadruple(amount, "credit", "Income", 0.65f)
        }

        return null
    }

    private fun extractAmount(raw: String): Long {
        val cleaned = raw.replace(",", "").trim()
        return (cleaned.toDoubleOrNull()?.times(100))?.toLong() ?: 0L
    }

    private fun extractMerchant(text: String): String {
        val patterns = listOf(
            Regex("""(?:at|to|via|from)\s+(\S+(?:\s+\S+){0,3})""", RegexOption.IGNORE_CASE),
            Regex("""(?:at|to|via|from)\s+(\S+(?:\s+\S+){0,3})""", RegexOption.IGNORE_CASE)
        )
        for (p in patterns) {
            p.find(text)?.let { return it.groupValues[1].trim() }
        }
        return "Unknown"
    }

    data class Quadruple(val first: Long, val second: String, val third: String, val fourth: Float)
}
