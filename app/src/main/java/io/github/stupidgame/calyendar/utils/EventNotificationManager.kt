package io.github.stupidgame.calyendar.utils

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import io.github.stupidgame.calyendar.EventNotificationReceiver
import io.github.stupidgame.calyendar.data.Event
import io.github.stupidgame.calyendar.data.notificationLeadTimes

class EventNotificationManager(private val context: Context) {

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    fun scheduleEventNotification(event: Event) {
        // Android 12 (API 31) 以降では権限チェックが必要
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
            return
        }
        val notificationList = event.notificationLeadTimes()

        if (notificationList.isEmpty()) return

        notificationList.forEachIndexed { index, minutes ->
            if (minutes < 0) return@forEachIndexed

            val intent = Intent(context, EventNotificationReceiver::class.java).apply {
                putExtra("event_title", event.title)
                putExtra("event_id", event.id.toInt())
            }
            // Use index to keep pending intents unique for multiple alarms on the same event
            val requestCode = (event.id.toInt() * 100) + index
            
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val notificationTime = event.startTime - (minutes * 60 * 1000)

            if (notificationTime > System.currentTimeMillis()) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    notificationTime,
                    pendingIntent
                )
            }
        }
    }

    fun cancelEventNotification(event: Event) {
        val notificationList = event.notificationLeadTimes()

        // Cancel standard one just in case the old code was used
        val standardIntent = Intent(context, EventNotificationReceiver::class.java)
        val standardPendingIntent = PendingIntent.getBroadcast(
            context,
            event.id.toInt(),
            standardIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(standardPendingIntent)

        notificationList.forEachIndexed { index, _ ->
            val intent = Intent(context, EventNotificationReceiver::class.java)
            val requestCode = (event.id.toInt() * 100) + index
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            alarmManager.cancel(pendingIntent)
        }
    }
}
