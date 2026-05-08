package io.github.stupidgame.calyendar.data

import java.time.LocalDate
import kotlinx.coroutines.flow.first

data class GoalComparisonSnapshot(
    val currentBalance: Long,
    val availableBalance: Long,
    val nextGoal: FinancialGoal?,
    val nextGoalTargetAmount: Long? = null
)

object GoalComparisonSnapshotCalculator {
    fun calculate(
        transactionsUpToCurrentDate: List<Transaction>,
        allGoals: List<FinancialGoal>,
        currentDate: LocalDate
    ): GoalComparisonSnapshot {
        val rawCurrentBalance =
            FinancialCalculator.calculateDailyBalance(transactionsUpToCurrentDate)
        val projection =
            FinancialCalculator.calculateGoalProjection(
                rawBalance = rawCurrentBalance,
                allGoals = allGoals,
                currentDate = currentDate,
                goalWindowStartDate = currentDate
            )
        val currentBalance = projection.currentBalance

        val availableBalance =
            if (projection.upcomingGoal != null) {
                projection.predictionDiff ?: currentBalance
            } else {
                currentBalance
            }

        return GoalComparisonSnapshot(
            currentBalance = currentBalance,
            availableBalance = availableBalance,
            nextGoal = projection.upcomingGoal,
            nextGoalTargetAmount = projection.goalTargetAmount
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
