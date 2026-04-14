package io.github.stupidgame.calyendar.data

import android.content.ContentResolver
import android.net.Uri
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class BackupSummary(
    val events: Int,
    val transactions: Int,
    val goals: Int
)

class UserDataBackupService(
    private val repository: CalYendarRepository,
    private val settingsStore: AppSettingsStore,
    private val eventSyncService: EventSyncService
) {
    suspend fun exportCsv(
        uri: Uri,
        contentResolver: ContentResolver,
        settings: AppSettings
    ): Result<BackupSummary> =
        withContext(Dispatchers.IO) {
            runCatching {
                val backupData =
                    CsvBackupData(
                        settings = settings,
                        events = repository.getAllEventsSnapshot(),
                        transactions = repository.getAllTransactionsSnapshot(),
                        goals = repository.getAllGoalsSnapshot()
                    )

                contentResolver.openOutputStream(uri)?.use { outputStream ->
                    outputStream.bufferedWriter(Charsets.UTF_8).use { writer ->
                        writer.write(CsvBackupCodec.encode(backupData))
                    }
                } ?: throw IOException("Unable to open the destination backup file.")

                backupData.toSummary()
            }
        }

    suspend fun importCsv(
        uri: Uri,
        contentResolver: ContentResolver
    ): Result<BackupSummary> =
        withContext(Dispatchers.IO) {
            runCatching {
                val csvContent =
                    contentResolver.openInputStream(uri)?.use { inputStream ->
                        inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
                    } ?: throw IOException("Unable to open the selected backup file.")

                val backupData = CsvBackupCodec.decode(csvContent)
                val existingEvents = repository.getAllEventsSnapshot()

                eventSyncService.replaceScheduledEvents(existingEvents) {
                    repository.replaceUserData(
                        transactions = backupData.transactions,
                        events = backupData.events,
                        goals = backupData.goals
                    )
                    settingsStore.updateSettings(backupData.settings)
                    backupData.events
                }

                backupData.toSummary()
            }
        }

    private fun CsvBackupData.toSummary(): BackupSummary =
        BackupSummary(
            events = events.size,
            transactions = transactions.size,
            goals = goals.size
        )
}
