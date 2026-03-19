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
            Text("Settings", style = MaterialTheme.typography.headlineMedium)

            Spacer(modifier = Modifier.height(24.dp))

            Text("Calendar import", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = { importIcsLauncher.launch(arrayOf("text/calendar", "text/*")) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Import iCal (.ics)")
            }

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = uiState.webCalUrl,
                onValueChange = settingsViewModel::updateWebCalUrl,
                label = { Text("WebCal URL") },
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
                Text("Import WebCal")
            }

            Spacer(modifier = Modifier.height(32.dp))

            Text("Default notifications", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("1 day before")
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
                Text("1 hour before")
                Switch(
                    checked = uiState.notificationOneHourBefore,
                    onCheckedChange = settingsViewModel::updateNotificationOneHourBefore
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            Text("CSV backup", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Export and import settings, events, transactions, and goals in one CSV file.",
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Import replaces local events, transactions, and goals. Imported calendar feeds stay as-is.",
                style = MaterialTheme.typography.bodySmall
            )
            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = {
                    exportCsvLauncher.launch("calyendar-backup-${LocalDate.now()}.csv")
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Export CSV")
            }
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = { importCsvLauncher.launch(arrayOf("text/*", "application/*")) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Import CSV")
            }
        }
    }
}
