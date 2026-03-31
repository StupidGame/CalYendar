package io.github.stupidgame.calyendar.data

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class FinancialCalculatorTest {

    @Test
    fun `subtracts goals on or before current date from current balance`() {
        val currentBalance = 200_000L
        val currentDate = LocalDate.of(2026, 3, 18)
        val goals =
            listOf(
                goal(LocalDate.of(2026, 3, 10), amount = 30_000L),
                goal(LocalDate.of(2026, 3, 18), amount = 40_000L),
                goal(LocalDate.of(2026, 3, 25), amount = 50_000L)
            )

        val result =
            FinancialCalculator.calculateBalanceAfterCompletedGoals(
                currentBalance = currentBalance,
                allGoals = goals,
                currentDate = currentDate
            )

        assertEquals(130_000L, result)
    }

    @Test
    fun `keeps current balance when all goals are after current date`() {
        val currentBalance = 80_000L
        val currentDate = LocalDate.of(2026, 3, 18)
        val goals =
            listOf(
                goal(LocalDate.of(2026, 3, 20), amount = 10_000L),
                goal(LocalDate.of(2026, 4, 1), amount = 20_000L)
            )

        val result =
            FinancialCalculator.calculateBalanceAfterCompletedGoals(
                currentBalance = currentBalance,
                allGoals = goals,
                currentDate = currentDate
            )

        assertEquals(80_000L, result)
    }

    private fun goal(date: LocalDate, amount: Long): FinancialGoal =
        FinancialGoal(
            year = date.year,
            month = date.monthValue - 1,
            day = date.dayOfMonth,
            name = "goal-${date.dayOfMonth}",
            amount = amount
        )
}
