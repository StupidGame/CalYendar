package io.github.stupidgame.calyendar

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class EventNotificationRestoreReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED -> {
                val pendingResult = goAsync()
                val app = context.applicationContext as CalYendarApplication
                CoroutineScope(SupervisorJob() + Dispatchers.Default).launch {
                    try {
                        app.eventSyncService.rescheduleAllEventNotifications()
                    } finally {
                        pendingResult.finish()
                    }
                }
            }
        }
    }
}
