package io.github.stupidgame.calyendar.data

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import io.github.stupidgame.calyendar.utils.EventNotificationManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId
import java.util.Calendar

data class DetailUiState(
    val balance: Long = 0,
    val transactionBalance: Long = 0,
    val goal: FinancialGoal? = null,
    val dailyTransactions: List<Transaction> = emptyList(),
    val events: List<Event> = emptyList(),
    val icalEvents: List<ImportedEvent> = emptyList(),
    val predictionBalance: Long? = null,
    val totalGoalCost: Long = 0L
)

class DetailViewModel(
    private val repository: CalYendarRepository,
    private val notificationManager: EventNotificationManager,
    val year: Int,
    val month: Int,
    val day: Int
) : ViewModel() {

    val uiState: Flow<DetailUiState> = combine(
        repository.getTransactionsUpToDate(year, month, day),
        repository.getAllGoals(),
        repository.getEventsForDate(year, month, day),
        repository.getTransactionsForDate(year, month, day),
        repository.getImportedEvents()
    ) { allTransactions, allGoals, dailyEvents, dailyTransactions, importedEvents ->
        
        // この日付までの合計残高を計算
        val transactionBalance = FinancialCalculator.calculateDailyBalance(allTransactions)

        val currentDayDate = LocalDate.of(year, month + 1, day)
        
        // 予測の計算
        val predictionResult = FinancialCalculator.calculatePrediction(
            currentBalance = transactionBalance,
            allGoals = allGoals,
            currentDate = currentDayDate
        )

        // この日のiCalイベント
        val dailyIcalEvents = importedEvents.filter {
            val cal = Calendar.getInstance()
            it.event.dateStart?.value?.let {
                date -> cal.time = date
                cal.get(Calendar.YEAR) == year && cal.get(Calendar.MONTH) == month && cal.get(Calendar.DAY_OF_MONTH) == day
            } ?: false
        }

        DetailUiState(
            balance = transactionBalance,
            transactionBalance = transactionBalance, // 元のコードでも冗長ですが、APIを維持します
            goal = predictionResult.upcomingGoal,
            dailyTransactions = dailyTransactions as List<Transaction>,
            events = dailyEvents as List<Event>,
            icalEvents = dailyIcalEvents,
            predictionBalance = predictionResult.predictionDiff,
            totalGoalCost = predictionResult.totalPriorGoalCost
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
            val id = repository.upsertEvent(event)
            // 通知の再スケジュール
            notificationManager.scheduleEventNotification(event.copy(id = id))
        }
    }

    fun upsertEventWithRepeat(event: Event, repeatType: String, repeatUntil: LocalDate?, repeatDays: Set<Int>) {
        viewModelScope.launch {
            if (repeatType == "なし" || repeatUntil == null) {
                val id = repository.upsertEvent(event)
                notificationManager.scheduleEventNotification(event.copy(id = id))
                return@launch
            }

            val seriesId = java.util.UUID.randomUUID().toString()
            val eventsToInsert = mutableListOf<Event>()
            var currentDate = LocalDate.of(event.year, event.month + 1, event.day)
            
            // For calculating offsets
            val zoneId = ZoneId.systemDefault()
            val startLocalTime = java.time.Instant.ofEpochMilli(event.startTime).atZone(zoneId).toLocalTime()
            val endLocalTime = java.time.Instant.ofEpochMilli(event.endTime).atZone(zoneId).toLocalTime()

            while (!currentDate.isAfter(repeatUntil)) {
                val shouldAdd = when (repeatType) {
                    "毎日" -> true
                    "毎週" -> currentDate.dayOfWeek == LocalDate.of(event.year, event.month + 1, event.day).dayOfWeek
                    "曜日指定" -> repeatDays.contains(currentDate.dayOfWeek.value)
                    else -> false
                }

                if (shouldAdd) {
                    val instanceStart = currentDate.atTime(startLocalTime).atZone(zoneId).toInstant().toEpochMilli()
                    val instanceEnd = currentDate.atTime(endLocalTime).atZone(zoneId).toInstant().toEpochMilli()
                    
                    eventsToInsert.add(event.copy(
                        id = 0, // Auto-generate new IDs
                        year = currentDate.year,
                        month = currentDate.monthValue - 1,
                        day = currentDate.dayOfMonth,
                        startTime = instanceStart,
                        endTime = instanceEnd,
                        seriesId = seriesId
                    ))
                }
                currentDate = currentDate.plusDays(1)
            }

            // Insert all at once or individually? Wait, CalYendarDao upsertEvents doesn't return IDs easily in Room without changes.
            // Oh, wait, upsertEvents is defined, but we need IDs to schedule notifications.
            // Let's iterate and insert.
            eventsToInsert.forEach { instance ->
                val id = repository.upsertEvent(instance)
                notificationManager.scheduleEventNotification(instance.copy(id = id))
            }
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
            return DetailViewModel(
                repository,
                notificationManager,
                year,
                month,
                day
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
