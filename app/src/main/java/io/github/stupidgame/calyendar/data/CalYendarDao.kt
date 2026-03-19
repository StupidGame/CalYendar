package io.github.stupidgame.calyendar.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface CalYendarDao {
    @Upsert
    suspend fun upsertTransaction(transaction: Transaction)

    @Upsert
    suspend fun upsertTransactions(transactions: List<Transaction>)

    @Delete
    suspend fun deleteTransaction(transaction: Transaction)

    @Query("SELECT * FROM transactions WHERE year = :year AND month = :month AND day = :day ORDER BY id ASC")
    fun getTransactionsForDate(year: Int, month: Int, day: Int): Flow<List<Transaction>>

    @Query("SELECT * FROM transactions WHERE (year < :year) OR (year = :year AND month < :month)")
    fun getTransactionsUpTo(year: Int, month: Int): Flow<List<Transaction>>

    @Query("SELECT * FROM transactions WHERE (year < :year) OR (year = :year AND month < :month) OR (year = :year AND month = :month AND day <= :day)")
    fun getTransactionsUpToDate(year: Int, month: Int, day: Int): Flow<List<Transaction>>

    @Query("SELECT * FROM transactions WHERE (year < :year) OR (year = :year AND month < :month) OR (year = :year AND month = :month AND day <= :day)")
    fun getTransactionsUpToToday(year: Int, month: Int, day: Int): Flow<List<Transaction>>

    @Query("SELECT * FROM transactions WHERE year = :year AND month = :month")
    fun getTransactionsForMonth(year: Int, month: Int): Flow<List<Transaction>>

    @Query("SELECT * FROM transactions ORDER BY year ASC, month ASC, day ASC, id ASC")
    suspend fun getAllTransactionsSnapshot(): List<Transaction>

    @Query("DELETE FROM transactions")
    suspend fun clearTransactions()

    @Upsert
    suspend fun upsertFinancialGoal(goal: FinancialGoal)

    @Upsert
    suspend fun upsertFinancialGoals(goals: List<FinancialGoal>)

    @Delete
    suspend fun deleteFinancialGoal(goal: FinancialGoal)

    @Query("SELECT * FROM financial_goals WHERE (year < :year) OR (year = :year AND month < :month) OR (year = :year AND month = :month AND day <= :day) ORDER BY year DESC, month DESC, day DESC LIMIT 1")
    fun getLatestGoalUpToDate(year: Int, month: Int, day: Int): Flow<FinancialGoal?>

    @Query("SELECT * FROM financial_goals ORDER BY year, month, day")
    fun getAllGoals(): Flow<List<FinancialGoal>>

    @Query("SELECT * FROM financial_goals ORDER BY year ASC, month ASC, day ASC, id ASC")
    suspend fun getAllGoalsSnapshot(): List<FinancialGoal>

    @Query("DELETE FROM financial_goals")
    suspend fun clearFinancialGoals()

    @Upsert
    suspend fun upsertEvent(event: Event): Long

    @Upsert
    suspend fun upsertEvents(events: List<Event>)

    @Delete
    suspend fun deleteEvent(event: Event)

    @Query("SELECT * FROM events WHERE year = :year AND month = :month AND day = :day ORDER BY startTime ASC")
    fun getEventsForDate(year: Int, month: Int, day: Int): Flow<List<Event>>

    @Query("SELECT * FROM events WHERE year = :year AND month = :month")
    fun getEventsForMonth(year: Int, month: Int): Flow<List<Event>>

    @Query("SELECT * FROM events ORDER BY startTime ASC, id ASC")
    suspend fun getAllEventsSnapshot(): List<Event>

    @Query("DELETE FROM events")
    suspend fun clearEvents()

    @Upsert
    suspend fun upsertImportedEvents(events: List<ImportedEvent>)

    @Delete
    suspend fun deleteImportedEvent(event: ImportedEvent)

    @Query("DELETE FROM imported_events WHERE isHoliday = 0")
    suspend fun clearImportedEvents()

    @Query("DELETE FROM imported_events WHERE isHoliday = 1")
    suspend fun deleteHolidays()

    @Query("SELECT * FROM imported_events")
    fun getImportedEvents(): Flow<List<ImportedEvent>>
}
