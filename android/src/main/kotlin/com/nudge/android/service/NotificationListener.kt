package com.nudge.android.service

import android.content.Context
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager

/**
 * Notification Listener Service — captures bank/UPI app notifications
 * (GPay, PhonePe, Paytm, etc.) for automated transaction tracking.
 * Event-driven, never polls. Raw notification data never leaves the device.
 *
 * Delegates actual parsing to NotificationParsingWorker (defined in ParsingWorkers.kt).
 */
class NudgeNotificationListener : NotificationListenerService() {

    companion object {
        private val TARGET_PACKAGES = setOf(
            "com.google.android.apps.nbu.paisa.user",
            "com.phonepe.app",
            "net.one97.paytm",
            "com.amazon.mShop.android.shopping",
            "in.org.npci.upiapp",
            "com.idfcfirstbank.optimus",
            "com.kotak.neo",
            "com.icici.bank.icicico",
            "com.hdfc.retail.netbanking",
            "com.yesbank.nomad",
            "com.axis.mobile",
            "com.sbi.lotus"
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
