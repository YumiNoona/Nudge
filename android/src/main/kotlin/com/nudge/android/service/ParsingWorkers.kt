package com.nudge.android.service

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SmsParsingWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork() = withContext(Dispatchers.IO) {
        val sender = inputData.getString("sender_id") ?: return@withContext Result.failure()
        val body = inputData.getString("message_body") ?: return@withContext Result.failure()
        TransactionCaptureProcessor(applicationContext).process(
            rawText = body,
            sourceId = sender,
            source = "sms",
            receivedAt = inputData.getLong("received_at", System.currentTimeMillis())
        )
        Result.success()
    }
}

class NotificationParsingWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork() = withContext(Dispatchers.IO) {
        val packageName = inputData.getString("package_name") ?: return@withContext Result.failure()
        val text = inputData.getString("notification_text") ?: return@withContext Result.failure()
        TransactionCaptureProcessor(applicationContext).process(
            rawText = text,
            sourceId = packageName,
            source = "notification",
            receivedAt = inputData.getLong("received_at", System.currentTimeMillis())
        )
        Result.success()
    }
}
