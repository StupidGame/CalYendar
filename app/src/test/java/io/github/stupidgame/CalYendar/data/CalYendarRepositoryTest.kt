package io.github.stupidgame.calyendar.data

import org.junit.Assert.assertEquals
import org.junit.Test

class CalYendarRepositoryTest {

    @Test
    fun `normalizes webcal scheme to https`() {
        assertEquals(
            "https://example.com/calendar.ics",
            normalizeCalendarUrl("webcal://example.com/calendar.ics")
        )
    }

    @Test
    fun `normalizes webcal scheme without touching later path text`() {
        assertEquals(
            "https://example.com/path/webcal-feed.ics",
            normalizeCalendarUrl(" webcal://example.com/path/webcal-feed.ics ")
        )
    }

    @Test
    fun `keeps existing https URL unchanged`() {
        assertEquals(
            "https://example.com/calendar.ics",
            normalizeCalendarUrl("https://example.com/calendar.ics")
        )
    }
}
