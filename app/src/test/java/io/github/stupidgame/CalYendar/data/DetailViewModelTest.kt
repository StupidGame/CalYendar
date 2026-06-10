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
    fun `detail goals include every goal on the selected date`() {
        val selectedGoal = goal(id = 1, date = LocalDate.of(2026, 5, 14), amount = 10_000)
        val sameDayGoal = goal(id = 2, date = LocalDate.of(2026, 5, 14), amount = 20_000)
        val anotherDayGoal = goal(id = 3, date = LocalDate.of(2026, 5, 20), amount = 30_000)

        val result =
            selectDetailGoals(
                allGoals = listOf(anotherDayGoal, sameDayGoal, selectedGoal),
                selectedDate = LocalDate.of(2026, 5, 14),
                fallbackGoal = anotherDayGoal
            )

        assertEquals(listOf(selectedGoal, sameDayGoal), result)
    }

    @Test
    fun `detail goals include every goal on the fallback date`() {
        val fallbackGoal = goal(id = 1, date = LocalDate.of(2026, 5, 20), amount = 10_000)
        val sameDayGoal = goal(id = 2, date = LocalDate.of(2026, 5, 20), amount = 20_000)
        val pastGoal = goal(id = 3, date = LocalDate.of(2026, 5, 10), amount = 30_000)

        val result =
            selectDetailGoals(
                allGoals = listOf(sameDayGoal, pastGoal, fallbackGoal),
                selectedDate = LocalDate.of(2026, 5, 14),
                fallbackGoal = fallbackGoal
            )

        assertEquals(listOf(fallbackGoal, sameDayGoal), result)
    }

    @Test
    fun `detail goal target amount is null when there is no goal`() {
        val result = calculateDetailGoalTargetAmount(emptyList())

        assertNull(result)
    }


    @Test
    fun `detail goal target amount sums every detail goal`() {
        val firstGoal = goal(id = 1, date = LocalDate.of(2026, 5, 20), amount = 10_000)
        val secondGoal = goal(id = 2, date = LocalDate.of(2026, 5, 20), amount = 5_000)

        val result = calculateDetailGoalTargetAmount(listOf(firstGoal, secondGoal))

        assertEquals(15_000, result)
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
