package io.github.stupidgame.calyendar.data

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

data class DetailUiState(
    val currentBalance: Long = 0L,
    val goals: List<FinancialGoal> = emptyList(),
    val goalTargetAmount: Long? = null,
    val dailyTransactions: List<Transaction> = emptyList(),
    val events: List<Event> = emptyList(),
    val icalEvents: List<ImportedEvent> = emptyList(),
    val totalGoalCost: Long = 0L
)

class DetailViewModel(
    private val repository: CalYendarRepository,
    private val eventSyncService: EventSyncService,
    val year: Int,
    val month: Int,
    val day: Int
) : ViewModel() {

    private val currentDate = LocalDate.of(year, month + 1, day)

    val uiState: Flow<DetailUiState> =
        combine(
            repository.getTransactionsUpToDate(year, month, day),
            repository.getAllGoals(),
            repository.getEventsForDate(year, month, day),
            repository.getTransactionsForDate(year, month, day),
            repository.getImportedEvents()
        ) { allTransactions, allGoals, dailyEvents, dailyTransactions, importedEvents ->
            val rawCurrentBalance = FinancialCalculator.calculateDailyBalance(allTransactions)
            val today = LocalDate.now()
            val currentProjection =
                FinancialCalculator.calculateGoalProjection(
                    rawBalance = rawCurrentBalance,
                    allGoals = allGoals,
                    currentDate = currentDate,
                    goalWindowStartDate = today
                )
            val predictionDate = if (currentDate.isBefore(today)) today else currentDate
            val predictionProjection =
                FinancialCalculator.calculateGoalProjection(
                    rawBalance = rawCurrentBalance,
                    allGoals = allGoals,
                    currentDate = predictionDate,
                    goalWindowStartDate = today
                )
            val detailGoals =
                selectEditableDetailGoals(
                    allGoals = allGoals,
                    selectedDate = currentDate,
                    fallbackGoal = predictionProjection.upcomingGoal
                )

            DetailUiState(
                currentBalance = currentProjection.currentBalance,
                goals = detailGoals,
                goalTargetAmount = calculateDetailGoalTargetAmount(detailGoals),
                dailyTransactions = dailyTransactions,
                events = dailyEvents,
                icalEvents = importedEvents.filterByStartLocalDate(currentDate),
                totalGoalCost = predictionProjection.reachedGoalCost
            )
        }

    fun upsertTransaction(transaction: Transaction) {
        viewModelScope.launch {
            repository.upsertTransaction(transaction)
        }
    }

    fun deleteTransaction(transaction: Transaction) {
        viewModelScope.launch {
            repository.deleteTransaction(transaction)
        }
    }

    fun upsertFinancialGoal(goal: FinancialGoal) {
        viewModelScope.launch {
            repository.upsertFinancialGoal(goal)
        }
    }

    fun deleteFinancialGoal(goal: FinancialGoal) {
        viewModelScope.launch {
            repository.deleteFinancialGoal(goal)
        }
    }

    fun upsertEvent(event: Event) {
        viewModelScope.launch {
            eventSyncService.upsertEvent(event)
        }
    }

    fun upsertEventWithRepeat(
        event: Event,
        repeatType: EventRepeatType,
        repeatUntil: LocalDate?,
        repeatDays: Set<Int>
    ) {
        viewModelScope.launch {
            eventSyncService.upsertRepeatedEvent(event, repeatType, repeatUntil, repeatDays)
        }
    }

    fun deleteEvent(event: Event) {
        viewModelScope.launch {
            eventSyncService.deleteEvent(event)
        }
    }

    fun deleteImportedEvent(event: ImportedEvent) {
        viewModelScope.launch {
            repository.deleteImportedEvent(event)
        }
    }

    fun clearImportedEvents() {
        viewModelScope.launch {
            repository.clearImportedEvents()
        }
    }
}

internal fun selectEditableDetailGoals(
    allGoals: List<FinancialGoal>,
    selectedDate: LocalDate,
    fallbackGoal: FinancialGoal?
): List<FinancialGoal> {
    val selectedDateGoals = allGoals.onDate(selectedDate)
    if (selectedDateGoals.isNotEmpty()) return selectedDateGoals

    return fallbackGoal
        ?.let { goal ->
            allGoals.onDate(goal.toLocalDate()).ifEmpty { listOf(goal) }
        }
        .orEmpty()
}

internal fun calculateDetailGoalTargetAmount(goals: List<FinancialGoal>): Long? {
    if (goals.isEmpty()) return null
    return goals.sumOf(FinancialGoal::amount)
}

class DetailViewModelFactory(
    private val repository: CalYendarRepository,
    private val eventSyncService: EventSyncService,
    private val year: Int,
    private val month: Int,
    private val day: Int
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(DetailViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return DetailViewModel(repository, eventSyncService, year, month, day) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
