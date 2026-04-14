package io.github.stupidgame.calyendar

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import io.github.stupidgame.calyendar.data.AppSettingsStore
import io.github.stupidgame.calyendar.data.CalYendarDatabase
import io.github.stupidgame.calyendar.data.CalYendarRepository
import io.github.stupidgame.calyendar.data.EventSyncService
import io.github.stupidgame.calyendar.data.UserDataBackupService
import io.github.stupidgame.calyendar.utils.EventNotificationManager
import io.github.stupidgame.calyendar.widget.BalanceGoalWidgetSyncManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class CalYendarApplication : Application() {
    val database: CalYendarDatabase by lazy { CalYendarDatabase.getDatabase(this) }
    val repository: CalYendarRepository by lazy { CalYendarRepository(database) }
    val appSettingsStore: AppSettingsStore by lazy { AppSettingsStore(this) }
    val eventNotificationManager: EventNotificationManager by lazy { EventNotificationManager(this) }
    val eventSyncService: EventSyncService by lazy {
        EventSyncService(repository, eventNotificationManager)
    }
    val userDataBackupService: UserDataBackupService by lazy {
        UserDataBackupService(repository, appSettingsStore, eventSyncService)
    }
    private val balanceGoalWidgetSyncManager by lazy {
        BalanceGoalWidgetSyncManager(this, database)
    }
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        balanceGoalWidgetSyncManager.start()
        applicationScope.launch {
            eventSyncService.rescheduleAllEventNotifications()
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = getString(R.string.notification_channel_name)
            val descriptionText = getString(R.string.notification_channel_description)
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
