package io.github.stupidgame.calyendar.data

import android.app.AlarmManager
import android.app.Application
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import io.github.stupidgame.calyendar.EventNotificationReceiver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.util.Calendar

data class DetailUiState(
    val balance: Long = 0,
    val transactionBalance: Long = 0,
    val goal: FinancialGoal? = null,
    val dailyTransactions: List<Transaction> = emptyList(),
    val events: List<Event> = emptyList(),
    val icalEvents: List<ImportedEvent> = emptyList()
)

class DetailViewModel(
    private val application: Application,
    private val dao: CalYendarDao,
    val year: Int,
    val month: Int,
    val day: Int
) : ViewModel() {

    val uiState: Flow<DetailUiState> = combine(
        dao.getTransactionsUpToDate(year, month, day),
        dao.getAllGoals(),
        dao.getEventsForDate(year, month, day),
        dao.getTransactionsForDate(year, month, day),
        dao.getImportedEvents()
    ) { allTransactions, allGoals, dailyEvents, dailyTransactions, importedEvents ->
        val transactionBalance = (allTransactions as List<Transaction>).sumOf {
            when (it.type) {
                TransactionType.INCOME -> it.amount
                TransactionType.EXPENSE -> -it.amount
                else -> 0L
            }
        }

        val currentDayDate = LocalDate.of(year, month + 1, day)
        val sortedGoals = (allGoals as List<FinancialGoal>).sortedWith(compareBy({ it.year }, { it.month }, { it.day }))
        val previousGoalsTotal = sortedGoals.sumOf { goal ->
            val goalDate = LocalDate.of(goal.year, goal.month + 1, goal.day)
            if (goalDate.isBefore(currentDayDate)) goal.amount else 0L
        }
        val adjustedTransactionBalance = transactionBalance - previousGoalsTotal
        val latestGoal = sortedGoals.firstOrNull { goal ->
            val goalDate = LocalDate.of(goal.year, goal.month + 1, goal.day)
            !currentDayDate.isAfter(goalDate)
        }

        val finalBalance = latestGoal?.let { goal ->
            adjustedTransactionBalance - goal.amount
        }
        val finalBalance = transactionBalance - achievedGoals.sumOf { it.amount }

        val dailyIcalEvents = (importedEvents as List<ImportedEvent>).filter {
            val cal = Calendar.getInstance()
            it.event.dateStart?.value?.let {
                date -> cal.time = date
                cal.get(Calendar.YEAR) == year && cal.get(Calendar.MONTH) == month && cal.get(Calendar.DAY_OF_MONTH) == day
            } ?: false
        }

        DetailUiState(
            balance = finalBalance ?: adjustedTransactionBalance,
            transactionBalance = adjustedTransactionBalance,
            goal = latestGoal,
            dailyTransactions = dailyTransactions as List<Transaction>,
            events = dailyEvents as List<Event>,
            icalEvents = dailyIcalEvents
        )
    }

    fun upsertTransaction(transaction: Transaction) {
        viewModelScope.launch(Dispatchers.IO) {
            dao.upsertTransaction(transaction)
        }
    }

    fun deleteTransaction(transaction: Transaction) {
        viewModelScope.launch(Dispatchers.IO) {
            dao.deleteTransaction(transaction)
        }
    }

    fun upsertFinancialGoal(goal: FinancialGoal) {
        viewModelScope.launch(Dispatchers.IO) {
            dao.upsertFinancialGoal(goal)
        }
    }

    fun deleteFinancialGoal(goal: FinancialGoal) {
        viewModelScope.launch(Dispatchers.IO) {
            dao.deleteFinancialGoal(goal)
        }
    }

    fun upsertEvent(event: Event) {
        viewModelScope.launch(Dispatchers.IO) {
            val id = dao.upsertEvent(event)
            if (event.notificationMinutesBefore != (-1).toLong()) {
                scheduleEventNotification(event.copy(id = id))
            }
        }
    }

    fun deleteEvent(event: Event) {
        viewModelScope.launch(Dispatchers.IO) {
            cancelEventNotification(event)
            dao.deleteEvent(event)
        }
    }

    fun deleteImportedEvent(event: ImportedEvent) {
        viewModelScope.launch(Dispatchers.IO) {
            dao.deleteImportedEvent(event)
        }
    }

    fun clearImportedEvents() {
        viewModelScope.launch(Dispatchers.IO) {
            dao.clearImportedEvents()
        }
    }

    private fun scheduleEventNotification(event: Event) {
        val alarmManager = application.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(application, EventNotificationReceiver::class.java).apply {
            putExtra("event_title", event.title)
            putExtra("event_id", event.id.toInt())
        }
        val pendingIntent = PendingIntent.getBroadcast(
            application,
            event.id.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notificationTime =
            event.startTime - (event.notificationMinutesBefore * 60 * 1000)

        if (notificationTime > System.currentTimeMillis()) {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                notificationTime,
                pendingIntent
            )
        }
    }

    private fun cancelEventNotification(event: Event) {
        val alarmManager = application.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(application, EventNotificationReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            application,
            event.id.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
    }
}

class DetailViewModelFactory(
    private val application: Application,
    private val dao: CalYendarDao,
    private val year: Int,
    private val month: Int,
    private val day: Int
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(DetailViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return DetailViewModel(application, dao, year, month, day) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
