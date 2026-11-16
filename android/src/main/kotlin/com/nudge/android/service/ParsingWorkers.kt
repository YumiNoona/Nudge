package com.nudge.android.service

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import android.provider.Telephony
import android.content.ContentUris

class SmsParsingWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork() = withContext(Dispatchers.IO) {
        val sender = inputData.getString("sender_id") ?: return@withContext Result.failure()
        val body = inputData.getString("message_body") ?: return@withContext Result.failure()
        val receivedAt = inputData.getLong("received_at", System.currentTimeMillis())
        val resolved = resolveSmsSource(applicationContext, sender, body, receivedAt)
        val messageId = inputData.getString("message_id") ?: resolved?.first
        val messageUri = inputData.getString("message_uri") ?: resolved?.second
        TransactionCaptureProcessor(applicationContext).process(
            rawText = body,
            sourceId = sender,
            source = "sms",
            receivedAt = receivedAt,
            sourceMetadata = TransactionCaptureProcessor.SourceMetadata(
                sender = sender,
                originalMessageId = messageId,
                originalMessageUri = messageUri
            )
        )
        Result.success()
    }
}

private fun resolveSmsSource(context: Context, sender: String, body: String, receivedAt: Long): Pair<String, String>? = runCatching {
    context.contentResolver.query(
        Telephony.Sms.CONTENT_URI,
        arrayOf(Telephony.Sms._ID, Telephony.Sms.DATE, Telephony.Sms.DATE_SENT),
        "${Telephony.Sms.ADDRESS} = ? AND ${Telephony.Sms.BODY} = ?",
        arrayOf(sender, body),
        "${Telephony.Sms.DATE} DESC"
    )?.use { cursor ->
        val candidates = mutableListOf<Pair<String, Long>>()
        while (cursor.moveToNext()) {
            val id = cursor.getLong(cursor.getColumnIndexOrThrow(Telephony.Sms._ID)).toString()
            candidates += id to cursor.getLong(cursor.getColumnIndexOrThrow(Telephony.Sms.DATE))
            candidates += id to cursor.getLong(cursor.getColumnIndexOrThrow(Telephony.Sms.DATE_SENT))
        }
        val id = com.nudge.android.data.CaptureIdentity.closestMessage(candidates, receivedAt) ?: return@use null
        id to ContentUris.withAppendedId(Telephony.Sms.CONTENT_URI, id.toLong()).toString()
    }
}.getOrNull()

class NotificationParsingWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork() = withContext(Dispatchers.IO) {
        val packageName = inputData.getString("package_name") ?: return@withContext Result.failure()
        val text = inputData.getString("notification_text") ?: return@withContext Result.failure()
        TransactionCaptureProcessor(applicationContext).process(
            rawText = text,
            sourceId = packageName,
            source = "notification",
            receivedAt = inputData.getLong("received_at", System.currentTimeMillis()),
            sourceMetadata = TransactionCaptureProcessor.SourceMetadata(packageName = packageName)
        )
        Result.success()
    }
}
