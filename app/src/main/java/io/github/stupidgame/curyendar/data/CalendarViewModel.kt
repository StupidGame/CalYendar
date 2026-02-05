package io.github.stupidgame.curyendar.data

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
import kotlinx.coroutines.launch
import java.net.URL
import java.util.Calendar

data class DayState(
    val dayOfMonth: Int,
    val balance: Long,
    val goal: Transaction?,
    val events: List<Event>,
    val transactions: List<Transaction>,
    val icalEvents: List<VEvent> = emptyList()
)

data class CalendarUiState(
    val year: Int,
    val month: Int,
    val dayStates: Map<Int, DayState> = emptyMap(),
)

class CalendarViewModel(private val dao: CuryendarDao) : ViewModel() {

    private val _uiState = MutableStateFlow(CalendarUiState(0,0))
    val uiState = _uiState.asStateFlow()

    private val _holidays = MutableStateFlow<List<VEvent>>(emptyList())

    init {
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
                _holidays
            ) { transactionsBefore, monthTransactions, monthEvents, allGoals, holidays ->
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
                    val dailyTransactions = monthTransactions.filter { it.day == day }
                    val dailyEvents = monthEvents.filter { it.day == day }

                    currentBalance += dailyTransactions.sumOf {
                        when (it.type) {
                            TransactionType.INCOME -> it.amount
                            TransactionType.EXPENSE -> -it.amount
                            else -> 0L
                        }
                    }

                    val latestGoal = allGoals.lastOrNull { goal ->
                        (goal.year < year) ||
                        (goal.year == year && goal.month < month) ||
                        (goal.year == year && goal.month == month && goal.day <= day)
                    }

                    val dailyIcalEvents = holidays.filter {
                        val cal = Calendar.getInstance()
                        it.dateStart?.value?.let {
                            date -> cal.time = date
                            cal.get(Calendar.YEAR) == year && cal.get(Calendar.MONTH) == month && cal.get(Calendar.DAY_OF_MONTH) == day
                        } ?: false
                    }

                    dayStates[day] = DayState(
                        dayOfMonth = day,
                        balance = currentBalance,
                        goal = latestGoal,
                        events = dailyEvents,
                        transactions = dailyTransactions,
                        icalEvents = dailyIcalEvents
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
                    _holidays.value = (_holidays.value + ical.events).distinct()
                }
                onResult("インポートに成功しました")
            }.onFailure {
                onResult("インポートに失敗しました")
            }
        }
    }

    fun importWebcal(url: String, onResult: (String) -> Unit) {
        viewModelScope.launch {
            runCatching {
                val ical = Biweekly.parse(URL(url).openStream()).first()
                _holidays.value = (_holidays.value + ical.events).distinct()
                onResult("インポートに成功しました")
            }.onFailure {
                onResult("インポートに失敗しました")
            }
        }
    }
}

class CalendarViewModelFactory(private val dao: CuryendarDao) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CalendarViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return CalendarViewModel(dao) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
