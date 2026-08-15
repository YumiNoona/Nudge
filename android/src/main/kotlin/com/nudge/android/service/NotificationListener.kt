package com.nudge.android.service

import android.content.Context
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkManager
import com.nudge.engine.FinancialMessageCandidate

/**
 * Notification Listener Service — captures bank/UPI app notifications
 * (GPay, PhonePe, Paytm, etc.) for automated transaction tracking.
 * Event-driven, never polls. Raw notification data never leaves the device.
 *
 * Delegates actual parsing to NotificationParsingWorker (defined in ParsingWorkers.kt).
 */
class NudgeNotificationListener : NotificationListenerService() {

    companion object {
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
        sbn?.let(::queueNotification)
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        // Covers notifications that arrived while Android was reconnecting the listener.
        activeNotifications.orEmpty().forEach(::queueNotification)
    }

    private fun queueNotification(sbn: StatusBarNotification) {
        val packageName = sbn.packageName
        if (packageName == applicationContext.packageName || packageName == "com.android.systemui") return

        val extras = sbn.notification.extras
        val fragments = buildList {
            extras.getCharSequence(android.app.Notification.EXTRA_TITLE)?.toString()?.let(::add)
            extras.getCharSequence(android.app.Notification.EXTRA_SUB_TEXT)?.toString()?.let(::add)
            extras.getCharSequence(android.app.Notification.EXTRA_BIG_TEXT)?.toString()?.let(::add)
            extras.getCharSequence(android.app.Notification.EXTRA_TEXT)?.toString()?.let(::add)
            extras.getCharSequenceArray(android.app.Notification.EXTRA_TEXT_LINES)
                ?.map(CharSequence::toString)?.let(::addAll)
        }.map(String::trim).filter(String::isNotBlank).distinct()
        val fullText = fragments.joinToString(" · ")

        if (!FinancialMessageCandidate.looksLikeCompletedMovement(fullText)) return

        val workData = Data.Builder()
            .putString("package_name", packageName)
            .putString("notification_text", fullText)
            .putLong("received_at", sbn.postTime)
            .build()

        val work = OneTimeWorkRequestBuilder<NotificationParsingWorker>()
            .setInputData(workData)
            .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
            .build()

        WorkManager.getInstance(this)
            .enqueueUniqueWork(
                "notif_parse_${sbn.key.hashCode()}_${fullText.hashCode()}_${sbn.postTime / 60_000L}",
                ExistingWorkPolicy.KEEP,
                work
            )
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        // Not needed for transaction capture
    }
}
