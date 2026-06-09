package io.github.stupidgame.calyendar

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
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
import io.github.stupidgame.calyendar.data.toNotificationStorage
import io.github.stupidgame.calyendar.ui.components.CurrentBalanceCard
import io.github.stupidgame.calyendar.ui.components.DetailGoalSummaryCard
import io.github.stupidgame.calyendar.ui.components.EventCard
import io.github.stupidgame.calyendar.ui.components.IcalEventCard
import io.github.stupidgame.calyendar.ui.components.TransactionCard
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

private data class DetailScreenDate(
    val year: Int,
    val month: Int,
    val day: Int
) {
    val displayText: String = "$year/${month + 1}/$day"

    fun toGoal(name: String, amount: Long): FinancialGoal =
        FinancialGoal(
            id = 0,
            year = year,
            month = month,
            day = day,
            name = name,
            amount = amount
        )

    fun toTransaction(type: TransactionType, name: String, amount: Long): Transaction =
        Transaction(
            year = year,
            month = month,
            day = day,
            type = type,
            name = name,
            amount = amount
        )
}

private sealed interface DetailDialogState {
    data object AddGoal : DetailDialogState
    data object AddIncome : DetailDialogState
    data object AddExpense : DetailDialogState
    data object AddEvent : DetailDialogState
    data class EditGoal(val goal: FinancialGoal) : DetailDialogState
    data class EditTransaction(val transaction: Transaction) : DetailDialogState
    data class EditEvent(val event: Event) : DetailDialogState
}

private sealed interface DetailDeleteTarget {
    data class Goal(val goal: FinancialGoal) : DetailDeleteTarget
    data class TransactionItem(val transaction: Transaction) : DetailDeleteTarget
    data class EventItem(val event: Event) : DetailDeleteTarget
    data class ImportedEventItem(val event: ImportedEvent) : DetailDeleteTarget
}

private data class DetailEventForm(
    val title: String,
    val startDate: LocalDate,
    val startTime: LocalTime,
    val endDate: LocalDate,
    val endTime: LocalTime,
    val zoneId: ZoneId,
    val notifications: List<Long>,
    val isHoliday: Boolean,
    val repeatType: EventRepeatType,
    val repeatUntil: LocalDate?,
    val repeatDays: Set<Int>
) {
    fun toEvent(existingEvent: Event?): Event {
        val startMillis = startDate.atTime(startTime).atZone(zoneId).toInstant().toEpochMilli()
        val endMillis = endDate.atTime(endTime).atZone(zoneId).toInstant().toEpochMilli()
        val baseEvent =
            existingEvent
                ?: Event(
                    year = startDate.year,
                    month = startDate.monthValue - 1,
                    day = startDate.dayOfMonth,
                    title = title,
                    startTime = startMillis,
                    endTime = endMillis,
                    notificationMinutesBefore = -1L
                )

        return baseEvent.copy(
            year = startDate.year,
            month = startDate.monthValue - 1,
            day = startDate.dayOfMonth,
            title = title,
            startTime = startMillis,
            endTime = endMillis,
            notificationMinutesBefore = -1L,
            notifications = notifications.toNotificationStorage(),
            isHoliday = isHoliday
        )
    }
}

