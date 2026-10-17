package io.github.stupidgame.calyendar

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.stupidgame.calyendar.data.CalendarViewModel
import io.github.stupidgame.calyendar.data.SettingsViewModel
import java.time.LocalDate

@Composable
fun SettingsScreen(calendarViewModel: CalendarViewModel, settingsViewModel: SettingsViewModel) {
    val context = LocalContext.current
    val uiState by settingsViewModel.uiState.collectAsState()
    val backupFileName = stringResource(R.string.settings_backup_file_name, LocalDate.now().toString())
    var pendingBackupImport by remember { mutableStateOf<Uri?>(null) }

    val importIcs = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let {
            calendarViewModel.importIcs(it, context.contentResolver) { message ->
                Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            }
        }
    }
    val exportBackup = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
        uri?.let {
            settingsViewModel.exportCsv(it, context.contentResolver) { message ->
                Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            }
        }
    }
    val selectBackup = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        pendingBackupImport = uri
    }

    pendingBackupImport?.let { uri ->
        AlertDialog(
            onDismissRequest = { pendingBackupImport = null },
            title = { Text("バックアップを読み込みますか？") },
            text = { Text(stringResource(R.string.settings_backup_warning)) },
            confirmButton = {
                TextButton(onClick = {
                    pendingBackupImport = null
                    settingsViewModel.importCsv(uri, context.contentResolver) { message ->
                        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                    }
                }) { Text("読み込む") }
            },
            dismissButton = { TextButton(onClick = { pendingBackupImport = null }) { Text("キャンセル") } }
        )
    }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Text("設定", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)

        SettingsSection(title = "外部カレンダー", icon = { Icon(Icons.Outlined.CalendarMonth, null) }) {
            Text(".ics ファイルや購読カレンダーを予定に追加できます。", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            OutlinedButton(
                onClick = { importIcs.launch(arrayOf("text/calendar", "text/*")) },
                modifier = Modifier.fillMaxWidth()
            ) { Text(stringResource(R.string.settings_import_ics)) }
            OutlinedTextField(
                value = uiState.webCalUrl,
                onValueChange = settingsViewModel::updateWebCalUrl,
                label = { Text(stringResource(R.string.settings_webcal_address)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Button(
                onClick = {
                    calendarViewModel.importWebcal(uiState.webCalUrl) { message ->
                        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                    }
                },
                enabled = uiState.webCalUrl.isNotBlank(),
                modifier = Modifier.fillMaxWidth()
            ) { Text(stringResource(R.string.settings_import_webcal)) }
        }

        SettingsSection(title = "既定の通知", icon = { Icon(Icons.Outlined.Notifications, null) }) {
            SettingsSwitchRow(
                label = "予定の1日前",
                checked = uiState.notificationOneDayBefore,
                onCheckedChange = settingsViewModel::updateNotificationOneDayBefore
            )
            SettingsSwitchRow(
                label = "予定の1時間前",
                checked = uiState.notificationOneHourBefore,
                onCheckedChange = settingsViewModel::updateNotificationOneHourBefore
            )
        }

        SettingsSection(title = "バックアップ", icon = { Icon(Icons.Outlined.Save, null) }) {
            Text(stringResource(R.string.settings_backup_description), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(stringResource(R.string.settings_backup_warning), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            OutlinedButton(onClick = { exportBackup.launch(backupFileName) }, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.settings_export_backup))
            }
            OutlinedButton(
                onClick = { selectBackup.launch(arrayOf("text/*", "application/*")) },
                modifier = Modifier.fillMaxWidth()
            ) { Text(stringResource(R.string.settings_import_backup)) }
        }
    }
}

@Composable
private fun SettingsSection(title: String, icon: @Composable () -> Unit, content: @Composable () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                icon()
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            content()
        }
    }
}

@Composable
private fun SettingsSwitchRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.bodyLarge)
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
