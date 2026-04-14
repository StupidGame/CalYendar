package io.github.stupidgame.calyendar.data

import org.junit.Assert.assertEquals
import org.junit.Test

class EventNotificationsTest {

    @Test
    fun `prefers normalized multi notification field over legacy value`() {
        val event =
            Event(
                id = 1L,
                year = 2026,
                month = 3,
                day = 14,
                title = "Reminder test",
                startTime = 1_744_585_200_000L,
                endTime = 1_744_588_800_000L,
                notificationMinutesBefore = 30L,
                notifications = "60, 1440,60,-1"
            )

        assertEquals(listOf(60L, 1440L), event.notificationLeadTimes())
    }

    @Test
    fun `falls back to legacy notification when multi notification field is empty`() {
        val event =
            Event(
                id = 2L,
                year = 2026,
                month = 3,
                day = 15,
                title = "Legacy reminder",
                startTime = 1_744_671_600_000L,
                endTime = 1_744_675_200_000L,
                notificationMinutesBefore = 30L
            )

        assertEquals(listOf(30L), event.notificationLeadTimes())
    }

    @Test
    fun `stores unique positive reminders without sentinel values`() {
        val reminders = listOf(1440L, 60L, 1440L, -1L, 30L)

        assertEquals("1440,60,30", reminders.toNotificationStorage())
    }
}
