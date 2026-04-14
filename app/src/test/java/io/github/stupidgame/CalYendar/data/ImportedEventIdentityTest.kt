package io.github.stupidgame.calyendar.data

import biweekly.component.VEvent
import biweekly.property.Uid
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.Date
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ImportedEventIdentityTest {

    @Test
    fun `matches existing imported events by uid and occurrence`() {
        val existing =
            ImportedEvent(
                id = 10L,
                event = recurringEvent(uid = "team-sync", start = localDateTime(2026, 4, 14, 9, 0))
            )
        val incoming =
            ImportedEvent(
                event = recurringEvent(uid = "team-sync", start = localDateTime(2026, 4, 14, 9, 0))
            )

        assertEquals(listOf(existing), importedEventsToReplace(listOf(existing), listOf(incoming)))
    }

    @Test
    fun `different occurrences with the same uid stay distinct`() {
        val morning = ImportedEvent(event = recurringEvent("team-sync", localDateTime(2026, 4, 14, 9, 0)))
        val afternoon = ImportedEvent(event = recurringEvent("team-sync", localDateTime(2026, 4, 14, 13, 0)))

        assertNotEquals(morning.identityKey(), afternoon.identityKey())
    }

    @Test
    fun `does not replace holidays when importing user calendars`() {
        val holiday =
            ImportedEvent(
                id = 1L,
                event = simpleEvent("Holiday", localDateTime(2026, 4, 29, 0, 0)),
                isHoliday = true
            )
        val incoming =
            ImportedEvent(
                event = simpleEvent("Holiday", localDateTime(2026, 4, 29, 0, 0))
            )

        assertTrue(importedEventsToReplace(listOf(holiday), listOf(incoming)).isEmpty())
    }

    @Test
    fun `falls back to summary and dates when uid is missing`() {
        val existing =
            ImportedEvent(
                id = 2L,
                event = simpleEvent("Dentist", localDateTime(2026, 5, 1, 18, 30))
            )
        val incoming =
            ImportedEvent(
                event = simpleEvent("Dentist", localDateTime(2026, 5, 1, 18, 30))
            )

        assertEquals(existing.identityKey(), incoming.identityKey())
    }

    private fun recurringEvent(uid: String, start: LocalDateTime): VEvent {
        val instant = start.atZone(ZoneId.systemDefault()).toInstant()
        return VEvent().apply {
            setSummary("Recurring event")
            setDateStart(Date.from(instant))
            this.uid = Uid(uid)
        }
    }

    private fun simpleEvent(summary: String, start: LocalDateTime): VEvent {
        val instant = start.atZone(ZoneId.systemDefault()).toInstant()
        return VEvent().apply {
            setSummary(summary)
            setDateStart(Date.from(instant))
        }
    }

    private fun localDateTime(
        year: Int,
        month: Int,
        day: Int,
        hour: Int,
        minute: Int
    ): LocalDateTime = LocalDateTime.of(year, month, day, hour, minute)
}
