package io.github.stupidgame.calyendar.data

import android.content.ContentResolver
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import java.time.LocalDate
import java.util.Calendar
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

data class DayState(
        val dayOfMonth: Int,
        val balance: Long,
        val goal: FinancialGoal?,
        val events: List<Event>,
        val transactions: List<Transaction>,
        val icalEvents: List<ImportedEvent> = emptyList(),
        val predictionDiff: Long? = null,
        val isHoliday: Boolean
)

data class CalendarUiState(
        val year: Int,
        val month: Int,
        val dayStates: Map<Int, DayState> = emptyMap(),
        val currentBalance: Long = 0L,
        val monthlyGoalCurrentBalance: Long = 0L
)

class CalendarViewModel(private val repository: CalYendarRepository) : ViewModel() {

    private val _uiState =
            MutableStateFlow(
                    Calendar.getInstance().let {
                        CalendarUiState(it.get(Calendar.YEAR), it.get(Calendar.MONTH))
                    }
            )
    val uiState = _uiState.asStateFlow()

    private var loadMonthJob: Job? = null

    init {
        loadMonth(uiState.value.year, uiState.value.month)
    }

    fun loadMonth(year: Int, month: Int) {
        loadMonthJob?.cancel()
        loadMonthJob =
                viewModelScope.launch {
                    val today = LocalDate.now()

                    combine(
                                    repository.getTransactionsUpToToday(
                                            today.year,
                                            today.monthValue - 1,
                                            today.dayOfMonth
                                    ),
                                    repository.getTransactionsUpTo(year, month),
                                    repository.getTransactionsForMonth(year, month),
                                    repository.getEventsForMonth(year, month),
                                    repository.getAllGoals(),
                                    repository.getImportedEvents()
                            ) { values ->
                        val transactionsUpToToday = values[0] as List<Transaction>
                        val transactionsBefore = values[1] as List<Transaction>
                        val monthTransactions = values[2] as List<Transaction>
                        val monthEvents = values[3] as List<Event>
                        val allGoals = values[4] as List<FinancialGoal>
                        val importedEvents = values[5] as List<ImportedEvent>

                        // 今日の残高
                        val todayBalance =
                                FinancialCalculator.calculateDailyBalance(transactionsUpToToday)

                        // 月初の初期残高
                        var currentBalance =
                                FinancialCalculator.calculateDailyBalance(transactionsBefore)

                        val calendar = Calendar.getInstance().apply { set(year, month, 1) }
                        val daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
                        val dayStates = mutableMapOf<Int, DayState>()

                        val goalsInMonth = allGoals.filter { it.year == year && it.month == month }
                        val targetDayForGoalBalance =
                                goalsInMonth.maxByOrNull { it.day }?.day ?: daysInMonth
                        var monthlyGoalCurrentBalanceTemp = currentBalance

                        for (day in 1..daysInMonth) {
                            val currentDayDate = LocalDate.of(year, month + 1, day)
                            val dailyTransactions = monthTransactions.filter { it.day == day }
                            val dailyEvents = monthEvents.filter { it.day == day }

                            // 今日の取引で残高を更新
                            currentBalance +=
                                    FinancialCalculator.calculateDailyBalance(dailyTransactions)

                            if (day <= targetDayForGoalBalance) {
                                monthlyGoalCurrentBalanceTemp +=
                                        FinancialCalculator.calculateDailyBalance(dailyTransactions)
                            }

                            // FinancialCalculatorによる予測ロジック
                            // この日付に関連する「次の」目標が必要
                            val predictionResult =
                                    FinancialCalculator.calculatePrediction(
                                            currentBalance,
                                            allGoals,
                                            currentDayDate
                                    )

                            // 日付が過去または目標日を過ぎている場合に目標を非表示にするロジック
                            val goalForDisplay =
                                    predictionResult.upcomingGoal?.let { goal ->
                                        val goalDate =
                                                LocalDate.of(goal.year, goal.month + 1, goal.day)
                                        if (currentDayDate.isBefore(today) ||
                                                        currentDayDate.isAfter(goalDate)
                                        ) {
                                            null
                                        } else {
                                            goal
                                        }
                                    }

                            val predictionDiffForDisplay =
                                    if (!currentDayDate.isBefore(today)) {
                                        predictionResult.predictionDiff
                                    } else {
                                        null
                                    }

                            // iCalロジック
                            val dailyIcalEvents =
                                    importedEvents.filter { ie ->
                                        val cal = Calendar.getInstance()
                                        ie.event.dateStart?.value?.let { date ->
                                            cal.time = date
                                            cal.get(Calendar.YEAR) == year &&
                                                    cal.get(Calendar.MONTH) == month &&
                                                    cal.get(Calendar.DAY_OF_MONTH) == day
                                        }
                                                ?: false
                                    }

                            dayStates[day] =
                                    DayState(
                                            dayOfMonth = day,
                                            balance = currentBalance,
                                            goal = goalForDisplay,
                                            events = dailyEvents,
                                            transactions = dailyTransactions,
                                            icalEvents = dailyIcalEvents,
                                            predictionDiff = predictionDiffForDisplay,
                                            isHoliday =
                                                    dailyIcalEvents.any { it.isHoliday } ||
                                                            dailyEvents.any { it.isHoliday }
                                    )
                        }

                        val priorGoalsTotal =
                                allGoals
                                        .filter {
                                            it.year < year || (it.year == year && it.month < month)
                                        }
                                        .sumOf { it.amount }

                        val finalMonthlyGoalCurrentBalance =
                                monthlyGoalCurrentBalanceTemp - priorGoalsTotal

                        CalendarUiState(
                                year,
                                month,
                                dayStates,
                                todayBalance,
                                finalMonthlyGoalCurrentBalance
                        )
                    }
                            .collect { calendarUiState -> _uiState.value = calendarUiState }
                }
    }

    fun importIcs(
            uri: Uri,
            contentResolver: ContentResolver,
            isHoliday: Boolean,
            onResult: (String) -> Unit
    ) {
        viewModelScope.launch {
            val result = repository.importIcs(uri, contentResolver, isHoliday)
            onResult(result.getOrDefault(result.exceptionOrNull()?.message ?: "Unknown error"))
        }
    }

    fun importWebcal(url: String, isHoliday: Boolean, onResult: (String) -> Unit) {
        viewModelScope.launch {
            val result = repository.importWebcal(url, isHoliday)
            onResult(result.getOrDefault(result.exceptionOrNull()?.message ?: "Unknown error"))
        }
    }
}

class CalendarViewModelFactory(private val repository: CalYendarRepository) :
        ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CalendarViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST") return CalendarViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
