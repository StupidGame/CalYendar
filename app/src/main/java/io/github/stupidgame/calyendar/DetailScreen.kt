package io.github.stupidgame.calyendar

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.stupidgame.calyendar.data.DetailUiState
import io.github.stupidgame.calyendar.data.DetailViewModel
import io.github.stupidgame.calyendar.data.DetailViewModelFactory
import io.github.stupidgame.calyendar.data.Event
import io.github.stupidgame.calyendar.data.EventRepeatType
import io.github.stupidgame.calyendar.data.FinancialGoal
import io.github.stupidgame.calyendar.data.ImportedEvent
import io.github.stupidgame.calyendar.data.Transaction
import io.github.stupidgame.calyendar.data.TransactionType
import io.github.stupidgame.calyendar.ui.components.EventCard
import io.github.stupidgame.calyendar.ui.components.IcalEventCard
import io.github.stupidgame.calyendar.ui.components.SummaryCard
import io.github.stupidgame.calyendar.ui.components.TransactionCard
import io.github.stupidgame.calyendar.utils.EventNotificationManager
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

private sealed interface DetailDialogState {
    data object AddGoal : DetailDialogState
    data object AddIncome : DetailDialogState
    data object AddExpense : DetailDialogState
    data object AddEvent : DetailDialogState
    data class EditGoal(val goal: FinancialGoal) : DetailDialogState
    data class EditTransaction(val transaction: Transaction) : DetailDialogState
    data class EditEvent(val event: Event) : DetailDialogState
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun DetailScreen(
    year: Int,
    month: Int,
    day: Int,
    viewModel: DetailViewModel,
    defaultNotifications: List<Long> = emptyList()
) {
    val uiState by viewModel.uiState.collectAsState(initial = DetailUiState())

    var showActionSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState()
    var activeDialog by remember { mutableStateOf<DetailDialogState?>(null) }
    var deleteTarget by remember { mutableStateOf<Any?>(null) }

    val saveEvent: (
        Event?,
        String,
        LocalDate,
        LocalTime,
        LocalDate,
        LocalTime,
        ZoneId,
        List<Long>,
        Boolean,
        EventRepeatType,
        LocalDate?,
        Set<Int>
    ) -> Unit = { existingEvent,
                   title,
                   startDate,
                   startTime,
                   endDate,
                   endTime,
                   zoneId,
                   notifications,
                   isHoliday,
                   repeatType,
                   repeatUntil,
                   repeatDays ->
        val startMillis = startDate.atTime(startTime).atZone(zoneId).toInstant().toEpochMilli()
        val endMillis = endDate.atTime(endTime).atZone(zoneId).toInstant().toEpochMilli()
        val eventToSave =
            (existingEvent
                    ?: Event(
                        year = startDate.year,
                        month = startDate.monthValue - 1,
                        day = startDate.dayOfMonth,
                        title = title,
                        startTime = startMillis,
                        endTime = endMillis,
                        notificationMinutesBefore = -1L
                    ))
                .copy(
                    year = startDate.year,
                    month = startDate.monthValue - 1,
                    day = startDate.dayOfMonth,
                    title = title,
                    startTime = startMillis,
                    endTime = endMillis,
                    notificationMinutesBefore = -1L,
                    notifications = notifications.joinToString(","),
                    isHoliday = isHoliday
                )

        viewModel.upsertEventWithRepeat(
            event = eventToSave,
            repeatType = repeatType,
            repeatUntil = repeatUntil,
            repeatDays = repeatDays
        )
        activeDialog = null
    }

    if (deleteTarget != null) {
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("削除の確認") },
            text = { Text("この項目を削除してもよろしいですか？") },
            confirmButton = {
                TextButton(
                    onClick = {
                        when (val item = deleteTarget) {
                            is Transaction -> viewModel.deleteTransaction(item)
                            is FinancialGoal -> viewModel.deleteFinancialGoal(item)
                            is Event -> viewModel.deleteEvent(item)
                            is ImportedEvent -> viewModel.deleteImportedEvent(item)
                        }
                        deleteTarget = null
                    }
                ) { Text("削除") }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null }) { Text("キャンセル") }
            }
        )
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { showActionSheet = true }) {
                Icon(Icons.Filled.Add, contentDescription = "追加")
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding =
                PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = innerPadding.calculateTopPadding() + 16.dp,
                    bottom = innerPadding.calculateBottomPadding() + 88.dp
                ),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text(
                    text = "$year/${month + 1}/$day",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(16.dp))
                SummaryCard(
                    displayBalance = uiState.summaryBalance,
                    goal = uiState.goal,
                    totalGoalCost = uiState.totalGoalCost,
                    onLongClick = { uiState.goal?.let { deleteTarget = it } },
                    onClick = { uiState.goal?.let { activeDialog = DetailDialogState.EditGoal(it) } }
                )
            }

            val holidays = uiState.events.filter(Event::isHoliday) + uiState.icalEvents.filter(ImportedEvent::isHoliday)
            val regularEvents = uiState.events.filterNot(Event::isHoliday)
            val regularIcalEvents = uiState.icalEvents.filterNot(ImportedEvent::isHoliday)

            if (holidays.isNotEmpty()) {
                item { Text("祝日", style = MaterialTheme.typography.titleLarge) }
                items(holidays) {
                    when (it) {
                        is Event ->
                            EventCard(event = it, onLongClick = { deleteTarget = it }) {
                                activeDialog = DetailDialogState.EditEvent(it)
                            }
                        is ImportedEvent ->
                            IcalEventCard(event = it, onLongClick = { deleteTarget = it })
                    }
                }
            }

            if (regularEvents.isNotEmpty()) {
                item { Text("イベント", style = MaterialTheme.typography.titleLarge) }
                items(regularEvents) { event ->
                    EventCard(event = event, onLongClick = { deleteTarget = event }) {
                        activeDialog = DetailDialogState.EditEvent(event)
                    }
                }
            }

            if (regularIcalEvents.isNotEmpty()) {
                item { Text("インポートしたイベント", style = MaterialTheme.typography.titleLarge) }
                items(regularIcalEvents) { event ->
                    IcalEventCard(event = event, onLongClick = { deleteTarget = event })
                }
            }

            if (uiState.dailyTransactions.isNotEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("取引", style = MaterialTheme.typography.titleLarge)
                }
                items(uiState.dailyTransactions) { transaction ->
                    TransactionCard(
                        transaction = transaction,
                        onLongClick = { deleteTarget = transaction }
                    ) {
                        activeDialog = DetailDialogState.EditTransaction(transaction)
                    }
                }
            }
        }

        if (showActionSheet) {
            ModalBottomSheet(
                onDismissRequest = { showActionSheet = false },
                sheetState = sheetState
            ) {
                Column(modifier = Modifier.padding(bottom = 32.dp)) {
                    ListItem(
                        headlineContent = { Text("目標を追加") },
                        leadingContent = { Icon(Icons.Filled.Add, contentDescription = null) },
                        modifier =
                            Modifier.clickable {
                                activeDialog = DetailDialogState.AddGoal
                                showActionSheet = false
                            }
                    )
                    ListItem(
                        headlineContent = { Text("収入を追加") },
                        leadingContent = {
                            Icon(Icons.Filled.TrendingUp, contentDescription = null)
                        },
                        modifier =
                            Modifier.clickable {
                                activeDialog = DetailDialogState.AddIncome
                                showActionSheet = false
                            }
                    )
                    ListItem(
                        headlineContent = { Text("支出を追加") },
                        leadingContent = {
                            Icon(Icons.Filled.TrendingDown, contentDescription = null)
                        },
                        modifier =
                            Modifier.clickable {
                                activeDialog = DetailDialogState.AddExpense
                                showActionSheet = false
                            }
                    )
                    ListItem(
                        headlineContent = { Text("イベントを追加") },
                        leadingContent = {
                            Icon(Icons.Filled.Event, contentDescription = null)
                        },
                        modifier =
                            Modifier.clickable {
                                activeDialog = DetailDialogState.AddEvent
                                showActionSheet = false
                            }
                    )
                    ListItem(
                        headlineContent = { Text("インポートしたイベントを削除") },
                        leadingContent = {
                            Icon(Icons.Filled.Delete, contentDescription = null)
                        },
                        modifier =
                            Modifier.clickable {
                                viewModel.clearImportedEvents()
                                showActionSheet = false
                            }
                    )
                }
            }
        }

        when (val dialog = activeDialog) {
            DetailDialogState.AddGoal ->
                AddGoalDialog(
                    goal = null,
                    onDismiss = { activeDialog = null },
                    onConfirm = { name: String, amount: Long ->
                        viewModel.upsertFinancialGoal(
                            FinancialGoal(
                                id = 0,
                                year = viewModel.year,
                                month = viewModel.month,
                                day = viewModel.day,
                                name = name,
                                amount = amount
                            )
                        )
                        activeDialog = null
                    }
                )

            DetailDialogState.AddIncome ->
                AddTransactionDialog(
                    transaction = null,
                    type = TransactionType.INCOME,
                    onDismiss = { activeDialog = null },
                    onConfirm = { name: String, amount: Long ->
                        viewModel.upsertTransaction(
                            Transaction(
                                year = viewModel.year,
                                month = viewModel.month,
                                day = viewModel.day,
                                type = TransactionType.INCOME,
                                name = name,
                                amount = amount
                            )
                        )
                        activeDialog = null
                    }
                )

            DetailDialogState.AddExpense ->
                AddTransactionDialog(
                    transaction = null,
                    type = TransactionType.EXPENSE,
                    onDismiss = { activeDialog = null },
                    onConfirm = { name: String, amount: Long ->
                        viewModel.upsertTransaction(
                            Transaction(
                                year = viewModel.year,
                                month = viewModel.month,
                                day = viewModel.day,
                                type = TransactionType.EXPENSE,
                                name = name,
                                amount = amount
                            )
                        )
                        activeDialog = null
                    }
                )

            DetailDialogState.AddEvent ->
                AddEventDialog(
                    event = null,
                    year = viewModel.year,
                    month = viewModel.month,
                    day = viewModel.day,
                    defaultNotifications = defaultNotifications,
                    onDismiss = { activeDialog = null },
                    onConfirm = { title,
                                  startDate,
                                  startTime,
                                  endDate,
                                  endTime,
                                  zoneId,
                                  notifications,
                                  isHoliday,
                                  repeatType,
                                  repeatUntil,
                                  repeatDays ->
                        saveEvent(
                            null,
                            title,
                            startDate,
                            startTime,
                            endDate,
                            endTime,
                            zoneId,
                            notifications,
                            isHoliday,
                            repeatType,
                            repeatUntil,
                            repeatDays
                        )
                    }
                )

            is DetailDialogState.EditGoal ->
                AddGoalDialog(
                    goal = dialog.goal,
                    onDismiss = { activeDialog = null },
                    onConfirm = { name: String, amount: Long ->
                        viewModel.upsertFinancialGoal(dialog.goal.copy(name = name, amount = amount))
                        activeDialog = null
                    }
                )

            is DetailDialogState.EditTransaction ->
                AddTransactionDialog(
                    transaction = dialog.transaction,
                    type = dialog.transaction.type,
                    onDismiss = { activeDialog = null },
                    onConfirm = { name: String, amount: Long ->
                        viewModel.upsertTransaction(
                            dialog.transaction.copy(name = name, amount = amount)
                        )
                        activeDialog = null
                    }
                )

            is DetailDialogState.EditEvent ->
                AddEventDialog(
                    event = dialog.event,
                    year = viewModel.year,
                    month = viewModel.month,
                    day = viewModel.day,
                    defaultNotifications = defaultNotifications,
                    onDismiss = { activeDialog = null },
                    onConfirm = { title,
                                  startDate,
                                  startTime,
                                  endDate,
                                  endTime,
                                  zoneId,
                                  notifications,
                                  isHoliday,
                                  repeatType,
                                  repeatUntil,
                                  repeatDays ->
                        saveEvent(
                            dialog.event,
                            title,
                            startDate,
                            startTime,
                            endDate,
                            endTime,
                            zoneId,
                            notifications,
                            isHoliday,
                            repeatType,
                            repeatUntil,
                            repeatDays
                        )
                    }
                )

            null -> Unit
        }
    }
}

@Composable
fun RealDetailScreen(year: Int, month: Int, day: Int) {
    val context = LocalContext.current
    val application = context.applicationContext as CalYendarApplication
    val notificationManager = remember { EventNotificationManager(application) }
    val settings by
        application.appSettingsStore.settingsFlow.collectAsState(
            initial = application.appSettingsStore.getSettings()
        )
    val viewModel: DetailViewModel =
        viewModel(
            factory =
                DetailViewModelFactory(
                    application.repository,
                    notificationManager,
                    year,
                    month,
                    day
                )
        )

    DetailScreen(
        year = year,
        month = month,
        day = day,
        viewModel = viewModel,
        defaultNotifications = settings.defaultNotificationMinutes
    )
}
