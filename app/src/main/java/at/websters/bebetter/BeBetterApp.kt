package at.websters.bebetter

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import at.websters.bebetter.data.ApiClient
import at.websters.bebetter.data.SessionManager
import at.websters.bebetter.workers.ReminderWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

class BeBetterApp : Application() {
    lateinit var session: SessionManager

    override fun onCreate() {
        super.onCreate()
        session = SessionManager(this)
        ApiClient.init(session)
        CoroutineScope(Dispatchers.IO).launch {
            runCatching { session.ensureMigrated() }
            runCatching { session.getToken() } // prime in-memory cache
            runCatching { ApiClient.setBaseUrl(session.getBaseUrl()) }
        }
        createChannels()
        scheduleReminders()
    }

    private fun createChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val mgr = getSystemService(NotificationManager::class.java)
            mgr?.createNotificationChannel(
                NotificationChannel("habits", "Habit reminders", NotificationManager.IMPORTANCE_HIGH)
            )
            mgr?.createNotificationChannel(
                NotificationChannel("general", "General", NotificationManager.IMPORTANCE_DEFAULT)
            )
        }
    }

    private fun scheduleReminders() {
        val req = PeriodicWorkRequestBuilder<ReminderWorker>(60, TimeUnit.MINUTES).build()
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "bebetter-reminders", ExistingPeriodicWorkPolicy.KEEP, req
        )
    }
}
