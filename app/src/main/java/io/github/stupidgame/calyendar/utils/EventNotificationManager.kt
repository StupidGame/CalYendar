package io.github.stupidgame.calyendar.utils

import android.app.AlarmManager
import android.app.Application
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import io.github.stupidgame.calyendar.EventNotificationReceiver
import io.github.stupidgame.calyendar.data.Event

class EventNotificationManager(private val context: Context) {

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    fun scheduleEventNotification(event: Event) {
        if (event.notificationMinutesBefore == -1L) return

        val intent = Intent(context, EventNotificationReceiver::class.java).apply {
            putExtra("event_title", event.title)
            putExtra("event_id", event.id.toInt())
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            event.id.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notificationTime = event.startTime - (event.notificationMinutesBefore * 60 * 1000)

        if (notificationTime > System.currentTimeMillis()) {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                notificationTime,
                pendingIntent
            )
        }
    }

    fun cancelEventNotification(event: Event) {
        val intent = Intent(context, EventNotificationReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            event.id.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
    }
}
