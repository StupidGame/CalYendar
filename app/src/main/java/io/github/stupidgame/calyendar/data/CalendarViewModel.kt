package io.github.stupidgame.calyendar.data

import android.content.ContentResolver
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import java.time.LocalDate
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
    val todayBalance: Long = 0L,
    val monthGoals: List<FinancialGoal> = emptyList(),
    val activeMonthGoals: List<FinancialGoal> = emptyList(),
    val availableMoneyAfterMonthGoals: Long = 0L,
    val hasTransactions: Boolean = false,
    val isCurrentMonth: Boolean = false
)

class CalendarViewModel(private val repository: CalYendarRepository) : ViewModel() {

    private val _uiState =
        MutableStateFlow(
            LocalDate.now().let { today -> CalendarUiState(today.year, today.monthValue - 1) }
        )
    val uiState = _uiState.asStateFlow()

    private var loadMonthJob: Job? = null

    init {
        loadMonth(uiState.value.year, uiState.value.month)
        viewModelScope.launch { repository.fetchJapaneseHolidays() }
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
                    CalendarMonthUiStateFactory.create(
                        CalendarMonthUiStateInput(
                            year = year,
                            month = month,
                            today = today,
                            transactionsUpToToday = values[0] as List<Transaction>,
                            transactionsBeforeMonth = values[1] as List<Transaction>,
                            monthTransactions = values[2] as List<Transaction>,
                            monthEvents = values[3] as List<Event>,
                            allGoals = values[4] as List<FinancialGoal>,
                            importedEvents = values[5] as List<ImportedEvent>
                        )
                    )
                }.collect { calendarUiState ->
                    _uiState.value = calendarUiState
                }
            }
    }

    fun importIcs(uri: Uri, contentResolver: ContentResolver, onResult: (String) -> Unit) {
        viewModelScope.launch {
            val result = repository.importIcs(uri, contentResolver)
            onResult(result.getOrDefault(result.exceptionOrNull()?.message ?: "Import failed."))
        }
    }

    fun importWebcal(url: String, onResult: (String) -> Unit) {
        viewModelScope.launch {
            val result = repository.importWebcal(url)
            onResult(result.getOrDefault(result.exceptionOrNull()?.message ?: "Import failed."))
        }
    }
}

class CalendarViewModelFactory(private val repository: CalYendarRepository) :
    ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CalendarViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return CalendarViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
