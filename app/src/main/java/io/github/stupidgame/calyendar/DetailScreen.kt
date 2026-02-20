package io.github.stupidgame.calyendar

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
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

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun DetailScreen(year: Int, month: Int, day: Int, viewModel: DetailViewModel) {
    val uiState by viewModel.uiState.collectAsState(initial = DetailUiState())

    var showBottomSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState()

    var showAddGoalDialog by remember { mutableStateOf(false) }
    var showAddIncomeDialog by remember { mutableStateOf(false) }
    var showAddExpenseDialog by remember { mutableStateOf(false) }
    var showAddEventDialog by remember { mutableStateOf(false) }

    var editingTransaction by remember { mutableStateOf<Transaction?>(null) }
    var editingGoal by remember { mutableStateOf<FinancialGoal?>(null) }
    var editingEvent by remember { mutableStateOf<Event?>(null) }

    var showDeleteDialog by remember { mutableStateOf<Any?>(null) }

    if (showDeleteDialog != null) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = null },
            title = { Text("削除の確認") },
            text = { Text("この項目を削除してもよろしいですか？") },
            confirmButton = {
                TextButton(
                    onClick = {
                        when (val item = showDeleteDialog) {
                            is Transaction -> viewModel.deleteTransaction(item)
                            is FinancialGoal -> viewModel.deleteFinancialGoal(item)
                            is Event -> viewModel.deleteEvent(item)
                            is ImportedEvent -> viewModel.deleteImportedEvent(item)
                        }
                        showDeleteDialog = null
                    }
                ) {
                    Text("削除")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = null }) {
                    Text("キャンセル")
                }
            }
        )
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { showBottomSheet = true }) {
                Icon(Icons.Filled.Add, contentDescription = "追加")
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
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
                    displayBalance = uiState.predictionBalance ?: uiState.transactionBalance,
                    goal = uiState.goal,
                    totalGoalCost = uiState.totalGoalCost,
                    onLongClick = { if (uiState.goal != null) showDeleteDialog = uiState.goal },
                    onClick = {
                        editingGoal = uiState.goal
                    }
                )
            }

            val holidays = uiState.events.filter { it.isHoliday } + uiState.icalEvents.filter { it.isHoliday }
            val regularEvents = uiState.events.filter { !it.isHoliday }
            val regularIcalEvents = uiState.icalEvents.filter { !it.isHoliday }

            if (holidays.isNotEmpty()) {
                item {
                    Text("休日", style = MaterialTheme.typography.titleLarge)
                }
                items(holidays) {
                    when (it) {
                        is Event -> EventCard(event = it, onLongClick = { showDeleteDialog = it }) {
                            editingEvent = it
                        }
                        is ImportedEvent -> IcalEventCard(event = it, onLongClick = { showDeleteDialog = it })
                    }
                }
            }

            if (regularEvents.isNotEmpty()) {
                item {
                    Text("イベント", style = MaterialTheme.typography.titleLarge)
                }
                items(regularEvents) { event ->
                    EventCard(event = event, onLongClick = { showDeleteDialog = event }) {
                        editingEvent = event
                    }
                }
            }

            if (regularIcalEvents.isNotEmpty()) {
                item {
                    Text("インポートしたイベント", style = MaterialTheme.typography.titleLarge)
                }
                items(regularIcalEvents) { event ->
                    IcalEventCard(event = event, onLongClick = { showDeleteDialog = event })
                }
            }

            if (uiState.dailyTransactions.isNotEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("取引", style = MaterialTheme.typography.titleLarge)
                }
                items(uiState.dailyTransactions) { transaction ->
                    TransactionCard(transaction = transaction, onLongClick = { showDeleteDialog = transaction }) {
                        editingTransaction = transaction
                    }
                }
            }
        }

        if (showBottomSheet) {
            ModalBottomSheet(
                onDismissRequest = { showBottomSheet = false },
                sheetState = sheetState
            ) {
                Column(modifier = Modifier.padding(bottom = 32.dp)) {
                    ListItem(
                        headlineContent = { Text("目標を編集") },
                        leadingContent = { Icon(Icons.Filled.Edit, contentDescription = null) },
                        modifier = Modifier.clickable { showAddGoalDialog = true; showBottomSheet = false }
                    )
                    ListItem(
                        headlineContent = { Text("収入を追加") },
                        leadingContent = { Icon(Icons.Filled.TrendingUp, contentDescription = null) },
                        modifier = Modifier.clickable { showAddIncomeDialog = true; showBottomSheet = false }
                    )
                    ListItem(
                        headlineContent = { Text("支出を追加") },
                        leadingContent = { Icon(Icons.Filled.TrendingDown, contentDescription = null) },
                        modifier = Modifier.clickable { showAddExpenseDialog = true; showBottomSheet = false }
                    )
                    ListItem(
                        headlineContent = { Text("イベントを追加") },
                        leadingContent = { Icon(Icons.Filled.Event, contentDescription = null) },
                        modifier = Modifier.clickable { showAddEventDialog = true; showBottomSheet = false }
                    )
                    ListItem(
                        headlineContent = { Text("インポートしたイベントを削除") },
                        leadingContent = { Icon(Icons.Filled.Delete, contentDescription = null) },
                        modifier = Modifier.clickable { viewModel.clearImportedEvents(); showBottomSheet = false }
                    )
                }
            }
        }

        editingGoal?.let {
            AddGoalDialog(
                goal = it,
                onDismiss = { editingGoal = null },
                onConfirm = { name: String, amount: Long ->
                    viewModel.upsertFinancialGoal(it.copy(name = name, amount = amount))
                    editingGoal = null
                }
            )
        }

        editingTransaction?.let {
            AddTransactionDialog(
                transaction = it,
                type = it.type,
                onDismiss = { editingTransaction = null },
                onConfirm = { name: String, amount: Long ->
                    viewModel.upsertTransaction(it.copy(name = name, amount = amount))
                    editingTransaction = null
                }
            )
        }

        editingEvent?.let {
            AddEventDialog(
                event = it,
                year = viewModel.year,
                month = viewModel.month,
                day = viewModel.day,
                onDismiss = { editingEvent = null },
                onConfirm = { title, startDate, startTime, endDate, endTime, zoneId, notificationMinutes, isHoliday ->
                    val startMillis = startDate.atTime(startTime).atZone(zoneId).toInstant().toEpochMilli()
                    val endMillis = endDate.atTime(endTime).atZone(zoneId).toInstant().toEpochMilli()
                    viewModel.upsertEvent(it.copy(
                        year = startDate.year,
                        month = startDate.monthValue - 1,
                        day = startDate.dayOfMonth,
                        title = title,
                        startTime = startMillis,
                        endTime = endMillis,
                        notificationMinutesBefore = notificationMinutes,
                        isHoliday = isHoliday
                    ))
                    editingEvent = null
                }
            )
        }

        if (showAddGoalDialog) {
            AddGoalDialog(
                goal = null,
                onDismiss = { showAddGoalDialog = false },
                onConfirm = { name: String, amount: Long ->
                    viewModel.upsertFinancialGoal(
                        FinancialGoal(
                            id = uiState.goal?.id ?: 0,
                            year = viewModel.year,
                            month = viewModel.month,
                            day = viewModel.day,
                            name = name,
                            amount = amount
                        )
                    )
                    showAddGoalDialog = false
                }
            )
        }

        if (showAddIncomeDialog) {
            AddTransactionDialog(
                transaction = null,
                type = TransactionType.INCOME,
                onDismiss = { showAddIncomeDialog = false },
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
                    showAddIncomeDialog = false
                }
            )
        }

        if (showAddExpenseDialog) {
            AddTransactionDialog(
                transaction = null,
                type = TransactionType.EXPENSE,
                onDismiss = { showAddExpenseDialog = false },
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
                    showAddExpenseDialog = false
                }
            )
        }

        if (showAddEventDialog) {
            AddEventDialog(
                event = null,
                year = viewModel.year,
                month = viewModel.month,
                day = viewModel.day,
                onDismiss = { showAddEventDialog = false },
                onConfirm = { title, startDate, startTime, endDate, endTime, zoneId, notificationMinutes, isHoliday ->
                    val startMillis = startDate.atTime(startTime).atZone(zoneId).toInstant().toEpochMilli()
                    val endMillis = endDate.atTime(endTime).atZone(zoneId).toInstant().toEpochMilli()
                    viewModel.upsertEvent(
                        Event(
                            year = startDate.year,
                            month = startDate.monthValue - 1,
                            day = startDate.dayOfMonth,
                            title = title,
                            startTime = startMillis,
                            endTime = endMillis,
                            notificationMinutesBefore = notificationMinutes,
                            isHoliday = isHoliday
                        )
                    )
                    showAddEventDialog = false
                }
            )
        }
    }
}

@Composable
fun RealDetailScreen(year: Int, month: Int, day: Int) {
    val context = LocalContext.current
    val application = context.applicationContext as CalYendarApplication
    val notificationManager = remember { EventNotificationManager(context) }
    val viewModel: DetailViewModel = viewModel(
        factory = DetailViewModelFactory(
            application.repository,
            notificationManager,
            year, month, day
        )
    )
    DetailScreen(year, month, day, viewModel)
}
