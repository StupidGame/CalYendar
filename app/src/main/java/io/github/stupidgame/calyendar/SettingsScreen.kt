package io.github.stupidgame.calyendar

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.Switch
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.ui.Alignment
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.stupidgame.calyendar.data.CalendarViewModel

@Composable
fun SettingsScreen(
    calendarViewModel: CalendarViewModel,
    onImportIcsClick: (Boolean) -> Unit
) {
    var webCalUrl by remember { mutableStateOf("") }
    var importAsHoliday by remember { mutableStateOf(false) }
    var notificationOneDayBefore by remember { mutableStateOf(true) }
    var notificationOneHourBefore by remember { mutableStateOf(false) }

    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            Text("設定", style = MaterialTheme.typography.headlineMedium)
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Text("カレンダーのインポート", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                Switch(checked = importAsHoliday, onCheckedChange = { importAsHoliday = it })
                Text("休日としてインポート")
            }
            
            Button(
                onClick = { onImportIcsClick(importAsHoliday) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("iCalファイル (.ics) をインポート")
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            OutlinedTextField(
                value = webCalUrl,
                onValueChange = { webCalUrl = it },
                label = { Text("WebCalのURL") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = { calendarViewModel.importWebcal(webCalUrl, importAsHoliday) {} },
                modifier = Modifier.fillMaxWidth(),
                enabled = webCalUrl.isNotBlank()
            ) {
                Text("WebCalをインポート")
            }

            Spacer(modifier = Modifier.height(32.dp))

            Text("通知の設定 (デフォルト)", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("1日前に通知")
                Switch(checked = notificationOneDayBefore, onCheckedChange = { notificationOneDayBefore = it })
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("1時間前に通知")
                Switch(checked = notificationOneHourBefore, onCheckedChange = { notificationOneHourBefore = it })
            }
        }
    }
}
