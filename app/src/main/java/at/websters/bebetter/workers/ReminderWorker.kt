package at.websters.bebetter.workers

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import at.websters.bebetter.MainActivity
import at.websters.bebetter.data.ApiClient
import java.time.LocalDate

/**
 * Lightweight replacement for web-push on Android: poll the backend for
 * today's scheduled habits/tasks + unread notifications and surface local
 * notifications. Runs hourly via WorkManager.
 */
class ReminderWorker(ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {
    override suspend fun doWork(): Result {
        return try {
            // Skip when logged out — 401 means no session, not a retry.
            val api = ApiClient.get()
            val today = LocalDate.now().toString()
            val scheduled = runCatching { api.scheduledHabits(today).habits }.getOrDefault(emptyList())
            val due = scheduled.filter { it.completedToday != true }
            if (due.isNotEmpty()) {
                notify(
                    id = 1001,
                    title = "BeBetter: ${due.size} habit${if (due.size == 1) "" else "s"} due today",
                    text = due.take(3).joinToString(", ") { "${it.emoji} ${it.title}".trim() }
                )
            }
            val notifs = runCatching { api.notifications().notifications }.getOrDefault(emptyList())
            val unread = notifs.count { !it.read && !it.pushed }
            if (unread > 0) {
                notify(1002, "BeBetter: $unread unread notification${if (unread == 1) "" else "s"}", "Open the app to catch up with friends & challenges.")
            }
            Result.success()
        } catch (e: Exception) {
            // 401/no-auth → success (don't retry); network → retry.
            if ((e as? retrofit2.HttpException)?.code() == 401) Result.success() else Result.retry()
        }
    }

    private fun notify(id: Int, title: String, text: String) {
        val intent = Intent(applicationContext, MainActivity::class.java)
        val pi = PendingIntent.getActivity(applicationContext, id, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val n = NotificationCompat.Builder(applicationContext, "habits")
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setContentIntent(pi)
            .setAutoCancel(true)
            .build()
        runCatching {
            if (NotificationManagerCompat.from(applicationContext).areNotificationsEnabled()) {
                NotificationManagerCompat.from(applicationContext).notify(id, n)
            }
        }
    }
}