@Composable
fun DetailScreen(
    year: Int,
    month: Int,
    day: Int,
    viewModel: DetailViewModel,
    defaultNotifications: List<Long> = emptyList()
) {
    val uiState by viewModel.uiState.collectAsState(initial = DetailUiState())
    val selectedDate = remember(year, month, day) { DetailScreenDate(year, month, day) }

    var showActionSheet by remember { mutableStateOf(false) }
    var activeDialog by remember { mutableStateOf<DetailDialogState?>(null) }
    var deleteTarget by remember { mutableStateOf<DetailDeleteTarget?>(null) }

    deleteTarget?.let { target ->
        DeleteConfirmationDialog(
            onDismiss = { deleteTarget = null },
            onConfirm = {
                viewModel.deleteTarget(target)
                deleteTarget = null
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
        DetailScreenContent(
            selectedDate = selectedDate,
            uiState = uiState,
            contentPadding =
                PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = innerPadding.calculateTopPadding() + 16.dp,
                    bottom = innerPadding.calculateBottomPadding() + 88.dp
                ),
            onEditGoal = { activeDialog = DetailDialogState.EditGoal(it) },
            onDeleteGoal = { deleteTarget = DetailDeleteTarget.Goal(it) },
            onEditEvent = { activeDialog = DetailDialogState.EditEvent(it) },
            onDeleteEvent = { deleteTarget = DetailDeleteTarget.EventItem(it) },
            onDeleteImportedEvent = { deleteTarget = DetailDeleteTarget.ImportedEventItem(it) },
            onEditTransaction = { activeDialog = DetailDialogState.EditTransaction(it) },
            onDeleteTransaction = { deleteTarget = DetailDeleteTarget.TransactionItem(it) }
        )

        if (showActionSheet) {
            DetailActionSheet(
                onDismiss = { showActionSheet = false },
                onSelectDialog = { dialog ->
                    activeDialog = dialog
                    showActionSheet = false
                },
                onClearImportedEvents = {
                    viewModel.clearImportedEvents()
                    showActionSheet = false
                }
            )
        }

        DetailDialogHost(
            dialog = activeDialog,
            selectedDate = selectedDate,
            defaultNotifications = defaultNotifications,
            onDismiss = { activeDialog = null },
            onSaveGoal = { goal ->
                viewModel.upsertFinancialGoal(goal)
                activeDialog = null
            },
            onSaveTransaction = { transaction ->
                viewModel.upsertTransaction(transaction)
                activeDialog = null
            },
            onSaveEvent = { existingEvent, form ->
                viewModel.saveEvent(existingEvent, form)
                activeDialog = null
            }
        )
    }
}

@Composable
private fun DetailScreenContent(
    selectedDate: DetailScreenDate,
    uiState: DetailUiState,
    contentPadding: PaddingValues,
    onEditGoal: (FinancialGoal) -> Unit,
    onDeleteGoal: (FinancialGoal) -> Unit,
    onEditEvent: (Event) -> Unit,
    onDeleteEvent: (Event) -> Unit,
    onDeleteImportedEvent: (ImportedEvent) -> Unit,
    onEditTransaction: (Transaction) -> Unit,
    onDeleteTransaction: (Transaction) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            DetailHeader(
                selectedDate = selectedDate,
                uiState = uiState,
                onEditGoal = onEditGoal,
                onDeleteGoal = onDeleteGoal
            )
        }

        eventSections(
            events = uiState.events,
            importedEvents = uiState.icalEvents,
            onEditEvent = onEditEvent,
            onDeleteEvent = onDeleteEvent,
            onDeleteImportedEvent = onDeleteImportedEvent
        )

        transactionSection(
            transactions = uiState.dailyTransactions,
            onEditTransaction = onEditTransaction,
            onDeleteTransaction = onDeleteTransaction
        )
    }
}

@Composable
private fun DetailHeader(
    selectedDate: DetailScreenDate,
    uiState: DetailUiState,
    onEditGoal: (FinancialGoal) -> Unit,
    onDeleteGoal: (FinancialGoal) -> Unit
) {
    Text(
        text = selectedDate.displayText,
        style = MaterialTheme.typography.headlineMedium,
        fontWeight = FontWeight.Bold
    )
    Spacer(modifier = Modifier.height(16.dp))
    CurrentBalanceCard(balance = uiState.currentBalance)
    Spacer(modifier = Modifier.height(16.dp))
    DetailGoalSummaryCard(
        displayBalance = uiState.currentBalance,
        goal = uiState.goal,
        goalTargetAmount = uiState.goalTargetAmount,
        totalGoalCost = uiState.totalGoalCost,
        onLongClick = { uiState.goal?.let(onDeleteGoal) },
        onClick = { uiState.goal?.let(onEditGoal) }
    )
}

