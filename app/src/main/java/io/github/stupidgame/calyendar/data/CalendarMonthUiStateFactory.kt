package io.github.stupidgame.calyendar.data

import java.time.LocalDate
import java.time.YearMonth

data class CalendarMonthUiStateInput(
    val year: Int,
    val month: Int,
    val today: LocalDate,
    val transactionsUpToToday: List<Transaction>,
    val transactionsBeforeMonth: List<Transaction>,
    val monthTransactions: List<Transaction>,
    val monthEvents: List<Event>,
    val allGoals: List<FinancialGoal>,
    val importedEvents: List<ImportedEvent>
)

object CalendarMonthUiStateFactory {

    fun create(input: CalendarMonthUiStateInput): CalendarUiState {
        val todayBalance = FinancialCalculator.calculateDailyBalance(input.transactionsUpToToday)
        val todayPrediction =
            FinancialCalculator.calculatePrediction(
                currentBalance = todayBalance,
                allGoals = input.allGoals,
                currentDate = input.today
            )
        val todayAvailableBalance = todayBalance - (todayPrediction.upcomingGoal?.amount ?: 0L)
        val transactionsByDay = input.monthTransactions.groupBy(Transaction::day)
        val eventsByDay = input.monthEvents.groupBy(Event::day)
        val importedEventsByDate = input.importedEvents.groupByStartLocalDate()
        val monthGoals =
            input.allGoals.filter { goal -> goal.year == input.year && goal.month == input.month }
        val activeMonthGoals =
            monthGoals.filter { goal -> !goal.toLocalDate().isBefore(input.today) }

        var runningBalance =
            FinancialCalculator.calculateDailyBalance(input.transactionsBeforeMonth)
        val daysInMonth = YearMonth.of(input.year, input.month + 1).lengthOfMonth()
        val dayStates = mutableMapOf<Int, DayState>()

        for (day in 1..daysInMonth) {
            val currentDate = LocalDate.of(input.year, input.month + 1, day)
            val dailyTransactions = transactionsByDay[day].orEmpty()
            val dailyEvents = eventsByDay[day].orEmpty()
            val dailyImportedEvents = importedEventsByDate[currentDate].orEmpty()

            runningBalance += FinancialCalculator.calculateDailyBalance(dailyTransactions)
            val prediction =
                FinancialCalculator.calculatePrediction(
                    currentBalance = runningBalance,
                    allGoals = input.allGoals,
                    currentDate = currentDate
                )

            dayStates[day] =
                DayState(
                    dayOfMonth = day,
                    balance = runningBalance,
                    goal = prediction.upcomingGoal?.takeIf { shouldDisplayGoal(currentDate, it, input.today) },
                    events = dailyEvents,
                    transactions = dailyTransactions,
                    icalEvents = dailyImportedEvents,
                    predictionDiff =
                        prediction.upcomingGoal
                            ?.let { goal -> runningBalance - goal.amount }
                            ?.takeIf { !currentDate.isBefore(input.today) },
                    isHoliday =
                        dailyImportedEvents.any(ImportedEvent::isHoliday) ||
                            dailyEvents.any(Event::isHoliday)
                )
        }

        val totalMonthBalance =
            FinancialCalculator.calculateDailyBalance(input.transactionsBeforeMonth) +
                FinancialCalculator.calculateDailyBalance(input.monthTransactions)
        val lastGoalDay =
            activeMonthGoals.maxOfOrNull(FinancialGoal::day)
                ?: monthGoals.maxOfOrNull(FinancialGoal::day)
                ?: daysInMonth
        val balanceUpToLastGoal =
            FinancialCalculator.calculateDailyBalance(input.transactionsBeforeMonth) +
                FinancialCalculator.calculateDailyBalance(
                    input.monthTransactions.filter { transaction -> transaction.day <= lastGoalDay }
                )
        val firstActiveGoalDate = activeMonthGoals.minOfOrNull { goal -> goal.toLocalDate() }
        val priorGoalCutoffDate =
            firstActiveGoalDate ?: LocalDate.of(input.year, input.month + 1, 1)
        val goalComparisonBalance =
            balanceUpToLastGoal -
                input.allGoals
                    .filter { goal ->
                        goal.toLocalDate().isBefore(priorGoalCutoffDate)
                    }
                    .sumOf(FinancialGoal::amount)
        val nextGoalAfterMonthGoals =
            monthGoals
                .maxOfOrNull { goal -> goal.toLocalDate() }
                ?.let { lastMonthGoalDate ->
                    input.allGoals
                        .sortedWith(compareBy({ it.year }, { it.month }, { it.day }, { it.id }))
                        .firstOrNull { goal -> goal.toLocalDate().isAfter(lastMonthGoalDate) }
                }
        val availableMoneyAfterMonthGoals =
            totalMonthBalance - (nextGoalAfterMonthGoals?.amount ?: 0L)
        val isCurrentMonth =
            input.year == input.today.year && input.month == input.today.monthValue - 1

        return CalendarUiState(
            year = input.year,
            month = input.month,
            dayStates = dayStates,
            currentBalance = balanceUpToLastGoal,
            todayBalance = todayBalance,
            todayAvailableBalance = todayAvailableBalance,
            goalComparisonBalance = goalComparisonBalance,
            monthGoals = monthGoals,
            activeMonthGoals = activeMonthGoals,
            availableMoneyAfterMonthGoals = availableMoneyAfterMonthGoals,
            hasTransactions = input.monthTransactions.isNotEmpty(),
            isCurrentMonth = isCurrentMonth
        )
    }

    private fun shouldDisplayGoal(
        currentDate: LocalDate,
        goal: FinancialGoal,
        today: LocalDate
    ): Boolean {
        val goalDate = goal.toLocalDate()
        return !currentDate.isBefore(today) && !currentDate.isAfter(goalDate)
    }
}
