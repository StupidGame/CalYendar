package io.github.stupidgame.CalYendar

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.stupidgame.CalYendar.data.Event
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEventDialog(
    event: Event?,
    year: Int,
    month: Int,
    day: Int,
    onDismiss: () -> Unit,
    onConfirm: (title: String, startDate: LocalDate, startTime: LocalTime, endDate: LocalDate, endTime: LocalTime, zoneId: ZoneId, notificationMinutes: Long) -> Unit
) {
    var title by remember { mutableStateOf(event?.title ?: "") }
    val initialStartDate = event?.let { Instant.ofEpochMilli(it.startTime).atZone(ZoneId.systemDefault()).toLocalDate() } ?: LocalDate.of(year, month + 1, day)
    val initialStartTime = event?.let { Instant.ofEpochMilli(it.startTime).atZone(ZoneId.systemDefault()).toLocalTime() } ?: LocalTime.now()
    val initialEndDate = event?.let { Instant.ofEpochMilli(it.endTime).atZone(ZoneId.systemDefault()).toLocalDate() } ?: LocalDate.of(year, month + 1, day)
    val initialEndTime = event?.let { Instant.ofEpochMilli(it.endTime).atZone(ZoneId.systemDefault()).toLocalTime() } ?: LocalTime.now().plusHours(1)

    var startDate by remember { mutableStateOf(initialStartDate) }
    var startTime by remember { mutableStateOf(initialStartTime) }
    var endDate by remember { mutableStateOf(initialEndDate) }
    var endTime by remember { mutableStateOf(initialEndTime) }
    var zoneId by remember { mutableStateOf(ZoneId.systemDefault()) }
    var zoneDropDownExpanded by remember { mutableStateOf(false) }
    val availableZoneIds = remember { ZoneId.getAvailableZoneIds().sorted() }

    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var editingStartDate by remember { mutableStateOf(false) }

    var notificationMinutes by remember { mutableStateOf(event?.notificationMinutesBefore ?: -1L) }
    val notificationOptions = remember {
        listOf(
            -1L to "なし",
            30L to "30分前",
            60L to "1時間前",
            1440L to "1日前",
            -2L to "カスタム"
        )
    }
    var notificationDropDownExpanded by remember { mutableStateOf(false) }
    var customNotificationValue by remember { mutableStateOf("1") }
    var customNotificationUnit by remember { mutableStateOf("分") }
    val customNotificationUnits = remember { listOf("分", "時間", "日") }
    var customUnitDropDownExpanded by remember { mutableStateOf(false) }


    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (event == null) "イベントを追加" else "イベントを編集") },
        text = {
            Column {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("タイトル") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(16.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                     Column(modifier = Modifier.weight(1f)) {
                        Text("開始日", style = MaterialTheme.typography.labelSmall)
                        OutlinedButton(
                            onClick = { showDatePicker = true; editingStartDate = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(startDate.format(DateTimeFormatter.ISO_LOCAL_DATE))
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("開始時刻", style = MaterialTheme.typography.labelSmall)
                        OutlinedButton(
                            onClick = { showTimePicker = true; editingStartDate = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(startTime.format(DateTimeFormatter.ofPattern("HH:mm")))
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                     Column(modifier = Modifier.weight(1f)) {
                        Text("終了日", style = MaterialTheme.typography.labelSmall)
                        OutlinedButton(
                            onClick = { showDatePicker = true; editingStartDate = false },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(endDate.format(DateTimeFormatter.ISO_LOCAL_DATE))
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("終了時刻", style = MaterialTheme.typography.labelSmall)
                        OutlinedButton(
                            onClick = { showTimePicker = true; editingStartDate = false },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(endTime.format(DateTimeFormatter.ofPattern("HH:mm")))
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                ExposedDropdownMenuBox(
                    expanded = zoneDropDownExpanded,
                    onExpandedChange = { zoneDropDownExpanded = !zoneDropDownExpanded }
                ) {
                    OutlinedTextField(
                        value = zoneId.id,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("タイムゾーン") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = zoneDropDownExpanded) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = zoneDropDownExpanded,
                        onDismissRequest = { zoneDropDownExpanded = false }
                    ) {
                        availableZoneIds.forEach { id ->
                            DropdownMenuItem(
                                text = { Text(id) },
                                onClick = {
                                    zoneId = ZoneId.of(id)
                                    zoneDropDownExpanded = false
                                }
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                ExposedDropdownMenuBox(
                    expanded = notificationDropDownExpanded,
                    onExpandedChange = { notificationDropDownExpanded = !notificationDropDownExpanded }
                ) {
                    OutlinedTextField(
                        value = notificationOptions.find { it.first == notificationMinutes }?.second ?: "カスタム",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("通知") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = notificationDropDownExpanded) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = notificationDropDownExpanded,
                        onDismissRequest = { notificationDropDownExpanded = false }
                    ) {
                        notificationOptions.forEach { (minutes, label) ->
                            DropdownMenuItem(
                                text = { Text(label) },
                                onClick = {
                                    notificationMinutes = minutes
                                    notificationDropDownExpanded = false
                                }
                            )
                        }
                    }
                }
                if (notificationMinutes == -2L) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = customNotificationValue,
                            onValueChange = { customNotificationValue = it },
                            label = { Text("通知時間") },
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        ExposedDropdownMenuBox(
                            expanded = customUnitDropDownExpanded,
                            onExpandedChange = { customUnitDropDownExpanded = !customUnitDropDownExpanded },
                            modifier = Modifier.weight(1f)
                        ) {
                            OutlinedTextField(
                                value = customNotificationUnit,
                                onValueChange = {},
                                readOnly = true,
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = customUnitDropDownExpanded) },
                                modifier = Modifier.menuAnchor()
                            )
                            ExposedDropdownMenu(
                                expanded = customUnitDropDownExpanded,
                                onDismissRequest = { customUnitDropDownExpanded = false }
                            ) {
                                customNotificationUnits.forEach { unit ->
                                    DropdownMenuItem(
                                        text = { Text(unit) },
                                        onClick = {
                                            customNotificationUnit = unit
                                            customUnitDropDownExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val finalNotificationMinutes = if (notificationMinutes == -2L) {
                        val value = customNotificationValue.toLongOrNull() ?: 0
                        when (customNotificationUnit) {
                            "分" -> value
                            "時間" -> value * 60
                            "日" -> value * 60 * 24
                            else -> -1L
                        }
                    } else {
                        notificationMinutes
                    }
                    onConfirm(title, startDate, startTime, endDate, endTime, zoneId, finalNotificationMinutes)
                },
                enabled = title.isNotBlank()
            ) {
                Text("保存")
            }
        },
        dismissButton = {
            Button(onClick = onDismiss) {
                Text("キャンセル")
            }
        }
    )

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = if (editingStartDate) startDate.atStartOfDay(zoneId).toInstant().toEpochMilli() else endDate.atStartOfDay(zoneId).toInstant().toEpochMilli())
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                Button(onClick = { 
                    showDatePicker = false
                    datePickerState.selectedDateMillis?.let {
                        val localDate = Instant.ofEpochMilli(it).atZone(zoneId).toLocalDate()
                        if (editingStartDate) {
                            startDate = localDate
                        } else {
                            endDate = localDate
                        }
                    }
                }) {
                    Text("OK")
                }
            },
            dismissButton = {
                Button(onClick = { showDatePicker = false }) {
                    Text("キャンセル")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showTimePicker) {
        val timePickerState = rememberTimePickerState(initialHour = if (editingStartDate) startTime.hour else endTime.hour, initialMinute = if (editingStartDate) startTime.minute else endTime.minute, is24Hour = true)
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            title = { Text(if (editingStartDate) "開始時刻" else "終了時刻") },
            text = { TimePicker(state = timePickerState) },
            confirmButton = {
                Button(onClick = { 
                    showTimePicker = false
                    val localTime = LocalTime.of(timePickerState.hour, timePickerState.minute)
                    if (editingStartDate) {
                        startTime = localTime
                    } else {
                        endTime = localTime
                    }
                }) {
                    Text("OK")
                }
            },
            dismissButton = {
                Button(onClick = { showTimePicker = false }) {
                    Text("キャンセル")
                }
            }
        )
    }
}