private fun LazyListScope.eventSections(
    events: List<Event>,
    importedEvents: List<ImportedEvent>,
    onEditEvent: (Event) -> Unit,
    onDeleteEvent: (Event) -> Unit,
    onDeleteImportedEvent: (ImportedEvent) -> Unit
) {
    val holidayEvents = events.filter(Event::isHoliday)
    val holidayImportedEvents = importedEvents.filter(ImportedEvent::isHoliday)
    val regularEvents = events.filterNot(Event::isHoliday)
    val regularImportedEvents = importedEvents.filterNot(ImportedEvent::isHoliday)

    if (holidayEvents.isNotEmpty() || holidayImportedEvents.isNotEmpty()) {
        item { DetailSectionTitle("祝日") }
        items(holidayEvents, key = { "event-${it.id}" }) { event ->
            EventCard(event = event, onLongClick = { onDeleteEvent(event) }) {
                onEditEvent(event)
            }
        }
        items(holidayImportedEvents, key = { "imported-event-${it.id}" }) { event ->
            IcalEventCard(event = event, onLongClick = { onDeleteImportedEvent(event) })
        }
    }

    if (regularEvents.isNotEmpty()) {
        item { DetailSectionTitle("イベント") }
        items(regularEvents, key = { "event-${it.id}" }) { event ->
            EventCard(event = event, onLongClick = { onDeleteEvent(event) }) {
                onEditEvent(event)
            }
        }
    }

    if (regularImportedEvents.isNotEmpty()) {
        item { DetailSectionTitle("インポートしたイベント") }
        items(regularImportedEvents, key = { "imported-event-${it.id}" }) { event ->
            IcalEventCard(event = event, onLongClick = { onDeleteImportedEvent(event) })
        }
    }
}

private fun LazyListScope.transactionSection(
    transactions: List<Transaction>,
    onEditTransaction: (Transaction) -> Unit,
    onDeleteTransaction: (Transaction) -> Unit
) {
    if (transactions.isEmpty()) return

    item {
        Spacer(modifier = Modifier.height(16.dp))
        DetailSectionTitle("取引")
    }
    items(transactions, key = { "transaction-${it.id}" }) { transaction ->
        TransactionCard(
            transaction = transaction,
            onLongClick = { onDeleteTransaction(transaction) }
        ) {
            onEditTransaction(transaction)
        }
    }
}

@Composable
private fun DetailSectionTitle(text: String) {
    Text(text = text, style = MaterialTheme.typography.titleLarge)
}

@Composable
private fun DeleteConfirmationDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("削除の確認") },
        text = { Text("この項目を削除してもよろしいですか？") },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text("削除") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("キャンセル") }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DetailActionSheet(
    onDismiss: () -> Unit,
    onSelectDialog: (DetailDialogState) -> Unit,
    onClearImportedEvents: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(modifier = Modifier.padding(bottom = 32.dp)) {
            ActionSheetItem(
                text = "目標を追加",
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                onClick = { onSelectDialog(DetailDialogState.AddGoal) }
            )
            ActionSheetItem(
                text = "収入を追加",
                icon = { Icon(Icons.Filled.TrendingUp, contentDescription = null) },
                onClick = { onSelectDialog(DetailDialogState.AddIncome) }
            )
            ActionSheetItem(
                text = "支出を追加",
                icon = { Icon(Icons.Filled.TrendingDown, contentDescription = null) },
                onClick = { onSelectDialog(DetailDialogState.AddExpense) }
            )
            ActionSheetItem(
                text = "イベントを追加",
                icon = { Icon(Icons.Filled.Event, contentDescription = null) },
                onClick = { onSelectDialog(DetailDialogState.AddEvent) }
            )
            ActionSheetItem(
                text = "インポートしたイベントを削除",
                icon = { Icon(Icons.Filled.Delete, contentDescription = null) },
                onClick = onClearImportedEvents
            )
        }
    }
}

@Composable
private fun ActionSheetItem(
    text: String,
    icon: @Composable () -> Unit,
    onClick: () -> Unit
) {
    ListItem(
        headlineContent = { Text(text) },
        leadingContent = icon,
        modifier = Modifier.clickable(onClick = onClick)
    )
}

