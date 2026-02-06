package io.github.stupidgame.calyendar

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.outlined.Flag
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
import io.github.stupidgame.calyendar.data.calyendarDao
import io.github.stupidgame.calyendar.data.DetailUiState
import io.github.stupidgame.calyendar.data.DetailViewModel
import io.github.stupidgame.calyendar.data.DetailViewModelFactory
import io.github.stupidgame.calyendar.data.Event
import io.github.stupidgame.calyendar.data.FinancialGoal
import io.github.stupidgame.calyendar.data.Transaction
import io.github.stupidgame.calyendar.data.TransactionType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
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

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { showBottomSheet = true }) {
                Icon(Icons.Filled.Add, contentDescription = "Add Item")
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text(
                    text = "$year/${month + 1}/$day",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(16.dp))
                SummaryCard(balance = uiState.balance, goal = uiState.goal) {
                    editingGoal = uiState.goal
                }
            }

            if (uiState.events.isNotEmpty()) {
                item {
                    Text("Events", style = MaterialTheme.typography.titleLarge)
                }
                items(uiState.events) { event ->
                    EventCard(event = event) {
                        editingEvent = event
                    }
                }
            }

            if (uiState.dailyTransactions.isNotEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Transactions", style = MaterialTheme.typography.titleLarge)
                }
                items(uiState.dailyTransactions) { transaction ->
                    TransactionCard(transaction = transaction) {
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
                onConfirm = { title, startTime, endTime, notificationMinutes ->
                    viewModel.upsertEvent(it.copy(title = title, startTime = startTime, endTime = endTime, notificationMinutesBefore = notificationMinutes))
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
                onConfirm = { title, startTime, endTime, notificationMinutes ->
                    viewModel.upsertEvent(
                        Event(
                            year = viewModel.year,
                            month = viewModel.month,
                            day = viewModel.day,
                            title = title,
                            startTime = startTime,
                            endTime = endTime,
                            notificationMinutesBefore = notificationMinutes
                        )
                    )
                    showAddEventDialog = false
                }
            )
        }
    }
}

@Composable
fun SummaryCard(balance: Long, goal: FinancialGoal?, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = "現在の残高", style = MaterialTheme.typography.titleMedium)
            Text(
                text = "%,d".format(balance),
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
            )
            Spacer(modifier = Modifier.height(16.dp))
            if (goal != null) {
                val percentage = if (goal.amount > 0) (balance.toFloat() / goal.amount.toFloat()) else if (balance >= goal.amount) 1f else 0f
                
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
                val difference = balance - goal.amount
                val diffColor = when {
                     difference >= 0 -> Color(0xFF2E7D32)
                     percentage >= 0.8f -> Color(0xFFF9A825) // Dark Yellow
                     else -> Color.Gray
                }
                
                Text(
                    text = if (difference >= 0) "目標達成！ (+%,d)".format(difference) else "目標まであと %,d".format(-difference),
                    style = MaterialTheme.typography.bodyMedium,
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

@Composable
fun TransactionCard(transaction: Transaction, onClick: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)) {
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

@Composable
fun EventCard(event: Event, onClick: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        val startTime = timeFormat.format(Date(event.startTime))
        val endTime = timeFormat.format(Date(event.endTime))

        ListItem(
            headlineContent = { Text(event.title) },
            supportingContent = { Text("$startTime - $endTime") }
        )
    }
}

@Preview
@Composable
fun DetailScreenPreview() {
    val fakeDao = object : calyendarDao {
        override fun getTransactionsUpToDate(year: Int, month: Int, day: Int): Flow<List<Transaction>> {
            return flowOf(listOf(
                Transaction(1, 2024, 5, 1, TransactionType.INCOME, "給料", 100000),
                Transaction(2, 2024, 5, 5, TransactionType.EXPENSE, "食費", 5000)
            ))
        }

        override fun getLatestGoalUpToDate(year: Int, month: Int, day: Int): Flow<FinancialGoal?> {
            return flowOf(FinancialGoal(3, 2024, 5, 1, "PS5", 50000))
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

        override suspend fun upsertFinancialGoal(goal: FinancialGoal) {}

        override suspend fun upsertEvent(event: Event) {}

        override fun getTransactionsUpTo(year: Int, month: Int): Flow<List<Transaction>> {
            return flowOf(emptyList())
        }

        override fun getTransactionsForMonth(year: Int, month: Int): Flow<List<Transaction>> {
            return flowOf(emptyList())
        }

        override fun getAllGoals(): Flow<List<FinancialGoal>> {
            return flowOf(emptyList())
        }

        override fun getEventsForMonth(year: Int, month: Int): Flow<List<Event>> {
            return flowOf(emptyList())
        }
    }
    val viewModel = DetailViewModel(fakeDao, 2024, 5, 17)
    DetailScreen(year = 2024, month = 5, day = 17, viewModel = viewModel)
}

@Composable
fun RealDetailScreen(year: Int, month: Int, day: Int) {
    val context = LocalContext.current
    val viewModel: DetailViewModel = viewModel(
        factory = DetailViewModelFactory(
            (context.applicationContext as calyendarApplication).database.calyendarDao(),
            year, month, day
        )
    )
    DetailScreen(year, month, day, viewModel)
}
