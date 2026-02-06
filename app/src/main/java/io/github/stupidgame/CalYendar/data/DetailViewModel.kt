package io.github.stupidgame.CalYendar.data

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

import io.github.stupidgame.CalYendar.data.FinancialGoal

data class DetailUiState(
    val balance: Long = 0,
    val goal: FinancialGoal? = null,
    val dailyTransactions: List<Transaction> = emptyList(),
    val events: List<Event> = emptyList()
)

class DetailViewModel(private val dao: CalYendarDao, val year: Int, val month: Int, val day: Int) : ViewModel() {

    val uiState: Flow<DetailUiState> = combine(
        dao.getTransactionsUpToDate(year, month, day),
        dao.getLatestGoalUpToDate(year, month, day),
        dao.getEventsForDate(year, month, day),
        dao.getTransactionsForDate(year, month, day)
    ) { allTransactions, latestGoal, dailyEvents, dailyTransactions ->
        val balance = allTransactions.sumOf {
            when (it.type) {
                TransactionType.INCOME -> it.amount
                TransactionType.EXPENSE -> -it.amount
                else -> 0L
            }
        }
        DetailUiState(
            balance = balance,
            goal = latestGoal,
            dailyTransactions = dailyTransactions,
            events = dailyEvents
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
            dao.upsertEvent(event)
        }
    }

    fun deleteEvent(event: Event) {
        viewModelScope.launch(Dispatchers.IO) {
            dao.deleteEvent(event)
        }
    }
}

class DetailViewModelFactory(private val dao: CalYendarDao, private val year: Int, private val month: Int, private val day: Int) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(DetailViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return DetailViewModel(dao, year, month, day) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
