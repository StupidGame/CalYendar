package io.github.stupidgame.calyendar.data

data class AppSettings(
    val webCalUrl: String = "",
    val notificationOneDayBefore: Boolean = true,
    val notificationOneHourBefore: Boolean = false
) {
    val defaultNotificationMinutes: List<Long>
        get() = buildList {
            if (notificationOneDayBefore) add(1440L)
            if (notificationOneHourBefore) add(60L)
        }
}
