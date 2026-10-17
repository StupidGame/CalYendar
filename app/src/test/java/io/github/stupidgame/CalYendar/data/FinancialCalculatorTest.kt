package io.github.stupidgame.calyendar.data

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class FinancialCalculatorTest {

    @Test
    fun `goal projection keeps a goal on the current date as upcoming`() {
        val result =
            FinancialCalculator.calculateGoalProjection(
                transactions = listOf(transaction(TransactionType.INCOME, 200_000L, LocalDate.of(2026, 1, 1))),
                allGoals =
                    listOf(
                        goal(LocalDate.of(2026, 3, 10), amount = 30_000L),
                        goal(LocalDate.of(2026, 3, 18), amount = 40_000L),
                        goal(LocalDate.of(2026, 3, 25), amount = 50_000L)
                    ),
                currentDate = LocalDate.of(2026, 3, 18),
                goalWindowStartDate = LocalDate.of(2026, 3, 18)
            )

        assertEquals(170_000L, result.currentBalance)
        assertEquals(30_000L, result.reachedGoalCost)
        assertEquals("goal-18", result.upcomingGoal?.name)
        assertEquals(40_000L, result.goalTargetAmount)
        assertEquals(130_000L, result.predictionDiff)
    }

    @Test
    fun `goal projection subtracts all reached goals before comparing later goals`() {
        val result =
            FinancialCalculator.calculateGoalProjection(
                transactions = listOf(transaction(TransactionType.INCOME, 20_000L, LocalDate.of(2026, 1, 1))),
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
                transactions = listOf(transaction(TransactionType.INCOME, 100_000L, LocalDate.of(2026, 1, 1))),
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
                transactions = listOf(transaction(TransactionType.INCOME, 5_000L, LocalDate.of(2026, 1, 1))),
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
    fun `goal projection does not subtract a goal on its date`() {
        val result =
            FinancialCalculator.calculateGoalProjection(
                transactions = listOf(transaction(TransactionType.INCOME, 5_000L, LocalDate.of(2026, 1, 1))),
                allGoals =
                    listOf(
                        goal(LocalDate.of(2026, 3, 15), amount = 1_000L),
                        goal(LocalDate.of(2026, 4, 5), amount = 3_000L)
                    ),
                currentDate = LocalDate.of(2026, 3, 15),
                goalWindowStartDate = LocalDate.of(2026, 3, 1)
            )

        assertEquals(5_000L, result.currentBalance)
        assertEquals(0L, result.reachedGoalCost)
        assertEquals("goal-15", result.upcomingGoal?.name)
        assertEquals(1_000L, result.goalTargetAmount)
        assertEquals(4_000L, result.predictionDiff)
    }

    @Test
    fun `goal projection subtracts a goal after its date and moves to the next goal`() {
        val result =
            FinancialCalculator.calculateGoalProjection(
                transactions = listOf(transaction(TransactionType.INCOME, 5_000L, LocalDate.of(2026, 1, 1))),
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
    fun `goal projection compares a current date goal before subtracting it`() {
        val result =
            FinancialCalculator.calculateGoalProjection(
                transactions = listOf(transaction(TransactionType.INCOME, 20_000L, LocalDate.of(2026, 1, 1))),
                allGoals =
                    listOf(
                        goal(LocalDate.of(2026, 3, 18), amount = 40_000L),
                        goal(LocalDate.of(2026, 3, 25), amount = 50_000L)
                    ),
                currentDate = LocalDate.of(2026, 3, 18),
                goalWindowStartDate = LocalDate.of(2026, 3, 18)
            )

        assertEquals(20_000L, result.currentBalance)
        assertEquals(0L, result.reachedGoalCost)
        assertEquals("goal-18", result.upcomingGoal?.name)
        assertEquals(40_000L, result.goalTargetAmount)
        assertEquals(-20_000L, result.predictionDiff)
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

    @Test
    fun `balance discards each daily deficit before later income`() {
        val transactions =
            listOf(
                transaction(TransactionType.INCOME, 1_000, LocalDate.of(2026, 3, 18)),
                transaction(TransactionType.EXPENSE, 500, LocalDate.of(2026, 3, 19)),
                transaction(TransactionType.EXPENSE, 800, LocalDate.of(2026, 3, 20)),
                transaction(TransactionType.EXPENSE, 300, LocalDate.of(2026, 3, 21)),
                transaction(TransactionType.INCOME, 200, LocalDate.of(2026, 3, 22))
            )

        assertEquals(0L, FinancialCalculator.calculateBalance(transactions.take(3)))
        assertEquals(0L, FinancialCalculator.calculateBalance(transactions.take(4)))
        assertEquals(200L, FinancialCalculator.calculateBalance(transactions.reversed()))
    }

    @Test
    fun `balance settles all transactions on the same date together`() {
        val date = LocalDate.of(2026, 3, 18)
        val transactions =
            listOf(
                transaction(TransactionType.EXPENSE, 800, date),
                transaction(TransactionType.INCOME, 1_000, date),
                transaction(TransactionType.EXPENSE, 300, date),
                transaction(TransactionType.INCOME, 200, date),
                transaction(TransactionType.GOAL, 500, date)
            )

        assertEquals(100L, FinancialCalculator.calculateBalance(transactions))
        assertEquals(100L, FinancialCalculator.calculateBalance(transactions.reversed()))
    }

    @Test
    fun `projection discards goal shortages before the next days income`() {
        val goals =
            listOf(
                goal(LocalDate.of(2026, 3, 18), 800),
                goal(LocalDate.of(2026, 3, 18), 300),
                goal(LocalDate.of(2026, 3, 25), 500)
            )
        val transactions =
            listOf(
                transaction(TransactionType.INCOME, 1_000, LocalDate.of(2026, 3, 18)),
                transaction(TransactionType.INCOME, 200, LocalDate.of(2026, 3, 19)),
                transaction(TransactionType.INCOME, 9_000, LocalDate.of(2026, 3, 20))
            )

        val result =
            FinancialCalculator.calculateGoalProjection(
                transactions = transactions,
                allGoals = goals,
                currentDate = LocalDate.of(2026, 3, 19)
            )

        assertEquals(200L, result.currentBalance)
        assertEquals(1_100L, result.reachedGoalCost)
        assertEquals(-300L, result.predictionDiff)
    }

    @Test
    fun `balance discards shortages on days without transactions and across years`() {
        val transactions =
            listOf(
                transaction(TransactionType.INCOME, 500, LocalDate.of(2025, 12, 30)),
                transaction(TransactionType.INCOME, 200, LocalDate.of(2026, 1, 1))
            )

        val result =
            FinancialCalculator.calculateGoalProjection(
                transactions = transactions,
                allGoals = listOf(goal(LocalDate.of(2025, 12, 31), 800)),
                currentDate = LocalDate.of(2026, 1, 1)
            )

        assertEquals(200L, result.currentBalance)
    }

    private fun goal(date: LocalDate, amount: Long): FinancialGoal =
        FinancialGoal(
            year = date.year,
            month = date.monthValue - 1,
            day = date.dayOfMonth,
            name = "goal-${date.dayOfMonth}",
            amount = amount
        )

    private fun transaction(
        type: TransactionType,
        amount: Long,
        date: LocalDate = LocalDate.of(2026, 3, 18)
    ): Transaction =
        Transaction(
            year = date.year,
            month = date.monthValue - 1,
            day = date.dayOfMonth,
            type = type,
            name = "$type-$amount",
            amount = amount
        )
}
