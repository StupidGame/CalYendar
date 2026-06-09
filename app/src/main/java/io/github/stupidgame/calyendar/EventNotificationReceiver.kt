package io.github.stupidgame.calyendar

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat

class EventNotificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val eventTitle =
            intent.getStringExtra(NotificationConstants.EXTRA_EVENT_TITLE)
                ?: context.getString(R.string.notification_default_title)
        val eventId = intent.getIntExtra(NotificationConstants.EXTRA_EVENT_ID, 0)

        val notification = NotificationCompat.Builder(context, NotificationConstants.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(eventTitle)
            .setContentText(context.getString(R.string.notification_content_upcoming))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(eventId, notification)
    }
}
