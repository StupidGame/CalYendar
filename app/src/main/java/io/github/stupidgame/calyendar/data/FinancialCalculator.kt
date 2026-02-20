package io.github.stupidgame.calyendar.data

import java.time.LocalDate

object FinancialCalculator {

    data class PredictionResult(
        val predictionDiff: Long? = null,
        val upcomingGoal: FinancialGoal? = null,
        val totalPriorGoalCost: Long = 0L
    )

    fun calculatePrediction(
        currentBalance: Long,
        allGoals: List<FinancialGoal>,
        currentDate: LocalDate
    ): PredictionResult {
        val sortedGoals = allGoals.sortedWith(compareBy({ it.year }, { it.month }, { it.day }))
        
        // 現在の日付以降で最初の目標を探す
        val upcomingGoal = sortedGoals.firstOrNull { goal ->
            val goalDate = LocalDate.of(goal.year, goal.month + 1, goal.day)
            !goalDate.isBefore(currentDate)
        } ?: return PredictionResult() // 次の目標がない場合

        val upcomingGoalIndex = sortedGoals.indexOf(upcomingGoal)
        
        // 次の目標より前のすべての目標の合計
        val priorGoals = if (upcomingGoalIndex > 0) {
            sortedGoals.subList(0, upcomingGoalIndex)
        } else {
            emptyList()
        }
        
        val totalPriorGoalCost = priorGoals.sumOf { it.amount }
        
        // 予想：現在残高 - (以前の目標の合計)
        // これは「次の目標に使えるお金」を表す
        val predictionDiff = currentBalance - totalPriorGoalCost

        return PredictionResult(
            predictionDiff = predictionDiff,
            upcomingGoal = upcomingGoal,
            totalPriorGoalCost = totalPriorGoalCost
        )
    }

    fun calculateDailyBalance(
        transactions: List<Transaction>
    ): Long {
        return transactions.sumOf {
            when (it.type) {
                TransactionType.INCOME -> it.amount
                TransactionType.EXPENSE -> -it.amount
                else -> 0L
            }
        }
    }
}
