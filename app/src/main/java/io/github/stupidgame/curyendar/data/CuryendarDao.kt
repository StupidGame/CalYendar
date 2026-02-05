package io.github.stupidgame.curyendar.data

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface CuryendarDao {
    @Upsert
    suspend fun upsertTransaction(transaction: Transaction)

    @Query("SELECT * FROM transactions WHERE year = :year AND month = :month AND day = :day ORDER BY id ASC")
    fun getTransactionsForDate(year: Int, month: Int, day: Int): Flow<List<Transaction>>

    @Query("SELECT * FROM transactions WHERE (year < :year) OR (year = :year AND month < :month)")
    fun getTransactionsUpTo(year: Int, month: Int): Flow<List<Transaction>>

    @Query("SELECT * FROM transactions WHERE (year < :year) OR (year = :year AND month < :month) OR (year = :year AND month = :month AND day <= :day)")
    fun getTransactionsUpToDate(year: Int, month: Int, day: Int): Flow<List<Transaction>>

    @Query("SELECT * FROM transactions WHERE type = 'GOAL' AND ((year < :year) OR (year = :year AND month < :month) OR (year = :year AND month = :month AND day <= :day)) ORDER BY year DESC, month DESC, day DESC LIMIT 1")
    fun getLatestGoalUpToDate(year: Int, month: Int, day: Int): Flow<Transaction?>

    @Query("SELECT * FROM transactions WHERE year = :year AND month = :month")
    fun getTransactionsForMonth(year: Int, month: Int): Flow<List<Transaction>>

    @Query("SELECT * FROM transactions WHERE type = 'GOAL' ORDER BY year, month, day")
    fun getAllGoals(): Flow<List<Transaction>>

    @Upsert
    suspend fun upsertEvent(event: Event)

    @Query("SELECT * FROM events WHERE year = :year AND month = :month AND day = :day ORDER BY startTime ASC")
    fun getEventsForDate(year: Int, month: Int, day: Int): Flow<List<Event>>

    @Query("SELECT * FROM events WHERE year = :year AND month = :month")
    fun getEventsForMonth(year: Int, month: Int): Flow<List<Event>>
}
