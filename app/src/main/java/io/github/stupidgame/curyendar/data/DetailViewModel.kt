package io.github.stupidgame.curyendar.data

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

data class DetailUiState(
    val balance: Long = 0,
    val goal: Transaction? = null,
    val dailyTransactions: List<Transaction> = emptyList(),
    val events: List<Event> = emptyList()
)

class DetailViewModel(private val dao: CuryendarDao, val year: Int, val month: Int, val day: Int) : ViewModel() {

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

    fun upsertEvent(event: Event) {
        viewModelScope.launch(Dispatchers.IO) {
            dao.upsertEvent(event)
        }
    }
}

class DetailViewModelFactory(private val dao: CuryendarDao, private val year: Int, private val month: Int, private val day: Int) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(DetailViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return DetailViewModel(dao, year, month, day) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
