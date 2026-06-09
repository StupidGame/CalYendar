package io.github.stupidgame.calyendar.data

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class FinancialCalculatorTest {

    @Test
    fun `goal projection subtracts a goal on the current date and moves to the next goal`() {
        val result =
            FinancialCalculator.calculateGoalProjection(
                rawBalance = 200_000L,
                allGoals =
                    listOf(
                        goal(LocalDate.of(2026, 3, 10), amount = 30_000L),
                        goal(LocalDate.of(2026, 3, 18), amount = 40_000L),
                        goal(LocalDate.of(2026, 3, 25), amount = 50_000L)
                    ),
                currentDate = LocalDate.of(2026, 3, 18),
                goalWindowStartDate = LocalDate.of(2026, 3, 18)
            )

        assertEquals(130_000L, result.currentBalance)
        assertEquals(70_000L, result.reachedGoalCost)
        assertEquals("goal-25", result.upcomingGoal?.name)
        assertEquals(50_000L, result.goalTargetAmount)
        assertEquals(80_000L, result.predictionDiff)
    }

    @Test
    fun `goal projection subtracts all reached goals before comparing later goals`() {
        val result =
            FinancialCalculator.calculateGoalProjection(
                rawBalance = 20_000L,
                allGoals =
                    listOf(
                        goal(LocalDate.of(2026, 2, 20), amount = 9_999L),
                        goal(LocalDate.of(2026, 3, 15), amount = 1_000L),
                        goal(LocalDate.of(2026, 4, 5), amount = 3_000L)
                    ),
                currentDate = LocalDate.of(2026, 3, 16),
                goalWindowStartDate = LocalDate.of(2026, 3, 1)
            )

        assertEquals(9_001L, result.currentBalance)
        assertEquals(10_999L, result.reachedGoalCost)
        assertEquals("goal-5", result.upcomingGoal?.name)
        assertEquals(3_000L, result.goalTargetAmount)
        assertEquals(6_001L, result.predictionDiff)
    }

    @Test
    fun `goal projection sums goals on the same upcoming date`() {
        val result =
            FinancialCalculator.calculateGoalProjection(
                rawBalance = 100_000L,
                allGoals =
                    listOf(
                        goal(LocalDate.of(2026, 3, 20), amount = 30_000L),
                        goal(LocalDate.of(2026, 3, 20), amount = 20_000L),
                        goal(LocalDate.of(2026, 4, 5), amount = 10_000L)
                    ),
                currentDate = LocalDate.of(2026, 3, 18),
                goalWindowStartDate = LocalDate.of(2026, 3, 18)
            )

        assertEquals("goal-20", result.upcomingGoal?.name)
        assertEquals(50_000L, result.goalTargetAmount)
        assertEquals(50_000L, result.predictionDiff)
    }

    @Test
    fun `goal projection does not subtract a goal before its date`() {
        val result =
            FinancialCalculator.calculateGoalProjection(
                rawBalance = 5_000L,
                allGoals =
                    listOf(
                        goal(LocalDate.of(2026, 3, 15), amount = 1_000L),
                        goal(LocalDate.of(2026, 4, 5), amount = 3_000L)
                    ),
                currentDate = LocalDate.of(2026, 3, 14),
                goalWindowStartDate = LocalDate.of(2026, 3, 1)
            )

        assertEquals(5_000L, result.currentBalance)
        assertEquals(0L, result.reachedGoalCost)
        assertEquals("goal-15", result.upcomingGoal?.name)
        assertEquals(1_000L, result.goalTargetAmount)
        assertEquals(4_000L, result.predictionDiff)
    }

    @Test
    fun `goal projection subtracts a goal on its date`() {
        val result =
            FinancialCalculator.calculateGoalProjection(
                rawBalance = 5_000L,
                allGoals =
                    listOf(
                        goal(LocalDate.of(2026, 3, 15), amount = 1_000L),
                        goal(LocalDate.of(2026, 4, 5), amount = 3_000L)
                    ),
                currentDate = LocalDate.of(2026, 3, 15),
                goalWindowStartDate = LocalDate.of(2026, 3, 1)
            )

        assertEquals(4_000L, result.currentBalance)
        assertEquals(1_000L, result.reachedGoalCost)
        assertEquals("goal-5", result.upcomingGoal?.name)
        assertEquals(3_000L, result.goalTargetAmount)
        assertEquals(1_000L, result.predictionDiff)
    }

    @Test
    fun `goal projection subtracts a goal after its date and moves to the next goal`() {
        val result =
            FinancialCalculator.calculateGoalProjection(
                rawBalance = 5_000L,
                allGoals =
                    listOf(
                        goal(LocalDate.of(2026, 3, 15), amount = 1_000L),
                        goal(LocalDate.of(2026, 4, 5), amount = 3_000L)
                    ),
                currentDate = LocalDate.of(2026, 3, 16),
                goalWindowStartDate = LocalDate.of(2026, 3, 1)
            )

        assertEquals(4_000L, result.currentBalance)
        assertEquals(1_000L, result.reachedGoalCost)
        assertEquals("goal-5", result.upcomingGoal?.name)
        assertEquals(3_000L, result.goalTargetAmount)
        assertEquals(1_000L, result.predictionDiff)
    }

    @Test
    fun `goal projection keeps current balance at zero or above`() {
        val result =
            FinancialCalculator.calculateGoalProjection(
                rawBalance = 20_000L,
                allGoals =
                    listOf(
                        goal(LocalDate.of(2026, 3, 18), amount = 40_000L),
                        goal(LocalDate.of(2026, 3, 25), amount = 50_000L)
                    ),
                currentDate = LocalDate.of(2026, 3, 18),
                goalWindowStartDate = LocalDate.of(2026, 3, 18)
            )

        assertEquals(0L, result.currentBalance)
        assertEquals(40_000L, result.reachedGoalCost)
        assertEquals("goal-25", result.upcomingGoal?.name)
        assertEquals(-50_000L, result.predictionDiff)
    }

    @Test
    fun `daily balance sums income and expenses`() {
        val result =
            FinancialCalculator.calculateDailyBalance(
                listOf(
                    transaction(TransactionType.INCOME, amount = 100_000L),
                    transaction(TransactionType.EXPENSE, amount = 30_000L),
                    transaction(TransactionType.GOAL, amount = 50_000L)
                )
            )

        assertEquals(70_000L, result)
    }

    private fun goal(date: LocalDate, amount: Long): FinancialGoal =
        FinancialGoal(
            year = date.year,
            month = date.monthValue - 1,
            day = date.dayOfMonth,
            name = "goal-${date.dayOfMonth}",
            amount = amount
        )

    private fun transaction(type: TransactionType, amount: Long): Transaction =
        Transaction(
            year = 2026,
            month = 2,
            day = 18,
            type = type,
            name = "$type-$amount",
            amount = amount
        )
}
