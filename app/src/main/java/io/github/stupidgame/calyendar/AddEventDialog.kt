package io.github.stupidgame.calyendar

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.stupidgame.calyendar.data.Event
import io.github.stupidgame.calyendar.data.EventRepeatType
import io.github.stupidgame.calyendar.data.normalizedNotificationLeadTimes
import io.github.stupidgame.calyendar.data.notificationLeadTimes
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

private val standardNotificationOptions =
    listOf(
        30L to "30分前",
        60L to "1時間前",
        1440L to "1日前"
    )

private val customNotificationUnits = listOf("分", "時間", "日")
private val daysOfWeek = listOf("月", "火", "水", "木", "金", "土", "日")
private val timeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

fun formatNotificationLabel(minutes: Long): String {
    return when (minutes) {
        -1L -> "なし"
        30L -> "30分前"
        60L -> "1時間前"
        1440L -> "1日前"
        else -> {
            if (minutes > 0 && minutes % (24 * 60) == 0L) "${minutes / (24 * 60)}日前"
            else if (minutes > 0 && minutes % 60 == 0L) "${minutes / 60}時間前"
            else "${minutes}分前"
        }
    }
}

private fun ZoneId.toJapaneseLabel(): String {
    val displayName = getDisplayName(TextStyle.FULL, Locale.JAPAN)
    val offset = rules.getOffset(Instant.now()).id.replace("Z", "+00:00")
    return "$displayName（時差 $offset）"
}

private fun customNotificationMinutes(valueText: String, unit: String): Long {
    val value = valueText.toLongOrNull() ?: return 0L
    return when (unit) {
        "分" -> value
        "時間" -> value * 60
        "日" -> value * 60 * 24
        else -> 0L
    }
}

