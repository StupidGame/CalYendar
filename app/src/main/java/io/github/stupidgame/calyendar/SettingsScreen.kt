package io.github.stupidgame.calyendar

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.stupidgame.calyendar.data.CalendarViewModel
import io.github.stupidgame.calyendar.data.SettingsViewModel
import java.time.LocalDate

@Composable
fun SettingsScreen(
    calendarViewModel: CalendarViewModel,
    settingsViewModel: SettingsViewModel
) {
    val context = LocalContext.current
    val uiState by settingsViewModel.uiState.collectAsState()
    val backupFileName =
        stringResource(R.string.settings_backup_file_name, LocalDate.now().toString())

    val importIcsLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            uri?.let {
                calendarViewModel.importIcs(it, context.contentResolver) { message ->
                    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                }
            }
        }
    val exportCsvLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
            uri?.let {
                settingsViewModel.exportCsv(it, context.contentResolver) { message ->
                    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                }
            }
        }
    val importCsvLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            uri?.let {
                settingsViewModel.importCsv(it, context.contentResolver) { message ->
                    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                }
            }
        }

    Scaffold { innerPadding ->
        Column(
            modifier =
                Modifier.fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
        ) {
            Text(stringResource(R.string.settings_title), style = MaterialTheme.typography.headlineMedium)

            Spacer(modifier = Modifier.height(24.dp))

            Text(stringResource(R.string.settings_calendar_import), style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = { importIcsLauncher.launch(arrayOf("text/calendar", "text/*")) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.settings_import_ics))
            }

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = uiState.webCalUrl,
                onValueChange = settingsViewModel::updateWebCalUrl,
                label = { Text(stringResource(R.string.settings_webcal_address)) },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = {
                    calendarViewModel.importWebcal(uiState.webCalUrl) { message ->
                        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = uiState.webCalUrl.isNotBlank()
            ) {
                Text(stringResource(R.string.settings_import_webcal))
            }

            Spacer(modifier = Modifier.height(32.dp))

            Text(stringResource(R.string.settings_default_notifications), style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(stringResource(R.string.settings_notify_one_day_before))
                Switch(
                    checked = uiState.notificationOneDayBefore,
                    onCheckedChange = settingsViewModel::updateNotificationOneDayBefore
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(stringResource(R.string.settings_notify_one_hour_before))
                Switch(
                    checked = uiState.notificationOneHourBefore,
                    onCheckedChange = settingsViewModel::updateNotificationOneHourBefore
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            Text(stringResource(R.string.settings_backup_title), style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                stringResource(R.string.settings_backup_description),
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                stringResource(R.string.settings_backup_warning),
                style = MaterialTheme.typography.bodySmall
            )
            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = { exportCsvLauncher.launch(backupFileName) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.settings_export_backup))
            }
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = { importCsvLauncher.launch(arrayOf("text/*", "application/*")) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.settings_import_backup))
            }
        }
    }
}
