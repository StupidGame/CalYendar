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
        transactions: List<Transaction>,
        allGoals: List<FinancialGoal>,
        currentDate: LocalDate,
        goalWindowStartDate: LocalDate = currentDate
    ): GoalProjection {
        val allSortedGoals = allGoals.sortedByDateThenId()
        val visibleGoals =
            allSortedGoals.filter { goal -> !goal.toLocalDate().isBefore(goalWindowStartDate) }
        val reachedGoals =
            allSortedGoals.filter { goal -> goal.toLocalDate().isBefore(currentDate) }
        val reachedGoalCost = reachedGoals.sumOf(FinancialGoal::amount)
        val currentBalance =
            calculateBalance(
                transactions.filter { !it.toLocalDate().isAfter(currentDate) },
                reachedGoals
            )
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

    fun calculateBalance(
        transactions: List<Transaction>,
        paidGoals: List<FinancialGoal> = emptyList()
    ): Long {
        val dailyChanges = sortedMapOf<LocalDate, Long>()
        transactions.groupBy(Transaction::toLocalDate).forEach { (date, dailyTransactions) ->
            dailyChanges[date] = calculateDailyBalance(dailyTransactions)
        }
        paidGoals.forEach { goal ->
            val date = goal.toLocalDate()
            dailyChanges[date] = (dailyChanges[date] ?: 0L) - goal.amount
        }

        // Settle each day's total before carrying its balance into the next day.
        return dailyChanges.values.fold(0L) { balance, change ->
            (balance + change).coerceAtLeast(0L)
        }
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
