package io.github.stupidgame.calyendar

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import io.github.stupidgame.calyendar.data.CalYendarDatabase
import io.github.stupidgame.calyendar.data.CalYendarRepository

class CalYendarApplication : Application() {
    val database: CalYendarDatabase by lazy { CalYendarDatabase.getDatabase(this) }
    val repository: CalYendarRepository by lazy { CalYendarRepository(database.calyendarDao()) }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Event Reminders"
            val descriptionText = "Notifications for upcoming events"
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel("EVENT_REMINDERS", name, importance).apply {
                description = descriptionText
            }
            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
}