@Composable
private fun DetailDialogHost(
    dialog: DetailDialogState?,
    selectedDate: DetailScreenDate,
    defaultNotifications: List<Long>,
    onDismiss: () -> Unit,
    onSaveGoal: (FinancialGoal) -> Unit,
    onSaveTransaction: (Transaction) -> Unit,
    onSaveEvent: (Event?, DetailEventForm) -> Unit
) {
    when (dialog) {
        DetailDialogState.AddGoal ->
            AddGoalDialog(
                goal = null,
                onDismiss = onDismiss,
                onConfirm = { name, amount ->
                    onSaveGoal(selectedDate.toGoal(name, amount))
                }
            )

        DetailDialogState.AddIncome ->
            AddTransactionDialog(
                transaction = null,
                type = TransactionType.INCOME,
                onDismiss = onDismiss,
                onConfirm = { name, amount ->
                    onSaveTransaction(selectedDate.toTransaction(TransactionType.INCOME, name, amount))
                }
            )

        DetailDialogState.AddExpense ->
            AddTransactionDialog(
                transaction = null,
                type = TransactionType.EXPENSE,
                onDismiss = onDismiss,
                onConfirm = { name, amount ->
                    onSaveTransaction(selectedDate.toTransaction(TransactionType.EXPENSE, name, amount))
                }
            )

        DetailDialogState.AddEvent ->
            AddEventDialog(
                event = null,
                year = selectedDate.year,
                month = selectedDate.month,
                day = selectedDate.day,
                defaultNotifications = defaultNotifications,
                onDismiss = onDismiss,
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
                    onSaveEvent(
                        null,
                        DetailEventForm(
                            title = title,
                            startDate = startDate,
                            startTime = startTime,
                            endDate = endDate,
                            endTime = endTime,
                            zoneId = zoneId,
                            notifications = notifications,
                            isHoliday = isHoliday,
                            repeatType = repeatType,
                            repeatUntil = repeatUntil,
                            repeatDays = repeatDays
                        )
                    )
                }
            )

        is DetailDialogState.EditGoal ->
            AddGoalDialog(
                goal = dialog.goal,
                onDismiss = onDismiss,
                onConfirm = { name, amount ->
                    onSaveGoal(dialog.goal.copy(name = name, amount = amount))
                }
            )

        is DetailDialogState.EditTransaction ->
            AddTransactionDialog(
                transaction = dialog.transaction,
                type = dialog.transaction.type,
                onDismiss = onDismiss,
                onConfirm = { name, amount ->
                    onSaveTransaction(dialog.transaction.copy(name = name, amount = amount))
                }
            )

        is DetailDialogState.EditEvent ->
            AddEventDialog(
                event = dialog.event,
                year = selectedDate.year,
                month = selectedDate.month,
                day = selectedDate.day,
                defaultNotifications = defaultNotifications,
                onDismiss = onDismiss,
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
                    onSaveEvent(
                        dialog.event,
                        DetailEventForm(
                            title = title,
                            startDate = startDate,
                            startTime = startTime,
                            endDate = endDate,
                            endTime = endTime,
                            zoneId = zoneId,
                            notifications = notifications,
                            isHoliday = isHoliday,
                            repeatType = repeatType,
                            repeatUntil = repeatUntil,
                            repeatDays = repeatDays
                        )
                    )
                }
            )

        null -> Unit
    }
}

private fun DetailViewModel.deleteTarget(target: DetailDeleteTarget) {
    when (target) {
        is DetailDeleteTarget.Goal -> deleteFinancialGoal(target.goal)
        is DetailDeleteTarget.TransactionItem -> deleteTransaction(target.transaction)
        is DetailDeleteTarget.EventItem -> deleteEvent(target.event)
        is DetailDeleteTarget.ImportedEventItem -> deleteImportedEvent(target.event)
    }
}

private fun DetailViewModel.saveEvent(
    existingEvent: Event?,
    form: DetailEventForm
) {
    upsertEventWithRepeat(
        event = form.toEvent(existingEvent),
        repeatType = form.repeatType,
        repeatUntil = form.repeatUntil,
        repeatDays = form.repeatDays
    )
}

@Composable
fun RealDetailScreen(year: Int, month: Int, day: Int) {
    val context = LocalContext.current
    val application = context.applicationContext as CalYendarApplication
    val settings by
        application.appSettingsStore.settingsFlow.collectAsState(
            initial = application.appSettingsStore.getSettings()
        )
    val viewModel: DetailViewModel =
        viewModel(
            factory =
                DetailViewModelFactory(
                    application.repository,
                    application.eventSyncService,
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
