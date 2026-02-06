package io.github.stupidgame.CalYendar.data

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import biweekly.Biweekly
import biweekly.component.VEvent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import java.net.URL
import java.time.LocalDate
import java.util.Calendar

data class DayState(
    val dayOfMonth: Int,
    val balance: Long,
    val goal: FinancialGoal?,
    val events: List<Event>,
    val transactions: List<Transaction>,
    val icalEvents: List<VEvent> = emptyList(),
    val isHoliday: Boolean = false,
    val predictionDiff: Long? = null
)

data class CalendarUiState(
    val year: Int,
    val month: Int,
    val dayStates: Map<Int, DayState> = emptyMap(),
    val currentBalance: Long = 0L
)

class CalendarViewModel(private val dao: CalYendarDao) : ViewModel() {

    private val _uiState = MutableStateFlow(CalendarUiState(0,0))
    val uiState = _uiState.asStateFlow()

    private val _importedEvents = MutableStateFlow<List<VEvent>>(emptyList())

    init {
        // Optional: Load webcal if internet works, otherwise ignore silently
        importWebcal("https://www.officeholidays.com/ics/japan") {}
    }

    fun loadMonth(year: Int, month: Int) {
        viewModelScope.launch {
            val today = LocalDate.now()
            val transactionsUpToTodayFlow = dao.getTransactionsUpToToday(today.year, today.monthValue - 1, today.dayOfMonth)
            val transactionsBeforeFlow = dao.getTransactionsUpTo(year, month)

            combine(
                transactionsUpToTodayFlow,
                transactionsBeforeFlow,
                dao.getTransactionsForMonth(year, month),
                dao.getEventsForMonth(year, month),
                dao.getAllGoals(),
                _importedEvents
            ) { values ->
                val transactionsUpToToday = values[0] as List<Transaction>
                val transactionsBefore = values[1] as List<Transaction>
                val monthTransactions = values[2] as List<Transaction>
                val monthEvents = values[3] as List<Event>
                val allGoals = values[4] as List<FinancialGoal>
                val importedEvents = values[5] as List<VEvent>

                val sortedGoals = allGoals.sortedWith(compareBy({ it.year }, { it.month }, { it.day }))

                val todayBalance = transactionsUpToToday.sumOf {
                    when (it.type) {
                        TransactionType.INCOME -> it.amount
                        TransactionType.EXPENSE -> -it.amount
                        else -> 0L
                    }
                }

                var currentBalance = transactionsBefore.sumOf {
                    when (it.type) {
                        TransactionType.INCOME -> it.amount
                        TransactionType.EXPENSE -> -it.amount
                        else -> 0L
                    }
                }

                val calendar = Calendar.getInstance().apply { set(year, month, 1) }
                val daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
                val dayStates = mutableMapOf<Int, DayState>()

                for (day in 1..daysInMonth) {
                    val currentDayDate = LocalDate.of(year, month + 1, day)
                    val dailyTransactions = monthTransactions.filter { it.day == day }
                    val dailyEvents = monthEvents.filter { it.day == day }

                    currentBalance += dailyTransactions.sumOf {
                        when (it.type) {
                            TransactionType.INCOME -> it.amount
                            TransactionType.EXPENSE -> -it.amount
                            else -> 0L
                        }
                    }

                    val upcomingGoal = sortedGoals.firstOrNull { goal ->
                        val goalDate = LocalDate.of(goal.year, goal.month + 1, goal.day)
                        !goalDate.isBefore(currentDayDate)
                    }

                    val goalForDisplay = upcomingGoal?.let { goal ->
                        val goalDate = LocalDate.of(goal.year, goal.month + 1, goal.day)
                        if (currentDayDate.isBefore(today) || currentDayDate.isAfter(goalDate)) {
                            null
                        } else {
                            goal
                        }
                    }

                    val dailyIcalEvents = importedEvents.filter {
                        val cal = Calendar.getInstance()
                        it.dateStart?.value?.let {
                            date -> cal.time = date
                            cal.get(Calendar.YEAR) == year && cal.get(Calendar.MONTH) == month && cal.get(Calendar.DAY_OF_MONTH) == day
                        } ?: false
                    }

                    val isFixedHoliday = JpHolidays.isHoliday(year, month, day)

                    var predictionDiff: Long? = null
                    if (!currentDayDate.isBefore(today)) {
                        predictionDiff = upcomingGoal?.let { goal ->
                            val upcomingGoalIndex = sortedGoals.indexOf(goal)
                            if (upcomingGoalIndex != -1) {
                                val goalsToConsider = sortedGoals.subList(0, upcomingGoalIndex + 1)
                                val totalGoalCost = goalsToConsider.sumOf { it.amount }
                                currentBalance - totalGoalCost
                            } else {
                                null
                            }
                        }
                    }

                    dayStates[day] = DayState(
                        dayOfMonth = day,
                        balance = currentBalance,
                        goal = goalForDisplay,
                        events = dailyEvents,
                        transactions = dailyTransactions,
                        icalEvents = dailyIcalEvents,
                        isHoliday = isFixedHoliday,
                        predictionDiff = predictionDiff
                    )
                }

                CalendarUiState(year, month, dayStates, todayBalance)

            }.collect { calendarUiState ->
                _uiState.value = calendarUiState
            }
        }
    }

    fun importIcs(uri: Uri, context: Context, onResult: (String) -> Unit) {
        viewModelScope.launch {
            runCatching {
                context.contentResolver.openInputStream(uri)?.use {
                    val ical = Biweekly.parse(it).first()
                    _importedEvents.value = (_importedEvents.value + ical.events).distinct()
                }
                onResult("インポートに成功しました")
            }.onFailure {
                onResult("インポートに失敗しました")
            }
        }
    }

    fun importWebcal(url: String, onResult: (String) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                val ical = Biweekly.parse(URL(url).openStream()).first()
                _importedEvents.value = (_importedEvents.value + ical.events).distinct()
                onResult("インポートに成功しました")
            }.onFailure {
                // Silently fail or log
            }
        }
    }
}

class CalendarViewModelFactory(private val dao: CalYendarDao) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CalendarViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return CalendarViewModel(dao) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
