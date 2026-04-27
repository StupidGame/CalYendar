package io.github.stupidgame.calyendar.data

import java.time.LocalDate
import kotlinx.coroutines.flow.first

data class GoalComparisonSnapshot(
    val currentBalance: Long,
    val availableBalance: Long,
    val nextGoal: FinancialGoal?
)

object GoalComparisonSnapshotCalculator {
    fun calculate(
        transactionsUpToCurrentDate: List<Transaction>,
        allGoals: List<FinancialGoal>,
        currentDate: LocalDate
    ): GoalComparisonSnapshot {
        val currentBalance = FinancialCalculator.calculateDailyBalance(transactionsUpToCurrentDate)
        val prediction =
            FinancialCalculator.calculatePrediction(
                currentBalance = currentBalance,
                allGoals = allGoals,
                currentDate = currentDate
            )

        val availableBalance = currentBalance - (prediction.upcomingGoal?.amount ?: 0L)

        return GoalComparisonSnapshot(
            currentBalance = currentBalance,
            availableBalance = availableBalance,
            nextGoal = prediction.upcomingGoal
        )
    }
}

suspend fun CalYendarRepository.loadGoalComparisonSnapshot(
    currentDate: LocalDate
): GoalComparisonSnapshot {
    val transactions =
        getTransactionsUpToToday(
            currentDate.year,
            currentDate.monthValue - 1,
            currentDate.dayOfMonth
        ).first()
    val allGoals = getAllGoals().first()

    return GoalComparisonSnapshotCalculator.calculate(
        transactionsUpToCurrentDate = transactions,
        allGoals = allGoals,
        currentDate = currentDate
    )
}
