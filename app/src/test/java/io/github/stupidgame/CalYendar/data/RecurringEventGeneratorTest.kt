package io.github.stupidgame.calyendar.data

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RecurringEventGeneratorTest {

    private val zoneId = ZoneId.systemDefault()

    @Test
    fun `returns base event unchanged when repeat is none`() {
        val baseEvent =
            createEvent(
                date = LocalDate.of(2026, 3, 19),
                id = 42L,
                seriesId = "existing-series"
            )

        val result =
            RecurringEventGenerator.generate(
                baseEvent = baseEvent,
                repeatType = EventRepeatType.NONE,
                repeatUntil = LocalDate.of(2026, 3, 31),
                repeatDays = emptySet(),
                zoneId = zoneId
            )

        assertEquals(listOf(baseEvent), result)
    }

    @Test
    fun `generates selected weekdays with shared series id and preserved time`() {
        val baseEvent = createEvent(date = LocalDate.of(2026, 3, 2), id = 7L)

        val result =
            RecurringEventGenerator.generate(
                baseEvent = baseEvent,
                repeatType = EventRepeatType.WEEKDAY_SELECTION,
                repeatUntil = LocalDate.of(2026, 3, 8),
                repeatDays =
                    setOf(
                        DayOfWeek.MONDAY.value,
                        DayOfWeek.WEDNESDAY.value,
                        DayOfWeek.FRIDAY.value
                    ),
                zoneId = zoneId
            )

        assertEquals(
            listOf(
                LocalDate.of(2026, 3, 2),
                LocalDate.of(2026, 3, 4),
                LocalDate.of(2026, 3, 6)
            ),
            result.map(Event::toLocalDate)
        )
        assertEquals(7L, result.first().id)
        assertTrue(result.drop(1).all { it.id == 0L })
        assertEquals(1, result.mapNotNull(Event::seriesId).distinct().size)
        assertTrue(result.all { it.notifications == "60,1440" })
        assertTrue(
            result.all { generated ->
                generated.startTime == generated.toLocalDate().atTime(9, 30).atZone(zoneId).toInstant().toEpochMilli()
            }
        )
    }

    private fun createEvent(
        date: LocalDate,
        id: Long = 0L,
        seriesId: String? = null
    ): Event =
        Event(
            id = id,
            year = date.year,
            month = date.monthValue - 1,
            day = date.dayOfMonth,
            title = "Focus time",
            startTime = date.atTime(9, 30).atZone(zoneId).toInstant().toEpochMilli(),
            endTime = date.atTime(11, 0).atZone(zoneId).toInstant().toEpochMilli(),
            notificationMinutesBefore = -1L,
            isHoliday = false,
            seriesId = seriesId,
            notifications = "60,1440"
        )
}
