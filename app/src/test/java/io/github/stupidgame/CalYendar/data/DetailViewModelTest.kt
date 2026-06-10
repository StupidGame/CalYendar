package io.github.stupidgame.calyendar.data

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DetailViewModelTest {

    @Test
    fun `editable detail goals prefer all goals on the selected date`() {
        val firstSameDayGoal = goal(id = 1, date = LocalDate.of(2026, 5, 14), amount = 10_000)
        val secondSameDayGoal = goal(id = 3, date = LocalDate.of(2026, 5, 14), amount = 5_000)
        val fallbackGoal = goal(id = 2, date = LocalDate.of(2026, 5, 20), amount = 20_000)

        val result =
            selectEditableDetailGoals(
                allGoals = listOf(fallbackGoal, secondSameDayGoal, firstSameDayGoal),
                selectedDate = LocalDate.of(2026, 5, 14),
                fallbackGoal = fallbackGoal
            )

        assertEquals(listOf(firstSameDayGoal, secondSameDayGoal), result)
    }

    @Test
    fun `editable detail goals prefer past goals on the selected date`() {
        val pastGoal = goal(id = 1, date = LocalDate.of(2026, 5, 13), amount = 10_000)
        val fallbackGoal = goal(id = 2, date = LocalDate.of(2026, 5, 20), amount = 20_000)

        val result =
            selectEditableDetailGoals(
                allGoals = listOf(pastGoal, fallbackGoal),
                selectedDate = LocalDate.of(2026, 5, 13),
                fallbackGoal = fallbackGoal
            )

        assertEquals(listOf(pastGoal), result)
    }

    @Test
    fun `editable detail goals keep all fallback date goals when the selected date has no goal`() {
        val pastGoal = goal(id = 1, date = LocalDate.of(2026, 5, 13), amount = 10_000)
        val fallbackGoal = goal(id = 2, date = LocalDate.of(2026, 5, 20), amount = 20_000)
        val sameFallbackDateGoal = goal(id = 3, date = LocalDate.of(2026, 5, 20), amount = 5_000)

        val result =
            selectEditableDetailGoals(
                allGoals = listOf(pastGoal, sameFallbackDateGoal, fallbackGoal),
                selectedDate = LocalDate.of(2026, 5, 12),
                fallbackGoal = fallbackGoal
            )

        assertEquals(listOf(fallbackGoal, sameFallbackDateGoal), result)
    }

    @Test
    fun `detail goal target amount sums visible goals`() {
        val firstGoal = goal(id = 1, date = LocalDate.of(2026, 5, 14), amount = 10_000)
        val secondGoal = goal(id = 2, date = LocalDate.of(2026, 5, 14), amount = 20_000)

        val result = calculateDetailGoalTargetAmount(listOf(firstGoal, secondGoal))

        assertEquals(30_000L, result)
    }

    @Test
    fun `detail goal target amount is null when there is no goal`() {
        val result = calculateDetailGoalTargetAmount(emptyList())

        assertNull(result)
    }

    private fun goal(
        id: Int,
        date: LocalDate,
        amount: Long
    ): FinancialGoal =
        FinancialGoal(
            id = id,
            year = date.year,
            month = date.monthValue - 1,
            day = date.dayOfMonth,
            name = "goal-${date.dayOfMonth}-$id",
            amount = amount
        )
}
