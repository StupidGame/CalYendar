package io.github.stupidgame.calyendar.data

import android.content.ContentResolver
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

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
    private val settingsStore: AppSettingsStore,
    private val backupService: UserDataBackupService
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
            val result = backupService.exportCsv(uri, contentResolver, _uiState.value.toAppSettings())
            onResult(
                result.fold(
                    onSuccess = { it.toExportMessage() },
                    onFailure = { it.message ?: "Backup export failed." }
                )
            )
        }
    }

    fun importCsv(uri: Uri, contentResolver: ContentResolver, onResult: (String) -> Unit) {
        viewModelScope.launch {
            val result = backupService.importCsv(uri, contentResolver)
            onResult(
                result.fold(
                    onSuccess = { it.toImportMessage() },
                    onFailure = { it.message ?: "Backup import failed." }
                )
            )
        }
    }

    private fun persist(update: SettingsUiState.() -> SettingsUiState) {
        val newState = _uiState.value.update()
        _uiState.value = newState
        settingsStore.updateSettings(newState.toAppSettings())
    }
}

class SettingsViewModelFactory(
    private val settingsStore: AppSettingsStore,
    private val backupService: UserDataBackupService
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SettingsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SettingsViewModel(settingsStore, backupService) as T
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

private fun BackupSummary.toExportMessage(): String =
    "Backup exported: $events events, $transactions transactions, $goals goals."

private fun BackupSummary.toImportMessage(): String =
    "Backup imported: $events events, $transactions transactions, $goals goals."
