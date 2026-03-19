package io.github.stupidgame.calyendar.data

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import io.github.stupidgame.calyendar.utils.EventNotificationManager
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

data class DetailUiState(
    val currentBalance: Long = 0L,
    val goal: FinancialGoal? = null,
    val dailyTransactions: List<Transaction> = emptyList(),
    val events: List<Event> = emptyList(),
    val icalEvents: List<ImportedEvent> = emptyList(),
    val comparisonBalance: Long? = null,
    val totalGoalCost: Long = 0L
) {
    val summaryBalance: Long
        get() = comparisonBalance ?: currentBalance
}

class DetailViewModel(
    private val repository: CalYendarRepository,
    private val notificationManager: EventNotificationManager,
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
            val currentBalance = FinancialCalculator.calculateDailyBalance(allTransactions)
            val prediction =
                FinancialCalculator.calculatePrediction(
                    currentBalance = currentBalance,
                    allGoals = allGoals,
                    currentDate = currentDate
                )

            DetailUiState(
                currentBalance = currentBalance,
                goal = prediction.upcomingGoal,
                dailyTransactions = dailyTransactions,
                events = dailyEvents,
                icalEvents = importedEvents.filterByStartLocalDate(currentDate),
                comparisonBalance = prediction.predictionDiff,
                totalGoalCost = prediction.totalPriorGoalCost
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
            upsertAndSchedule(listOf(event))
        }
    }

    fun upsertEventWithRepeat(
        event: Event,
        repeatType: EventRepeatType,
        repeatUntil: LocalDate?,
        repeatDays: Set<Int>
    ) {
        viewModelScope.launch {
            upsertAndSchedule(
                RecurringEventGenerator.generate(
                    baseEvent = event,
                    repeatType = repeatType,
                    repeatUntil = repeatUntil,
                    repeatDays = repeatDays
                )
            )
        }
    }

    fun deleteEvent(event: Event) {
        viewModelScope.launch {
            notificationManager.cancelEventNotification(event)
            repository.deleteEvent(event)
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

    private suspend fun upsertAndSchedule(events: List<Event>) {
        events.forEach { instance ->
            val id = repository.upsertEvent(instance)
            notificationManager.scheduleEventNotification(instance.copy(id = id))
        }
    }
}

class DetailViewModelFactory(
    private val repository: CalYendarRepository,
    private val notificationManager: EventNotificationManager,
    private val year: Int,
    private val month: Int,
    private val day: Int
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(DetailViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return DetailViewModel(repository, notificationManager, year, month, day) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
