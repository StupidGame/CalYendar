package io.github.stupidgame.calyendar

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat

class EventNotificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val eventTitle =
            intent.getStringExtra("event_title") ?: context.getString(R.string.notification_default_title)
        val eventId = intent.getIntExtra("event_id", 0)

        val notification = NotificationCompat.Builder(context, "EVENT_REMINDERS")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(eventTitle)
            .setContentText(context.getString(R.string.notification_content_upcoming))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(eventId, notification)
    }
}
