package io.github.stupidgame.calyendar.data

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DetailViewModelTest {

    @Test
    fun `editable detail goal prefers a goal on the selected date`() {
        val sameDayGoal = goal(id = 1, date = LocalDate.of(2026, 5, 14), amount = 10_000)
        val fallbackGoal = goal(id = 2, date = LocalDate.of(2026, 5, 20), amount = 20_000)

        val result =
            selectEditableDetailGoal(
                allGoals = listOf(fallbackGoal, sameDayGoal),
                selectedDate = LocalDate.of(2026, 5, 14),
                fallbackGoal = fallbackGoal
            )

        assertEquals(sameDayGoal, result)
    }

    @Test
    fun `editable detail goal prefers a past goal on the selected date`() {
        val pastGoal = goal(id = 1, date = LocalDate.of(2026, 5, 13), amount = 10_000)
        val fallbackGoal = goal(id = 2, date = LocalDate.of(2026, 5, 20), amount = 20_000)

        val result =
            selectEditableDetailGoal(
                allGoals = listOf(pastGoal, fallbackGoal),
                selectedDate = LocalDate.of(2026, 5, 13),
                fallbackGoal = fallbackGoal
            )

        assertEquals(pastGoal, result)
    }

    @Test
    fun `editable detail goal keeps the fallback when the selected date has no goal`() {
        val pastGoal = goal(id = 1, date = LocalDate.of(2026, 5, 13), amount = 10_000)
        val fallbackGoal = goal(id = 2, date = LocalDate.of(2026, 5, 20), amount = 20_000)

        val result =
            selectEditableDetailGoal(
                allGoals = listOf(pastGoal, fallbackGoal),
                selectedDate = LocalDate.of(2026, 5, 12),
                fallbackGoal = fallbackGoal
            )

        assertEquals(fallbackGoal, result)
    }

    @Test
    fun `detail goal target amount sums goals on the same date`() {
        val selectedGoal = goal(id = 1, date = LocalDate.of(2026, 5, 14), amount = 10_000)
        val sameDayGoal = goal(id = 2, date = LocalDate.of(2026, 5, 14), amount = 20_000)
        val anotherDayGoal = goal(id = 3, date = LocalDate.of(2026, 5, 20), amount = 30_000)

        val result =
            calculateDetailGoalTargetAmount(
                allGoals = listOf(selectedGoal, sameDayGoal, anotherDayGoal),
                goal = selectedGoal
            )

        assertEquals(30_000L, result)
    }

    @Test
    fun `detail goal target amount is null when there is no goal`() {
        val result = calculateDetailGoalTargetAmount(allGoals = emptyList(), goal = null)

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
            name = "goal-${date.dayOfMonth}",
            amount = amount
        )
}
