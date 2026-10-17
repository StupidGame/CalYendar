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
        val todayProjection =
            FinancialCalculator.calculateGoalProjection(
                transactions = input.transactionsUpToToday,
                allGoals = input.allGoals,
                currentDate = input.today,
                goalWindowStartDate = input.today
            )
        val todayBalance = todayProjection.currentBalance
        val todayAvailableBalance = todayProjection.predictionDiff ?: todayBalance
        val transactionsByDay = input.monthTransactions.groupBy(Transaction::day)
        val eventsByDay = input.monthEvents.groupBy(Event::day)
        val importedEventsByDate = input.importedEvents.groupByStartLocalDate()
        val monthGoals =
            input.allGoals.filter { goal -> goal.year == input.year && goal.month == input.month }
        val activeMonthGoals =
            monthGoals.filter { goal -> !goal.toLocalDate().isBefore(input.today) }

        val transactionsThroughMonth = input.transactionsBeforeMonth + input.monthTransactions
        val daysInMonth = YearMonth.of(input.year, input.month + 1).lengthOfMonth()
        val monthStartDate = LocalDate.of(input.year, input.month + 1, 1)
        val monthEndDate = LocalDate.of(input.year, input.month + 1, daysInMonth)
        val dayStates = mutableMapOf<Int, DayState>()

        for (day in 1..daysInMonth) {
            val currentDate = LocalDate.of(input.year, input.month + 1, day)
            val dailyTransactions = transactionsByDay[day].orEmpty()
            val dailyEvents = eventsByDay[day].orEmpty()
            val dailyImportedEvents = importedEventsByDate[currentDate].orEmpty()

            val projection =
                FinancialCalculator.calculateGoalProjection(
                    transactions = transactionsThroughMonth,
                    allGoals = input.allGoals,
                    currentDate = currentDate,
                    goalWindowStartDate = input.today
                )
            val displayedGoal =
                projection.upcomingGoal?.takeIf { shouldDisplayGoal(currentDate, it, input.today) }
            val predictionDiff =
                displayedGoal?.let { projection.predictionDiff }?.takeIf {
                    !currentDate.isBefore(input.today)
                }

            dayStates[day] =
                DayState(
                    dayOfMonth = day,
                    balance = projection.currentBalance,
                    goal = displayedGoal,
                    goalTargetAmount = displayedGoal?.let { projection.goalTargetAmount },
                    events = dailyEvents,
                    transactions = dailyTransactions,
                    icalEvents = dailyImportedEvents,
                    predictionDiff = predictionDiff,
                    isHoliday =
                        dailyImportedEvents.any(ImportedEvent::isHoliday) ||
                            dailyEvents.any(Event::isHoliday)
                )
        }

        val lastGoalDay =
            activeMonthGoals.maxOfOrNull(FinancialGoal::day)
                ?: monthGoals.maxOfOrNull(FinancialGoal::day)
                ?: daysInMonth
        val firstActiveGoalDate = activeMonthGoals.minOfOrNull { goal -> goal.toLocalDate() }
        val paidGoalCutoffDate =
            firstActiveGoalDate?.minusDays(1)
                ?: monthGoals.maxOfOrNull { goal -> goal.toLocalDate() }
                ?: LocalDate.of(input.year, input.month + 1, daysInMonth)
        val goalComparisonBalance =
            FinancialCalculator.calculateBalance(
                transactions =
                    input.transactionsBeforeMonth +
                        input.monthTransactions.filter { transaction -> transaction.day <= lastGoalDay },
                paidGoals =
                    input.allGoals.filter { goal ->
                        !goal.toLocalDate().isAfter(paidGoalCutoffDate)
                    }
            )
        val nextGoalAfterMonthGoals =
            monthGoals
                .maxOfOrNull { goal -> goal.toLocalDate() }
                ?.let { lastMonthGoalDate ->
                    input.allGoals.firstAfterDate(lastMonthGoalDate)
                }
        val balanceAfterMonthGoals =
            FinancialCalculator.calculateGoalProjection(
                transactions = transactionsThroughMonth,
                allGoals = input.allGoals,
                currentDate = monthEndDate.plusDays(1),
                goalWindowStartDate = input.today
            ).currentBalance
        val nextGoalAfterMonthGoalsTargetAmount =
            nextGoalAfterMonthGoals?.let { goal ->
                input.allGoals.totalAmountOnDate(goal.toLocalDate())
            } ?: 0L
        val availableMoneyAfterMonthGoals =
            balanceAfterMonthGoals - nextGoalAfterMonthGoalsTargetAmount
        val isCurrentMonth =
            input.year == input.today.year && input.month == input.today.monthValue - 1
        val isPastMonth =
            YearMonth.of(input.year, input.month + 1).isBefore(YearMonth.from(input.today))
        val spanningWindowStartDate =
            if (input.today.isAfter(monthStartDate)) input.today else monthStartDate
        val spanningGoal =
            if (activeMonthGoals.isEmpty() && !monthEndDate.isBefore(input.today)) {
                input.allGoals.sortedByDateThenId().firstOrNull { goal ->
                    val goalDate = goal.toLocalDate()
                    !goalDate.isBefore(spanningWindowStartDate) && goalDate.isAfter(monthEndDate)
                }
            } else {
                null
            }
        val spanningGoalTargetAmount =
            spanningGoal?.let { goal -> input.allGoals.totalAmountOnDate(goal.toLocalDate()) }
        val spanningGoalBalance =
            if (spanningGoal == null) {
                0L
            } else if (isCurrentMonth) {
                todayBalance
            } else {
                FinancialCalculator.calculateGoalProjection(
                    transactions = transactionsThroughMonth,
                    allGoals = input.allGoals,
                    currentDate = monthEndDate,
                    goalWindowStartDate = input.today
                ).currentBalance
            }

        return CalendarUiState(
            year = input.year,
            month = input.month,
            dayStates = dayStates,
            currentBalance = goalComparisonBalance,
            todayBalance = todayBalance,
            todayAvailableBalance = todayAvailableBalance,
            goalComparisonBalance = goalComparisonBalance,
            monthGoals = monthGoals,
            activeMonthGoals = activeMonthGoals,
            spanningGoal = spanningGoal,
            spanningGoalTargetAmount = spanningGoalTargetAmount,
            spanningGoalBalance = spanningGoalBalance,
            availableMoneyAfterMonthGoals = availableMoneyAfterMonthGoals,
            hasTransactions = input.monthTransactions.isNotEmpty(),
            isCurrentMonth = isCurrentMonth,
            isPastMonth = isPastMonth
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
