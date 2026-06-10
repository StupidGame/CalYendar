package io.github.stupidgame.calyendar.data

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class GoalComparisonSnapshotCalculatorTest {

    @Test
    fun `subtracts past goals when there is no upcoming goal`() {
        val transactions =
            listOf(
                Transaction(
                    year = 2026,
                    month = 2,
                    day = 18,
                    type = TransactionType.INCOME,
                    name = "salary",
                    amount = 100_000
                )
            )
        val goals =
            listOf(
                FinancialGoal(
                    id = 1,
                    year = 2026,
                    month = 1,
                    day = 10,
                    name = "past",
                    amount = 30_000
                )
            )

        val snapshot =
            GoalComparisonSnapshotCalculator.calculate(
                transactionsUpToCurrentDate = transactions,
                allGoals = goals,
                currentDate = LocalDate.of(2026, 3, 18)
            )

        assertEquals(70_000L, snapshot.currentBalance)
        assertEquals(70_000L, snapshot.availableBalance)
        assertNull(snapshot.nextGoal)
        assertNull(snapshot.nextGoalTargetAmount)
    }

    @Test
    fun `subtracts past goals and reserves the next goal for widget available balance`() {
        val transactions =
            listOf(
                Transaction(
                    year = 2026,
                    month = 2,
                    day = 1,
                    type = TransactionType.INCOME,
                    name = "salary",
                    amount = 200_000
                ),
                Transaction(
                    year = 2026,
                    month = 2,
                    day = 5,
                    type = TransactionType.EXPENSE,
                    name = "food",
                    amount = 20_000
                )
            )
        val goals =
            listOf(
                FinancialGoal(
                    id = 1,
                    year = 2026,
                    month = 2,
                    day = 10,
                    name = "past",
                    amount = 50_000
                ),
                FinancialGoal(
                    id = 2,
                    year = 2026,
                    month = 2,
                    day = 25,
                    name = "trip",
                    amount = 100_000
                )
            )

        val snapshot =
            GoalComparisonSnapshotCalculator.calculate(
                transactionsUpToCurrentDate = transactions,
                allGoals = goals,
                currentDate = LocalDate.of(2026, 3, 18)
            )

        assertEquals(130_000L, snapshot.currentBalance)
        assertEquals(30_000L, snapshot.availableBalance)
        assertEquals("trip", snapshot.nextGoal?.name)
        assertEquals(100_000L, snapshot.nextGoal?.amount)
        assertEquals(100_000L, snapshot.nextGoalTargetAmount)
    }

    @Test
    fun `keeps a goal on the current date in the widget balance`() {
        val transactions =
            listOf(
                Transaction(
                    year = 2026,
                    month = 2,
                    day = 18,
                    type = TransactionType.INCOME,
                    name = "salary",
                    amount = 100_000
                )
            )
        val goals =
            listOf(
                FinancialGoal(
                    id = 1,
                    year = 2026,
                    month = 2,
                    day = 18,
                    name = "today",
                    amount = 70_000
                ),
                FinancialGoal(
                    id = 2,
                    year = 2026,
                    month = 2,
                    day = 25,
                    name = "future",
                    amount = 40_000
                )
            )

        val snapshot =
            GoalComparisonSnapshotCalculator.calculate(
                transactionsUpToCurrentDate = transactions,
                allGoals = goals,
                currentDate = LocalDate.of(2026, 3, 18)
            )

        assertEquals(100_000L, snapshot.currentBalance)
        assertEquals(30_000L, snapshot.availableBalance)
        assertEquals("today", snapshot.nextGoal?.name)
        assertEquals(70_000L, snapshot.nextGoalTargetAmount)
    }

    @Test
    fun `keeps widget balance at zero when paid goals exceed transactions`() {
        val transactions =
            listOf(
                Transaction(
                    year = 2026,
                    month = 2,
                    day = 1,
                    type = TransactionType.INCOME,
                    name = "salary",
                    amount = 50_000
                )
            )
        val goals =
            listOf(
                FinancialGoal(
                    id = 1,
                    year = 2026,
                    month = 2,
                    day = 10,
                    name = "paid",
                    amount = 80_000
                ),
                FinancialGoal(
                    id = 2,
                    year = 2026,
                    month = 2,
                    day = 25,
                    name = "future",
                    amount = 20_000
                )
            )

        val snapshot =
            GoalComparisonSnapshotCalculator.calculate(
                transactionsUpToCurrentDate = transactions,
                allGoals = goals,
                currentDate = LocalDate.of(2026, 3, 18)
            )

        assertEquals(0L, snapshot.currentBalance)
        assertEquals(-20_000L, snapshot.availableBalance)
        assertEquals("future", snapshot.nextGoal?.name)
        assertEquals(20_000L, snapshot.nextGoalTargetAmount)
    }

    @Test
    fun `sums goals on the same next goal date for widget available balance`() {
        val transactions =
            listOf(
                Transaction(
                    year = 2026,
                    month = 2,
                    day = 18,
                    type = TransactionType.INCOME,
                    name = "salary",
                    amount = 100_000
                )
            )
        val goals =
            listOf(
                FinancialGoal(
                    id = 1,
                    year = 2026,
                    month = 2,
                    day = 25,
                    name = "trip",
                    amount = 60_000
                ),
                FinancialGoal(
                    id = 2,
                    year = 2026,
                    month = 2,
                    day = 25,
                    name = "ticket",
                    amount = 30_000
                )
            )

        val snapshot =
            GoalComparisonSnapshotCalculator.calculate(
                transactionsUpToCurrentDate = transactions,
                allGoals = goals,
                currentDate = LocalDate.of(2026, 3, 18)
            )

        assertEquals(100_000L, snapshot.currentBalance)
        assertEquals(10_000L, snapshot.availableBalance)
        assertEquals("trip", snapshot.nextGoal?.name)
        assertEquals(90_000L, snapshot.nextGoalTargetAmount)
    }
}
