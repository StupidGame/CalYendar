package io.github.stupidgame.calyendar.data

import android.content.ContentResolver
import android.net.Uri
import android.util.Log
import biweekly.Biweekly
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException

class CalYendarRepository(private val dao: CalYendarDao) {

    // 取引メソッド
    fun getTransactionsForDate(year: Int, month: Int, day: Int): Flow<List<Transaction>> =
        dao.getTransactionsForDate(year, month, day)

    fun getTransactionsUpToDate(year: Int, month: Int, day: Int): Flow<List<Transaction>> =
        dao.getTransactionsUpToDate(year, month, day)
    
    fun getTransactionsUpToToday(year: Int, month: Int, day: Int): Flow<List<Transaction>> =
        dao.getTransactionsUpToToday(year, month, day)

    fun getTransactionsUpTo(year: Int, month: Int): Flow<List<Transaction>> =
        dao.getTransactionsUpTo(year, month)

    fun getTransactionsForMonth(year: Int, month: Int): Flow<List<Transaction>> =
        dao.getTransactionsForMonth(year, month)

    suspend fun upsertTransaction(transaction: Transaction) = dao.upsertTransaction(transaction)
    suspend fun deleteTransaction(transaction: Transaction) = dao.deleteTransaction(transaction)

    // 目標メソッド
    fun getAllGoals(): Flow<List<FinancialGoal>> = dao.getAllGoals()
    
    suspend fun upsertFinancialGoal(goal: FinancialGoal) = dao.upsertFinancialGoal(goal)
    suspend fun deleteFinancialGoal(goal: FinancialGoal) = dao.deleteFinancialGoal(goal)

    // イベントメソッド
    fun getEventsForDate(year: Int, month: Int, day: Int): Flow<List<Event>> =
        dao.getEventsForDate(year, month, day)

    fun getEventsForMonth(year: Int, month: Int): Flow<List<Event>> =
        dao.getEventsForMonth(year, month)

    suspend fun upsertEvent(event: Event): Long = dao.upsertEvent(event)
    suspend fun deleteEvent(event: Event) = dao.deleteEvent(event)

    // インポートしたイベントのメソッド
    fun getImportedEvents(): Flow<List<ImportedEvent>> = dao.getImportedEvents()

    suspend fun upsertImportedEvents(events: List<ImportedEvent>) = dao.upsertImportedEvents(events)
    suspend fun deleteImportedEvent(event: ImportedEvent) = dao.deleteImportedEvent(event)
    suspend fun clearImportedEvents() = dao.clearImportedEvents()

    // インポートロジック (ViewModelから移動)
    suspend fun importIcs(uri: Uri, contentResolver: ContentResolver, isHoliday: Boolean): Result<String> {
        return withContext(Dispatchers.IO) {
            runCatching {
                contentResolver.openInputStream(uri)?.use { inputStream ->
                    val ical = Biweekly.parse(inputStream).first()
                    if (ical == null) throw IOException("Failed to parse iCal")
                    
                    val events = ical.events.map { event ->
                        val holiday = isHoliday || event.summary?.value?.contains("休日") == true
                        ImportedEvent(event = event, isHoliday = holiday)
                    }
                    dao.upsertImportedEvents(events)
                    "インポートに成功しました"
                } ?: throw IOException("Could not open input stream")
            }
        }
    }

    suspend fun importWebcal(url: String, isHoliday: Boolean): Result<String> {
        return withContext(Dispatchers.IO) {
            runCatching {
                val client = OkHttpClient()
                val request = Request.Builder()
                    .url(url.replace("webcal", "https"))
                    .build()
                
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) throw IOException("Unexpected code $response")

                    val inputStream = response.body?.byteStream() ?: throw IOException("Empty body")
                    val ical = Biweekly.parse(inputStream).first()
                    if (ical == null) throw IOException("Failed to parse iCal")

                    val events = ical.events.map { event ->
                        val holiday = isHoliday || event.summary?.value?.contains("休日") == true
                        ImportedEvent(event = event, isHoliday = holiday)
                    }
                    dao.upsertImportedEvents(events)
                    "インポートに成功しました"
                }
            }
        }
    }
}
