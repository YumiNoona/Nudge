package com.nudge.android.service

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.nudge.android.R
import com.nudge.android.ui.MainActivity
import java.util.Calendar
import java.util.concurrent.TimeUnit
import kotlin.random.Random

class ExpenseReminderWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val prefs = applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        if (!prefs.getBoolean(PREF_ENABLED, false)) return Result.success()
        if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(
                applicationContext,
                Manifest.permission.POST_NOTIFICATIONS,
            ) != PackageManager.PERMISSION_GRANTED
        ) return Result.success()

        createChannel(applicationContext)
        val lastIndex = prefs.getInt(PREF_LAST_VARIANT, -1)
        val candidates = REMINDERS.indices.filter { it != lastIndex }
        val index = candidates.random()
        prefs.edit { putInt(PREF_LAST_VARIANT, index) }
        val (title, body) = REMINDERS[index]
        val intent = Intent(applicationContext, MainActivity::class.java)
            .putExtra(MainActivity.EXTRA_OPEN_TRANSACTIONS, true)
            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        val pendingIntent = PendingIntent.getActivity(
            applicationContext,
            4201,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_nudge)
            .setColor(0xFF173B31.toInt())
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
        NotificationManagerCompat.from(applicationContext).notify(REMINDER_NOTIFICATION_ID, notification)
        return Result.success()
    }

    companion object {
        const val PREFS_NAME = "nudge_prefs"
        const val PREF_ENABLED = "expense_reminders_enabled"
        private const val PREF_LAST_VARIANT = "expense_reminder_last_variant"
        private const val CHANNEL_ID = "expense_check_ins"
        private const val WORK_NAME = "nudge_expense_check_in"
        private const val REMINDER_NOTIFICATION_ID = 4201

        val REMINDERS = listOf(
            "A tiny money check-in" to "Take 30 seconds to review your latest expenses.",
            "Keep your month clear" to "A quick look now makes the rest of the month easier.",
            "Your spending, your call" to "Review anything new and keep your timeline accurate.",
            "A gentle expense nudge" to "Your latest transactions are ready for a quick check.",
            "Stay close to your money" to "Open your expense timeline and see what changed.",
            "Small check, clearer picture" to "Confirm recent expenses while they are still fresh.",
            "Two minutes for future you" to "Tidy up your recent spending and move on with your day.",
            "Know where today went" to "Take a quick glance at your latest spending.",
            "Your expense timeline is ready" to "Review new activity and keep everything organized.",
            "A calm money moment" to "Check recent expenses—no spreadsheets, no pressure.",
        )

        fun setEnabled(context: Context, enabled: Boolean) {
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit { putBoolean(PREF_ENABLED, enabled) }
            if (enabled) schedule(context) else WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
        }

        fun sync(context: Context) {
            val enabled = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getBoolean(PREF_ENABLED, false)
            if (enabled) schedule(context)
        }

        private fun schedule(context: Context) {
            createChannel(context)
            val work = PeriodicWorkRequestBuilder<ExpenseReminderWorker>(24, TimeUnit.HOURS)
                .setInitialDelay(nextEveningDelayMillis(), TimeUnit.MILLISECONDS)
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                work,
            )
        }

        private fun nextEveningDelayMillis(): Long {
            val now = Calendar.getInstance()
            val target = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 19)
                set(Calendar.MINUTE, Random.nextInt(0, 121))
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
                if (!after(now)) add(Calendar.DAY_OF_YEAR, 1)
            }
            return (target.timeInMillis - now.timeInMillis).coerceAtLeast(15 * 60_000L)
        }

        private fun createChannel(context: Context) {
            val manager = context.getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, "Expense check-ins", NotificationManager.IMPORTANCE_DEFAULT).apply {
                    description = "Occasional reminders to review recent expenses"
                },
            )
        }
    }
}
