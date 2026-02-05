package io.github.stupidgame.curyendar.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface CuryendarDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertGoal(goal: Goal)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertExpense(expense: Expense)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertIncome(income: Income)

    @Query("SELECT * FROM goals WHERE year = :year AND month = :month AND day = :day")
    fun getGoalsForDate(year: Int, month: Int, day: Int): Flow<List<Goal>>

    @Query("SELECT * FROM expenses WHERE year = :year AND month = :month AND day = :day")
    fun getExpensesForDate(year: Int, month: Int, day: Int): Flow<List<Expense>>

    @Query("SELECT * FROM incomes WHERE year = :year AND month = :month AND day = :day")
    fun getIncomesForDate(year: Int, month: Int, day: Int): Flow<List<Income>>
}
