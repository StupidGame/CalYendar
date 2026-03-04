package io.github.stupidgame.calyendar.data

import android.content.ContentResolver
import android.net.Uri
import android.util.Log
import biweekly.Biweekly
import biweekly.component.VEvent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

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

    // 祝日API取得ロジック
    suspend fun fetchJapaneseHolidays() {
        withContext(Dispatchers.IO) {
            try {
                val client = OkHttpClient()
                val request = Request.Builder()
                    .url("https://holidays-jp.github.io/api/v1/date.json")
                    .build()

                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) return@withContext

                    val json = JSONObject(response.body?.string() ?: return@withContext)
                    val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.JAPAN).apply {
                        timeZone = TimeZone.getTimeZone("Asia/Tokyo")
                    }

                    val holidays = json.keys().asSequence().mapNotNull { dateStr ->
                        val name = json.getString(dateStr)
                        val date = dateFormat.parse(dateStr) ?: return@mapNotNull null
                        val vEvent = VEvent().apply {
                            setSummary(name)
                            setDateStart(date)
                        }
                        ImportedEvent(event = vEvent, isHoliday = true)
                    }.toList()

                    dao.deleteHolidays()
                    dao.upsertImportedEvents(holidays)
                }
            } catch (e: Exception) {
                Log.e("CalYendar", "祝日の取得に失敗しました", e)
            }
        }
    }

    // インポートロジック (ViewModelから移動)
    suspend fun importIcs(uri: Uri, contentResolver: ContentResolver): Result<String> {
        return withContext(Dispatchers.IO) {
            runCatching {
                contentResolver.openInputStream(uri)?.use { inputStream ->
                    val ical = Biweekly.parse(inputStream).first()
                    if (ical == null) throw IOException("Failed to parse iCal")
                    
                    val events = ical.events.map { event ->
                        ImportedEvent(event = event, isHoliday = false)
                    }
                    dao.upsertImportedEvents(events)
                    "インポートに成功しました"
                } ?: throw IOException("Could not open input stream")
            }
        }
    }

    suspend fun importWebcal(url: String): Result<String> {
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
                        ImportedEvent(event = event, isHoliday = false)
                    }
                    dao.upsertImportedEvents(events)
                    "インポートに成功しました"
                }
            }
        }
    }
}
