package io.github.stupidgame.calyendar.data

import android.content.ContentResolver
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import io.github.stupidgame.calyendar.utils.EventNotificationManager
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class SettingsUiState(
    val webCalUrl: String = "",
    val notificationOneDayBefore: Boolean = true,
    val notificationOneHourBefore: Boolean = false
) {
    fun toAppSettings(): AppSettings {
        return AppSettings(
            webCalUrl = webCalUrl,
            notificationOneDayBefore = notificationOneDayBefore,
            notificationOneHourBefore = notificationOneHourBefore
        )
    }
}

class SettingsViewModel(
    private val repository: CalYendarRepository,
    private val settingsStore: AppSettingsStore,
    private val notificationManager: EventNotificationManager
) : ViewModel() {
    private val _uiState = MutableStateFlow(settingsStore.getSettings().toUiState())
    val uiState = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            settingsStore.settingsFlow.collect { settings ->
                _uiState.value = settings.toUiState()
            }
        }
    }

    fun updateWebCalUrl(value: String) {
        persist { copy(webCalUrl = value) }
    }

    fun updateNotificationOneDayBefore(enabled: Boolean) {
        persist { copy(notificationOneDayBefore = enabled) }
    }

    fun updateNotificationOneHourBefore(enabled: Boolean) {
        persist { copy(notificationOneHourBefore = enabled) }
    }

    fun exportCsv(uri: Uri, contentResolver: ContentResolver, onResult: (String) -> Unit) {
        viewModelScope.launch {
            val result =
                withContext(Dispatchers.IO) {
                    runCatching {
                        val backupData =
                            CsvBackupData(
                                settings = _uiState.value.toAppSettings(),
                                events = repository.getAllEventsSnapshot(),
                                transactions = repository.getAllTransactionsSnapshot(),
                                goals = repository.getAllGoalsSnapshot()
                            )

                        contentResolver.openOutputStream(uri)?.use { outputStream ->
                            outputStream.bufferedWriter(Charsets.UTF_8).use { writer ->
                                writer.write(CsvBackupCodec.encode(backupData))
                            }
                        } ?: throw IOException("書き出し先を開けませんでした。")

                        "バックアップを書き出しました。イベント${backupData.events.size}件、取引${backupData.transactions.size}件、目標${backupData.goals.size}件です。"
                    }
                }

            onResult(result.getOrDefault(result.exceptionOrNull()?.message ?: "不明なエラーが発生しました。"))
        }
    }

    fun importCsv(uri: Uri, contentResolver: ContentResolver, onResult: (String) -> Unit) {
        viewModelScope.launch {
            val result =
                withContext(Dispatchers.IO) {
                    runCatching {
                        val csvContent =
                            contentResolver.openInputStream(uri)?.use { inputStream ->
                                inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
                            } ?: throw IOException("バックアップファイルを開けませんでした。")

                        val backupData = CsvBackupCodec.decode(csvContent)
                        val existingEvents = repository.getAllEventsSnapshot()

                        try {
                            existingEvents.forEach(notificationManager::cancelEventNotification)
                            repository.replaceUserData(
                                transactions = backupData.transactions,
                                events = backupData.events,
                                goals = backupData.goals
                            )
                            settingsStore.updateSettings(backupData.settings)
                            backupData.events.forEach(notificationManager::scheduleEventNotification)
                        } catch (exception: Exception) {
                            existingEvents.forEach(notificationManager::scheduleEventNotification)
                            throw exception
                        }

                        "バックアップを読み込みました。イベント${backupData.events.size}件、取引${backupData.transactions.size}件、目標${backupData.goals.size}件です。"
                    }
                }

            onResult(result.getOrDefault(result.exceptionOrNull()?.message ?: "不明なエラーが発生しました。"))
        }
    }

    private fun persist(update: SettingsUiState.() -> SettingsUiState) {
        val newState = _uiState.value.update()
        _uiState.value = newState
        settingsStore.updateSettings(newState.toAppSettings())
    }
}

class SettingsViewModelFactory(
    private val repository: CalYendarRepository,
    private val settingsStore: AppSettingsStore,
    private val notificationManager: EventNotificationManager
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SettingsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SettingsViewModel(repository, settingsStore, notificationManager) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

private fun AppSettings.toUiState(): SettingsUiState {
    return SettingsUiState(
        webCalUrl = webCalUrl,
        notificationOneDayBefore = notificationOneDayBefore,
        notificationOneHourBefore = notificationOneHourBefore
    )
}
