package io.github.stupidgame.calyendar.data

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class GoalComparisonSnapshotCalculatorTest {

    @Test
    fun `keeps widget balance raw and available balance raw when there is no upcoming goal`() {
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

        assertEquals(100_000L, snapshot.currentBalance)
        assertEquals(100_000L, snapshot.availableBalance)
        assertNull(snapshot.nextGoal)
    }

    @Test
    fun `keeps widget balance raw and subtracts next goal for available balance when upcoming goal exists`() {
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

        assertEquals(180_000L, snapshot.currentBalance)
        assertEquals(80_000L, snapshot.availableBalance)
        assertEquals("trip", snapshot.nextGoal?.name)
        assertEquals(100_000L, snapshot.nextGoal?.amount)
    }
}
