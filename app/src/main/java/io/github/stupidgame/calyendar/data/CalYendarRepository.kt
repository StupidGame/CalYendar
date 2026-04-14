package io.github.stupidgame.calyendar.data

import android.content.ContentResolver
import android.net.Uri
import android.util.Log
import androidx.room.withTransaction
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

class CalYendarRepository(private val database: CalYendarDatabase) {
    private val dao = database.calyendarDao()

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

    suspend fun getAllTransactionsSnapshot(): List<Transaction> =
        withContext(Dispatchers.IO) { dao.getAllTransactionsSnapshot() }

    suspend fun upsertTransaction(transaction: Transaction) = dao.upsertTransaction(transaction)

    suspend fun deleteTransaction(transaction: Transaction) = dao.deleteTransaction(transaction)

    fun getAllGoals(): Flow<List<FinancialGoal>> = dao.getAllGoals()

    suspend fun getAllGoalsSnapshot(): List<FinancialGoal> =
        withContext(Dispatchers.IO) { dao.getAllGoalsSnapshot() }

    suspend fun upsertFinancialGoal(goal: FinancialGoal) = dao.upsertFinancialGoal(goal)

    suspend fun deleteFinancialGoal(goal: FinancialGoal) = dao.deleteFinancialGoal(goal)

    fun getEventsForDate(year: Int, month: Int, day: Int): Flow<List<Event>> =
        dao.getEventsForDate(year, month, day)

    fun getEventsForMonth(year: Int, month: Int): Flow<List<Event>> =
        dao.getEventsForMonth(year, month)

    suspend fun getAllEventsSnapshot(): List<Event> =
        withContext(Dispatchers.IO) { dao.getAllEventsSnapshot() }

    suspend fun getEventById(id: Long): Event? =
        withContext(Dispatchers.IO) { dao.getEventById(id) }

    suspend fun upsertEvent(event: Event): Long = dao.upsertEvent(event)

    suspend fun deleteEvent(event: Event) = dao.deleteEvent(event)

    suspend fun replaceUserData(
        transactions: List<Transaction>,
        events: List<Event>,
        goals: List<FinancialGoal>
    ) {
        withContext(Dispatchers.IO) {
            database.withTransaction {
                dao.clearTransactions()
                dao.clearFinancialGoals()
                dao.clearEvents()

                if (transactions.isNotEmpty()) {
                    dao.upsertTransactions(transactions)
                }
                if (goals.isNotEmpty()) {
                    dao.upsertFinancialGoals(goals)
                }
                if (events.isNotEmpty()) {
                    dao.upsertEvents(events)
                }
            }
        }
    }

    fun getImportedEvents(): Flow<List<ImportedEvent>> = dao.getImportedEvents()

    suspend fun getImportedEventsSnapshot(): List<ImportedEvent> =
        withContext(Dispatchers.IO) { dao.getImportedEventsSnapshot() }

    suspend fun upsertImportedEvents(events: List<ImportedEvent>) = dao.upsertImportedEvents(events)

    suspend fun deleteImportedEvent(event: ImportedEvent) = dao.deleteImportedEvent(event)

    suspend fun clearImportedEvents() = dao.clearImportedEvents()

    suspend fun fetchJapaneseHolidays() {
        withContext(Dispatchers.IO) {
            try {
                val client = OkHttpClient()
                val request = Request.Builder().url("https://holidays-jp.github.io/api/v1/date.json").build()

                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) return@withContext

                    val json = JSONObject(response.body?.string() ?: return@withContext)
                    val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.JAPAN).apply {
                        timeZone = TimeZone.getTimeZone("Asia/Tokyo")
                    }

                    val holidays =
                        json.keys().asSequence().mapNotNull { dateStr ->
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
            } catch (exception: Exception) {
                Log.e("CalYendar", "Failed to fetch holidays", exception)
            }
        }
    }

    suspend fun importIcs(uri: Uri, contentResolver: ContentResolver): Result<String> {
        return withContext(Dispatchers.IO) {
            runCatching {
                contentResolver.openInputStream(uri)?.use { inputStream ->
                    val ical = Biweekly.parse(inputStream).first()
                    if (ical == null) throw IOException("カレンダーファイルを解析できませんでした。")

                    val events = ical.events.map { event ->
                        ImportedEvent(event = event, isHoliday = false)
                    }
                    replaceMatchingImportedEvents(events)
                    "カレンダーを読み込みました。"
                } ?: throw IOException("ファイルを開けませんでした。")
            }
        }
    }

    suspend fun importWebcal(url: String): Result<String> {
        return withContext(Dispatchers.IO) {
            runCatching {
                val client = OkHttpClient()
                val request = Request.Builder().url(url.replace("webcal", "https")).build()

                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        throw IOException("サーバーから予期しない応答が返されました。(${response.code})")
                    }

                    val inputStream = response.body?.byteStream()
                        ?: throw IOException("サーバーの応答が空でした。")
                    val ical = Biweekly.parse(inputStream).first()
                    if (ical == null) throw IOException("カレンダー情報を解析できませんでした。")

                    val events = ical.events.map { event ->
                        ImportedEvent(event = event, isHoliday = false)
                    }
                    replaceMatchingImportedEvents(events)
                    "カレンダーを読み込みました。"
                }
            }
        }
    }

    private suspend fun replaceMatchingImportedEvents(incomingEvents: List<ImportedEvent>) {
        val deduplicatedIncomingEvents = incomingEvents.distinctBy(ImportedEvent::identityKey)
        val existingEventsToReplace =
            importedEventsToReplace(dao.getImportedEventsSnapshot(), deduplicatedIncomingEvents)

        if (existingEventsToReplace.isNotEmpty()) {
            dao.deleteImportedEvents(existingEventsToReplace)
        }
        if (deduplicatedIncomingEvents.isNotEmpty()) {
            dao.upsertImportedEvents(deduplicatedIncomingEvents)
        }
    }
}
