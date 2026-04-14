package io.github.stupidgame.calyendar.data

private const val LegacyNoNotification = -1L

fun Event.notificationLeadTimes(): List<Long> {
    return when {
        notifications.isNotBlank() ->
            notifications.split(",").mapNotNull { token ->
                token.trim().toLongOrNull()
            }

        notificationMinutesBefore >= 0 -> listOf(notificationMinutesBefore)
        else -> emptyList()
    }.normalizedNotificationLeadTimes()
}

fun Iterable<Long>.normalizedNotificationLeadTimes(): List<Long> {
    val normalized = linkedSetOf<Long>()

    for (minutes in this) {
        if (minutes != LegacyNoNotification && minutes >= 0) {
            normalized += minutes
        }
    }

    return normalized.toList()
}

fun Iterable<Long>.toNotificationStorage(): String =
    normalizedNotificationLeadTimes().joinToString(",")
