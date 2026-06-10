package io.github.stupidgame.calyendar.data

import java.time.LocalDate

object FinancialCalculator {

    data class GoalProjection(
        val currentBalance: Long,
        val upcomingGoal: FinancialGoal? = null,
        val goalTargetAmount: Long? = null,
        val predictionDiff: Long? = null,
        val reachedGoalCost: Long = 0L
    )

    fun calculateGoalProjection(
        rawBalance: Long,
        allGoals: List<FinancialGoal>,
        currentDate: LocalDate,
        goalWindowStartDate: LocalDate = currentDate
    ): GoalProjection {
        val allSortedGoals = allGoals.sortedByDateThenId()
        val visibleGoals =
            allSortedGoals.filter { goal -> !goal.toLocalDate().isBefore(goalWindowStartDate) }
        val reachedGoalCost =
            allSortedGoals
                .filter { goal -> goal.toLocalDate().isBefore(currentDate) }
                .sumOf(FinancialGoal::amount)
        val currentBalance = (rawBalance - reachedGoalCost).coerceAtLeast(0L)
        val upcomingGoal =
            visibleGoals.firstOrNull { goal -> !goal.toLocalDate().isBefore(currentDate) }
        val goalTargetAmount =
            upcomingGoal?.let { goal ->
                allGoals.totalAmountOnDate(goal.toLocalDate())
            }
        val predictionDiff = goalTargetAmount?.let { currentBalance - it }

        return GoalProjection(
            currentBalance = currentBalance,
            upcomingGoal = upcomingGoal,
            goalTargetAmount = goalTargetAmount,
            predictionDiff = predictionDiff,
            reachedGoalCost = reachedGoalCost
        )
    }

    fun calculateDailyBalance(transactions: List<Transaction>): Long {
        return transactions.sumOf {
            when (it.type) {
                TransactionType.INCOME -> it.amount
                TransactionType.EXPENSE -> -it.amount
                else -> 0L
            }
        }
    }
}
