package io.github.stupidgame.calyendar.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged

class AppSettingsStore(context: Context) {
    private val preferences =
        context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    val settingsFlow: Flow<AppSettings> = callbackFlow {
        val listener =
            SharedPreferences.OnSharedPreferenceChangeListener { _, _ ->
                trySend(getSettings())
            }

        preferences.registerOnSharedPreferenceChangeListener(listener)
        trySend(getSettings())

        awaitClose {
            preferences.unregisterOnSharedPreferenceChangeListener(listener)
        }
    }.distinctUntilChanged()

    fun getSettings(): AppSettings {
        return AppSettings(
            webCalUrl = preferences.getString(KEY_WEBCAL_URL, "").orEmpty(),
            notificationOneDayBefore = preferences.getBoolean(KEY_NOTIFICATION_ONE_DAY, true),
            notificationOneHourBefore = preferences.getBoolean(KEY_NOTIFICATION_ONE_HOUR, false)
        )
    }

    fun updateSettings(settings: AppSettings) {
        preferences
            .edit()
            .putString(KEY_WEBCAL_URL, settings.webCalUrl)
            .putBoolean(KEY_NOTIFICATION_ONE_DAY, settings.notificationOneDayBefore)
            .putBoolean(KEY_NOTIFICATION_ONE_HOUR, settings.notificationOneHourBefore)
            .apply()
    }

    companion object {
        private const val PREFERENCES_NAME = "calyendar_settings"
        private const val KEY_WEBCAL_URL = "webcal_url"
        private const val KEY_NOTIFICATION_ONE_DAY = "notification_one_day_before"
        private const val KEY_NOTIFICATION_ONE_HOUR = "notification_one_hour_before"
    }
}
