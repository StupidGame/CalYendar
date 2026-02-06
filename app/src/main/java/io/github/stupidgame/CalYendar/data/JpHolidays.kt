package io.github.stupidgame.CalYendar.data

import java.time.DayOfWeek
import java.time.LocalDate

object JpHolidays {
    fun isHoliday(year: Int, month: Int, day: Int): Boolean {
        val date = LocalDate.of(year, month + 1, day)
        return isFixedHoliday(date) || isHappyMonday(date) || isEquinox(date)
    }

    private fun isFixedHoliday(date: LocalDate): Boolean {
        return when (date.monthValue) {
            1 -> date.dayOfMonth == 1
            2 -> date.dayOfMonth == 11 || (date.year >= 2020 && date.dayOfMonth == 23)
            4 -> date.dayOfMonth == 29
            5 -> date.dayOfMonth == 3 || date.dayOfMonth == 4 || date.dayOfMonth == 5
            8 -> date.dayOfMonth == 11
            11 -> date.dayOfMonth == 3 || date.dayOfMonth == 23
            else -> false
        }
    }

    private fun isHappyMonday(date: LocalDate): Boolean {
        val week = (date.dayOfMonth - 1) / 7 + 1
        val dayOfWeek = date.dayOfWeek
        if (dayOfWeek != DayOfWeek.MONDAY) return false

        return when (date.monthValue) {
            1 -> week == 2 // Coming of Age Day
            7 -> week == 3 // Marine Day
            9 -> week == 3 // Respect for the Aged Day
            10 -> week == 2 // Sports Day
            else -> false
        }
    }

    private fun isEquinox(date: LocalDate): Boolean {
        // Simple approximation for Vernal and Autumnal Equinox
        if (date.monthValue == 3 && date.dayOfMonth == 20) return true // Rough approx
        if (date.monthValue == 9 && date.dayOfMonth == 23) return true // Rough approx
        return false
    }
}
