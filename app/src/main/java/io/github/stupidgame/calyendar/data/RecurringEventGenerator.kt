package io.github.stupidgame.calyendar.data

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID

enum class EventRepeatType(val label: String) {
    NONE("なし"),
    DAILY("毎日"),
    WEEKLY("毎週"),
    WEEKDAY_SELECTION("曜日指定")
}

object RecurringEventGenerator {

    fun generate(
        baseEvent: Event,
        repeatType: EventRepeatType,
        repeatUntil: LocalDate?,
        repeatDays: Set<Int>,
        zoneId: ZoneId = ZoneId.systemDefault()
    ): List<Event> {
        if (repeatType == EventRepeatType.NONE || repeatUntil == null) {
            return listOf(baseEvent)
        }

        val startDate = baseEvent.toLocalDate()
        if (repeatUntil.isBefore(startDate)) {
            return listOf(baseEvent)
        }

        val startTime =
            Instant.ofEpochMilli(baseEvent.startTime).atZone(zoneId).toLocalTime()
        val endTime =
            Instant.ofEpochMilli(baseEvent.endTime).atZone(zoneId).toLocalTime()
        val initialDayOfWeek = startDate.dayOfWeek
        val seriesId = baseEvent.seriesId ?: UUID.randomUUID().toString()

        return generateSequence(startDate) { currentDate ->
                currentDate.plusDays(1).takeUnless { it.isAfter(repeatUntil) }
            }
            .filter { currentDate ->
                when (repeatType) {
                    EventRepeatType.NONE -> true
                    EventRepeatType.DAILY -> true
                    EventRepeatType.WEEKLY -> currentDate.dayOfWeek == initialDayOfWeek
                    EventRepeatType.WEEKDAY_SELECTION ->
                        currentDate.dayOfWeek.value in repeatDays
                }
            }
            .map { currentDate ->
                baseEvent.copy(
                    id = if (currentDate == startDate) baseEvent.id else 0,
                    year = currentDate.year,
                    month = currentDate.monthValue - 1,
                    day = currentDate.dayOfMonth,
                    startTime =
                        currentDate.atTime(startTime).atZone(zoneId).toInstant().toEpochMilli(),
                    endTime =
                        currentDate.atTime(endTime).atZone(zoneId).toInstant().toEpochMilli(),
                    seriesId = seriesId
                )
            }
            .toList()
    }
}
