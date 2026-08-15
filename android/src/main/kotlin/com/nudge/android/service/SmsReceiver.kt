package com.nudge.android.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkManager
import com.nudge.engine.FinancialMessageCandidate

/**
 * Event-driven SMS receiver — parses incoming bank/transaction SMS on-device.
 * Never uploads raw message content. All processing is local.
 *
 * The first pass is based on completed-money language rather than a brittle sender allowlist.
 * Multipart messages are joined before parsing and processing is expedited for real-time UX.
 */
class SmsReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return

        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
        messages.groupBy { it.originatingAddress.orEmpty() }.forEach { (senderId, parts) ->
            if (senderId.isBlank()) return@forEach
            val body = parts.joinToString(separator = "") { it.messageBody.orEmpty() }.trim()
            if (!FinancialMessageCandidate.looksLikeCompletedMovement(body)) return@forEach
            val receivedAt = parts.minOfOrNull { it.timestampMillis }?.takeIf { it > 0L }
                ?: System.currentTimeMillis()

            val workData = Data.Builder()
                .putString("sender_id", senderId)
                .putString("message_body", body)
                .putLong("received_at", receivedAt)
                .build()

            val work = OneTimeWorkRequestBuilder<SmsParsingWorker>()
                .setInputData(workData)
                .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                .build()

            WorkManager.getInstance(context)
                .enqueueUniqueWork(
                    "sms_parse_${senderId.hashCode()}_${body.hashCode()}_${receivedAt / 60_000L}",
                    ExistingWorkPolicy.KEEP,
                    work
                )
        }
    }
}
