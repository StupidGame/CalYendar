package io.github.stupidgame.CalYendar

import android.app.Application
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import biweekly.component.VEvent
import io.github.stupidgame.CalYendar.data.CalYendarDao
import io.github.stupidgame.CalYendar.data.DetailUiState
import io.github.stupidgame.CalYendar.data.DetailViewModel
import io.github.stupidgame.CalYendar.data.DetailViewModelFactory
import io.github.stupidgame.CalYendar.data.Event
import io.github.stupidgame.CalYendar.data.FinancialGoal
import io.github.stupidgame.CalYendar.data.ImportedEvent
import io.github.stupidgame.CalYendar.data.Transaction
import io.github.stupidgame.CalYendar.data.TransactionType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import java.text.SimpleDateFormat
import java.time.ZoneId
import java.util.Date
import java.util.Locale

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
            title = { Text("Delete Item") },
            text = { Text("Are you sure you want to delete this item?") },
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
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { showBottomSheet = true }) {
                Icon(Icons.Filled.Add, contentDescription = "Add Item")
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize(),
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
                SummaryCard(transactionBalance = uiState.transactionBalance, finalBalance = uiState.balance, goal = uiState.goal, onLongClick = { if (uiState.goal != null) showDeleteDialog = uiState.goal }, onClick = {
                    editingGoal = uiState.goal
                })
            }

            if (uiState.events.isNotEmpty()) {
                item {
                    Text("Events", style = MaterialTheme.typography.titleLarge)
                }
                items(uiState.events) { event ->
                    EventCard(event = event, onLongClick = { showDeleteDialog = event }) {
                        editingEvent = event
                    }
                }
            }

            if (uiState.icalEvents.isNotEmpty()) {
                item {
                    Text("Imported Events", style = MaterialTheme.typography.titleLarge)
                }
                items(uiState.icalEvents) { event ->
                    IcalEventCard(event = event, onLongClick = { showDeleteDialog = event })
                }
            }

            if (uiState.dailyTransactions.isNotEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Transactions", style = MaterialTheme.typography.titleLarge)
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
                onConfirm = { name, amount ->
                    viewModel.upsertFinancialGoal(it.copy(name = name, amount = amount))
                    editingGoal = null
                }
            )
        }

        editingTransaction?.let {
            when (it.type) {
                TransactionType.INCOME -> {
                    AddTransactionDialog(
                        transaction = it,
                        type = it.type,
                        onDismiss = { editingTransaction = null },
                        onConfirm = { name, amount ->
                            viewModel.upsertTransaction(it.copy(name = name, amount = amount))
                            editingTransaction = null
                        }
                    )
                }
                TransactionType.EXPENSE -> {
                    AddTransactionDialog(
                        transaction = it,
                        type = it.type,
                        onDismiss = { editingTransaction = null },
                        onConfirm = { name, amount ->
                            viewModel.upsertTransaction(it.copy(name = name, amount = amount))
                            editingTransaction = null
                        }
                    )
                }
                else -> {}
            }
        }

        editingEvent?.let {
            AddEventDialog(
                event = it,
                year = viewModel.year,
                month = viewModel.month,
                day = viewModel.day,
                onDismiss = { editingEvent = null },
                onConfirm = { title, startDate, startTime, endDate, endTime, zoneId, notificationMinutes ->
                    val startMillis = startDate.atTime(startTime).atZone(zoneId).toInstant().toEpochMilli()
                    val endMillis = endDate.atTime(endTime).atZone(zoneId).toInstant().toEpochMilli()
                    viewModel.upsertEvent(it.copy(
                        year = startDate.year,
                        month = startDate.monthValue - 1,
                        day = startDate.dayOfMonth,
                        title = title,
                        startTime = startMillis,
                        endTime = endMillis,
                        notificationMinutesBefore = notificationMinutes
                    ))
                    editingEvent = null
                }
            )
        }

        if (showAddGoalDialog) {
            AddGoalDialog(
                goal = null,
                onDismiss = { showAddGoalDialog = false },
                onConfirm = { name, amount ->

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
                onConfirm = { name, amount ->
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
                onConfirm = { name, amount ->
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
                onConfirm = { title, startDate, startTime, endDate, endTime, zoneId, notificationMinutes ->
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
                            notificationMinutesBefore = notificationMinutes
                        )
                    )
                    showAddEventDialog = false
                }
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SummaryCard(transactionBalance: Long, finalBalance: Long, goal: FinancialGoal?, onLongClick: () -> Unit, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().combinedClickable(onClick = onClick, onLongClick = onLongClick),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = "今日の時点の残高", style = MaterialTheme.typography.titleMedium)
            Text(
                text = "%,d".format(transactionBalance),
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
            )
            Spacer(modifier = Modifier.height(16.dp))
            if (goal != null) {
                val percentage = if (goal.amount > 0) (transactionBalance.toFloat() / goal.amount.toFloat()) else if (transactionBalance >= goal.amount) 1f else 0f

                // Green if met, Yellow if close (80%), Red otherwise
                val cardColor = when {
                    percentage >= 1f -> Color(0xFF66BB6A)
                    percentage >= 0.8f -> Color(0xFFFFEE58)
                    else -> Color(0xFFEF5350)
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Outlined.Flag, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = goal.name, style = MaterialTheme.typography.titleMedium)
                }
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = percentage.coerceIn(0f, 1f),
                    modifier = Modifier.fillMaxWidth(),
                    color = cardColor
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(text = "達成率: %.0f".format(percentage * 100) + "%")
                    Text(text = "目標: %,d".format(goal.amount))
                }
                val difference = finalBalance
                val diffColor = when {
                    difference >= 0 -> Color(0xFF2E7D32)
                    percentage >= 0.8f -> Color(0xFFF9A825) // Dark Yellow
                    else -> Color.Gray
                }

                Text(
                    text = if (difference >= 0) "目標日には %,d 円余ります".format(difference) else "目標日には %,d 円足りません".format(-difference),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = diffColor
                )

            } else {
                Text(
                    text = "目標を設定して、お金を貯めよう！",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TransactionCard(transaction: Transaction, onLongClick: () -> Unit, onClick: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().combinedClickable(onClick = onClick, onLongClick = onLongClick)) {
        val (icon, color, sign) = when (transaction.type) {
            TransactionType.INCOME -> Triple(Icons.Filled.TrendingUp, Color(0xFF2E7D32), "+")
            TransactionType.EXPENSE -> Triple(Icons.Filled.TrendingDown, Color(0xFFC62828), "-")
            TransactionType.GOAL -> Triple(Icons.Filled.Edit, MaterialTheme.colorScheme.primary, "")
        }

        ListItem(
            headlineContent = { Text(transaction.name) },
            leadingContent = { Icon(icon, contentDescription = null, tint = color) },
            trailingContent = { Text("$sign %,d".format(transaction.amount), color = color, fontWeight = FontWeight.Bold) }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun EventCard(event: Event, onLongClick: () -> Unit, onClick: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().combinedClickable(onClick = onClick, onLongClick = onLongClick)) {
        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        val startTime = timeFormat.format(Date(event.startTime))
        val endTime = timeFormat.format(Date(event.endTime))

        ListItem(
            headlineContent = { Text(event.title) },
            supportingContent = { Text("$startTime - $endTime") }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun IcalEventCard(event: ImportedEvent, onLongClick: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().combinedClickable(onClick = {}, onLongClick = onLongClick)) {
        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        val startTime = event.event.dateStart.value?.let { timeFormat.format(it) }
        val endTime = event.event.dateEnd.value?.let { timeFormat.format(it) }

        ListItem(
            headlineContent = { Text(event.event.summary.value) },
            supportingContent = { Text(if(startTime != null && endTime != null) "$startTime - $endTime" else "終日") }
        )
    }
}


@Preview
@Composable
fun DetailScreenPreview() {
    val fakeDao = object : CalYendarDao {
        override fun getTransactionsUpToDate(year: Int, month: Int, day: Int): Flow<List<Transaction>> {
            return flowOf(listOf(
                Transaction(1, 2024, 5, 1, TransactionType.INCOME, "給料", 100000),
                Transaction(2, 2024, 5, 5, TransactionType.EXPENSE, "食費", 5000)
            ))
        }

        override fun getLatestGoalUpToDate(year: Int, month: Int, day: Int): Flow<FinancialGoal?> {
            return flowOf(FinancialGoal(3, 2024, 6, 1, "新しいPC", 150000))
        }

        override fun getEventsForDate(year: Int, month: Int, day: Int): Flow<List<Event>> {
            return flowOf(listOf(
                Event(1, 2024, 5, 17, "友達とランチ", Date().time, Date().time + 3600000, -1)
            ))
        }

        override fun getTransactionsForDate(year: Int, month: Int, day: Int): Flow<List<Transaction>> {
            return flowOf(listOf(
                Transaction(4, 2024, 5, 17, TransactionType.EXPENSE, "ランチ代", 1500)
            ))
        }

        override suspend fun upsertTransaction(transaction: Transaction) {}

        override suspend fun deleteTransaction(transaction: Transaction) {}

        override suspend fun upsertFinancialGoal(goal: FinancialGoal) {}

        override suspend fun deleteFinancialGoal(goal: FinancialGoal) {}

        override suspend fun upsertEvent(event: Event): Long {
            return 0
        }

        override suspend fun deleteEvent(event: Event) {}

        override fun getTransactionsUpTo(year: Int, month: Int): Flow<List<Transaction>> {
            return flowOf(emptyList())
        }

        override fun getTransactionsForMonth(year: Int, month: Int): Flow<List<Transaction>> {
            return flowOf(emptyList())
        }

        override fun getAllGoals(): Flow<List<FinancialGoal>> {
            return flowOf(listOf(FinancialGoal(3, 2024, 6, 1, "新しいPC", 150000)))
        }

        override fun getEventsForMonth(year: Int, month: Int): Flow<List<Event>> {
            return flowOf(emptyList())
        }

        override fun getTransactionsUpToToday(year: Int, month: Int, day: Int): Flow<List<Transaction>> {
            return flowOf(emptyList())
        }
        override fun getImportedEvents(): Flow<List<ImportedEvent>> {
            return flowOf(emptyList())
        }

        override suspend fun upsertImportedEvents(events: List<ImportedEvent>) {}

        override suspend fun clearImportedEvents() {}

        override suspend fun deleteImportedEvent(event: ImportedEvent) {}

    }
    val context = LocalContext.current
    val viewModel = DetailViewModel(context.applicationContext as Application, fakeDao, 2024, 5, 17)
    DetailScreen(year = 2024, month = 5, day = 17, viewModel = viewModel)
}

@Composable
fun RealDetailScreen(year: Int, month: Int, day: Int) {
    val context = LocalContext.current
    val application = context.applicationContext as CalYendarApplication
    val viewModel: DetailViewModel = viewModel(
        factory = DetailViewModelFactory(
            application,
            application.database.calyendarDao(),
            year, month, day
        )
    )
    DetailScreen(year, month, day, viewModel)
}