private fun List<Long>.withNotificationLeadTime(minutes: Long): List<Long> {
    return if (minutes > 0 && minutes !in this) {
        (this + minutes).normalizedNotificationLeadTimes()
    } else {
        this
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddEventDialog(
    event: Event?,
    year: Int,
    month: Int,
    day: Int,
    defaultNotifications: List<Long> = emptyList(),
    onDismiss: () -> Unit,
    onConfirm: (
        title: String,
        startDate: LocalDate,
        startTime: LocalTime,
        endDate: LocalDate,
        endTime: LocalTime,
        zoneId: ZoneId,
        notifications: List<Long>,
        isHoliday: Boolean,
        repeatType: EventRepeatType,
        repeatUntil: LocalDate?,
        repeatDays: Set<Int>
    ) -> Unit
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
    val availableZoneIds = remember {
        ZoneId.getAvailableZoneIds().map(ZoneId::of).sortedBy { it.toJapaneseLabel() }
    }

    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var editingStartDate by remember { mutableStateOf(false) }

    // Notifications state
    var notifications: List<Long> by remember {
        mutableStateOf(
            event?.notificationLeadTimes() ?: defaultNotifications.normalizedNotificationLeadTimes()
        )
    }

    var notificationDropDownExpanded by remember { mutableStateOf(false) }
    var selectedStandardNotification by remember { mutableStateOf<Long?>(null) }
    var customNotificationValue by remember { mutableStateOf("1") }
    var customNotificationUnit by remember { mutableStateOf("分") }
    var customUnitDropDownExpanded by remember { mutableStateOf(false) }

    var isHoliday by remember { mutableStateOf(event?.isHoliday ?: false) }

    // Repeat state
    var repeatType by remember { mutableStateOf(EventRepeatType.NONE) }
    val repeatOptions = remember { EventRepeatType.entries }
    var repeatDropDownExpanded by remember { mutableStateOf(false) }

    var repeatUntil by remember { mutableStateOf(startDate.plusYears(1)) }
    var showRepeatUntilPicker by remember { mutableStateOf(false) }

    var selectedDays by remember { mutableStateOf(setOf<Int>()) }

    val scrollState = rememberScrollState()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (event == null) "イベントを追加" else "イベントを編集") },
        text = {
            Column(modifier = Modifier.verticalScroll(scrollState)) {
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
                            Text(startTime.format(timeFormatter))
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
                            Text(endTime.format(timeFormatter))
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                ExposedDropdownMenuBox(
                    expanded = zoneDropDownExpanded,
                    onExpandedChange = { zoneDropDownExpanded = !zoneDropDownExpanded }
                ) {
                    OutlinedTextField(
                        value = zoneId.toJapaneseLabel(),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("タイムゾーン") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = zoneDropDownExpanded) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )
                    DropdownMenu(
                        expanded = zoneDropDownExpanded,
                        onDismissRequest = { zoneDropDownExpanded = false }
                    ) {
                        availableZoneIds.forEach { availableZoneId ->
                            DropdownMenuItem(
                                text = { Text(availableZoneId.toJapaneseLabel()) },
                                onClick = {
                                    zoneId = availableZoneId
                                    zoneDropDownExpanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                Text("通知", style = MaterialTheme.typography.titleMedium)
                if (notifications.isNotEmpty()) {
                    FlowRow(modifier = Modifier.fillMaxWidth()) {
                        notifications.forEach { minutes ->
                            AssistChip(
                                onClick = { notifications = notifications.filter { it != minutes } },
                                label = { Text(formatNotificationLabel(minutes)) },
                                trailingIcon = { Icon(Icons.Default.Close, contentDescription = "削除", modifier = Modifier.size(16.dp)) },
                                modifier = Modifier.padding(end = 4.dp)
                            )
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    ExposedDropdownMenuBox(
                        expanded = notificationDropDownExpanded,
                        onExpandedChange = { notificationDropDownExpanded = !notificationDropDownExpanded },
                        modifier = Modifier.weight(1f)
                    ) {
                        OutlinedTextField(
                            value = selectedStandardNotification?.let { formatNotificationLabel(it) } ?: "カスタム",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("通知を追加") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = notificationDropDownExpanded) },
                            modifier = Modifier.menuAnchor()
                        )
                        DropdownMenu(
                            expanded = notificationDropDownExpanded,
                            onDismissRequest = { notificationDropDownExpanded = false }
                        ) {
                            standardNotificationOptions.forEach { (minutes, label) ->
                                DropdownMenuItem(
                                    text = { Text(label) },
                                    onClick = {
                                        selectedStandardNotification = minutes
                                        notificationDropDownExpanded = false
                                    }
                                )
                            }
                            DropdownMenuItem(
                                text = { Text("カスタム") },
                                onClick = {
                                    selectedStandardNotification = null
                                    notificationDropDownExpanded = false
                                }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(onClick = {
                        val minutesToAdd =
                            selectedStandardNotification
                                ?: customNotificationMinutes(
                                    valueText = customNotificationValue,
                                    unit = customNotificationUnit
                                )
                        notifications = notifications.withNotificationLeadTime(minutesToAdd)
                    }) {
                        Text("追加")
                    }
                }

                if (selectedStandardNotification == null) {
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
                            DropdownMenu(
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

                Spacer(modifier = Modifier.height(16.dp))
                Text("繰り返し", style = MaterialTheme.typography.titleMedium)
                ExposedDropdownMenuBox(
                    expanded = repeatDropDownExpanded,
                    onExpandedChange = { repeatDropDownExpanded = !repeatDropDownExpanded }
                ) {
                    OutlinedTextField(
                        value = repeatType.label,
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = repeatDropDownExpanded) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )
                    DropdownMenu(
                        expanded = repeatDropDownExpanded,
                        onDismissRequest = { repeatDropDownExpanded = false }
                    ) {
                        repeatOptions.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option.label) },
                                onClick = {
                                    repeatType = option
                                    repeatDropDownExpanded = false
                                }
                            )
                        }
                    }
                }

                if (repeatType == EventRepeatType.WEEKDAY_SELECTION) {
                    Spacer(modifier = Modifier.height(8.dp))
                    FlowRow {
                        daysOfWeek.forEachIndexed { index, dayName ->
                            val dayInt = index + 1
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable {
                                selectedDays = if (selectedDays.contains(dayInt)) selectedDays - dayInt else selectedDays + dayInt
                            }) {
                                Checkbox(checked = selectedDays.contains(dayInt), onCheckedChange = {
                                    selectedDays = if (it) selectedDays + dayInt else selectedDays - dayInt
                                })
                                Text(dayName, modifier = Modifier.padding(end = 8.dp))
                            }
                        }
                    }
                }

                if (repeatType != EventRepeatType.NONE) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("繰り返し終了日", style = MaterialTheme.typography.labelSmall)
                    OutlinedButton(
                        onClick = { showRepeatUntilPicker = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(repeatUntil.format(DateTimeFormatter.ISO_LOCAL_DATE))
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { isHoliday = !isHoliday }) {
                    Checkbox(checked = isHoliday, onCheckedChange = { isHoliday = it })
                    Text("休日として作成")
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(title, startDate, startTime, endDate, endTime, zoneId, notifications, isHoliday, repeatType, repeatUntil, selectedDays)
                },
                enabled =
                    title.isNotBlank() &&
                        (repeatType != EventRepeatType.WEEKDAY_SELECTION ||
                            selectedDays.isNotEmpty())
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
                    Text(stringResource(R.string.action_confirm))
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

    if (showRepeatUntilPicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = repeatUntil.atStartOfDay(zoneId).toInstant().toEpochMilli())
        DatePickerDialog(
            onDismissRequest = { showRepeatUntilPicker = false },
            confirmButton = {
                Button(onClick = {
                    showRepeatUntilPicker = false
                    datePickerState.selectedDateMillis?.let {
                        repeatUntil = Instant.ofEpochMilli(it).atZone(zoneId).toLocalDate()
                    }
                }) {
                    Text(stringResource(R.string.action_confirm))
                }
            },
            dismissButton = {
                Button(onClick = { showRepeatUntilPicker = false }) {
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
                    Text(stringResource(R.string.action_confirm))
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
