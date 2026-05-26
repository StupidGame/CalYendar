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
        val rawTodayBalance = FinancialCalculator.calculateDailyBalance(input.transactionsUpToToday)
        val todayProjection =
            FinancialCalculator.calculateGoalProjection(
                rawBalance = rawTodayBalance,
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
            monthGoals.filter { goal -> goal.toLocalDate().isAfter(input.today) }

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
            val projection =
                FinancialCalculator.calculateGoalProjection(
                    rawBalance = runningBalance,
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
        val paidGoalCutoffDate =
            firstActiveGoalDate?.minusDays(1)
                ?: monthGoals.maxOfOrNull { goal -> goal.toLocalDate() }
                ?: LocalDate.of(input.year, input.month + 1, daysInMonth)
        val goalComparisonBalance =
            (
                balanceUpToLastGoal -
                    input.allGoals
                        .filter { goal ->
                            val goalDate = goal.toLocalDate()
                            !goalDate.isAfter(paidGoalCutoffDate)
                        }
                        .sumOf(FinancialGoal::amount)
            ).coerceAtLeast(0L)
        val nextGoalAfterMonthGoals =
            monthGoals
                .maxOfOrNull { goal -> goal.toLocalDate() }
                ?.let { lastMonthGoalDate ->
                    input.allGoals.firstAfterDate(lastMonthGoalDate)
                }
        val balanceAfterMonthGoals =
            FinancialCalculator.calculateGoalProjection(
                rawBalance = totalMonthBalance,
                allGoals = input.allGoals,
                currentDate = LocalDate.of(input.year, input.month + 1, daysInMonth),
                goalWindowStartDate = input.today
            ).currentBalance
        val availableMoneyAfterMonthGoals =
            balanceAfterMonthGoals - (nextGoalAfterMonthGoals?.amount ?: 0L)
        val isCurrentMonth =
            input.year == input.today.year && input.month == input.today.monthValue - 1

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
