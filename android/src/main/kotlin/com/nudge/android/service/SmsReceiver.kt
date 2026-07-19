package com.nudge.android.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.nudge.engine.BundledRulePack

/**
 * Event-driven SMS receiver — parses incoming bank/transaction SMS on-device.
 * Never uploads raw message content. All processing is local.
 *
 * Uses a coarse sender filter to avoid waking the worker for non-bank SMS,
 * then delegates to SmsParsingWorker (ParsingWorkers.kt) which runs the full
 * DefaultSmsParserEngine pipeline (50+ bank regex templates, merchant normalization).
 */
class SmsReceiver : BroadcastReceiver() {

    private val senderWhitelist: Set<String> by lazy {
        BundledRulePack.getSenderWhitelist()
            .map { it.senderId.uppercase() }
            .toSet()
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return

        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
        for (message in messages) {
            val senderId = message.originatingAddress ?: continue
            val body = message.messageBody ?: continue

            if (!isWhitelisted(senderId)) continue

            val workData = Data.Builder()
                .putString("sender_id", senderId)
                .putString("message_body", body)
                .putLong("received_at", System.currentTimeMillis())
                .build()

            val work = OneTimeWorkRequestBuilder<SmsParsingWorker>()
                .setInputData(workData)
                .setConstraints(
                    Constraints.Builder()
                        .setRequiresBatteryNotLow(true)
                        .build()
                )
                .build()

            WorkManager.getInstance(context)
                .enqueueUniqueWork(
                    "sms_parse_${body.hashCode()}",
                    ExistingWorkPolicy.KEEP,
                    work
                )
        }
    }

    private fun isWhitelisted(senderId: String): Boolean {
        val upper = senderId.uppercase()
        return senderWhitelist.any { upper.contains(it) }
    }
}
