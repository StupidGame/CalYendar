package io.github.stupidgame.curyendar.data

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

class DetailViewModel(private val dao: CuryendarDao, private val year: Int, private val month: Int, private val day: Int) : ViewModel() {

    val items: Flow<List<CuryendarItem>> = combine(
        dao.getGoalsForDate(year, month, day),
        dao.getExpensesForDate(year, month, day),
        dao.getIncomesForDate(year, month, day)
    ) { goals, expenses, incomes ->
        goals + expenses + incomes
    }

    fun insertGoal(name: String, amount: Double) {
        viewModelScope.launch {
            dao.insertGoal(Goal(name = name, amount = amount, year = year, month = month, day = day))
        }
    }

    fun insertExpense(description: String, amount: Double) {
        viewModelScope.launch {
            dao.insertExpense(Expense(description = description, amount = amount, year = year, month = month, day = day))
        }
    }

    fun insertIncome(description: String, amount: Double) {
        viewModelScope.launch {
            dao.insertIncome(Income(description = description, amount = amount, year = year, month = month, day = day))
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
