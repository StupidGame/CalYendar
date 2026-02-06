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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import java.net.URL
import java.util.Calendar
import io.github.stupidgame.CalYendar.data.FinancialGoal

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
            val transactionsBeforeFlow = dao.getTransactionsUpTo(year, month)

            combine(
                transactionsBeforeFlow,
                dao.getTransactionsForMonth(year, month),
                dao.getEventsForMonth(year, month),
                dao.getAllGoals(),
                _importedEvents
            ) { transactionsBefore, monthTransactions, monthEvents, allGoals, importedEvents ->
                // Sort goals for logic
                val sortedGoals = allGoals.sortedWith(compareBy({ it.year }, { it.month }, { it.day }))
                val lastGoal = sortedGoals.lastOrNull()

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

                val today = java.time.LocalDate.now()

                for (day in 1..daysInMonth) {
                    val currentDayDate = java.time.LocalDate.of(year, month + 1, day)
                    
                    val dailyTransactions = monthTransactions.filter { it.day == day }
                    val dailyEvents = monthEvents.filter { it.day == day }

                    currentBalance += dailyTransactions.sumOf {
                        when (it.type) {
                            TransactionType.INCOME -> it.amount
                            TransactionType.EXPENSE -> -it.amount
                            else -> 0L
                        }
                    }

                    val latestGoal = sortedGoals.lastOrNull { goal ->
                        (goal.year < year) ||
                        (goal.year == year && goal.month < month) ||
                        (goal.year == year && goal.month == month && goal.day <= day)
                    }

                    val dailyIcalEvents = importedEvents.filter {
                        val cal = Calendar.getInstance()
                        it.dateStart?.value?.let {
                            date -> cal.time = date
                            cal.get(Calendar.YEAR) == year && cal.get(Calendar.MONTH) == month && cal.get(Calendar.DAY_OF_MONTH) == day
                        } ?: false
                    }
                    
                    val isFixedHoliday = JpHolidays.isHoliday(year, month, day)

                    // Prediction Logic
                    var predictionDiff: Long? = null
                    if (lastGoal != null && !currentDayDate.isBefore(today)) {
                         val lastGoalDate = java.time.LocalDate.of(lastGoal.year, lastGoal.month + 1, lastGoal.day)
                         if (!currentDayDate.isAfter(lastGoalDate)) {
                             // Find next goal (inclusive of today)
                             val nextGoal = sortedGoals.firstOrNull { 
                                 val gDate = java.time.LocalDate.of(it.year, it.month + 1, it.day)
                                 !gDate.isBefore(currentDayDate)
                             }

                             if (nextGoal != null) {
                                 val nextGoalDate = java.time.LocalDate.of(nextGoal.year, nextGoal.month + 1, nextGoal.day)
                                 
                                 // Calculate deducted goals (strictly before nextGoal)
                                 val deductedAmount = sortedGoals
                                     .filter { 
                                         val gDate = java.time.LocalDate.of(it.year, it.month + 1, it.day)
                                         gDate.isBefore(nextGoalDate)
                                     }
                                     .sumOf { it.amount }

                                 predictionDiff = (currentBalance - deductedAmount) - nextGoal.amount
                             }
                         }
                    }

                    dayStates[day] = DayState(
                        dayOfMonth = day,
                        balance = currentBalance,
                        goal = latestGoal,
                        events = dailyEvents,
                        transactions = dailyTransactions,
                        icalEvents = dailyIcalEvents,
                        isHoliday = isFixedHoliday,
                        predictionDiff = predictionDiff
                    )
                }

                CalendarUiState(year, month, dayStates)

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
