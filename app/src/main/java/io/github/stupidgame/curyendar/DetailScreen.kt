package io.github.stupidgame.curyendar

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
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.stupidgame.curyendar.data.CuryendarItem
import io.github.stupidgame.curyendar.data.DetailViewModel
import io.github.stupidgame.curyendar.data.DetailViewModelFactory
import io.github.stupidgame.curyendar.data.Expense
import io.github.stupidgame.curyendar.data.Goal
import io.github.stupidgame.curyendar.data.Income

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(year: Int, month: Int, day: Int) {
    val context = LocalContext.current
    val viewModel: DetailViewModel = viewModel(
        factory = DetailViewModelFactory(
            (context.applicationContext as CuryendarApplication).database.curyendarDao(),
            year, month, day
        )
    )
    val items by viewModel.items.collectAsState(initial = emptyList())

    var showBottomSheet by remember { mutableStateOf(false) }
    var showAddExpenseDialog by remember { mutableStateOf(false) }
    var showAddGoalDialog by remember { mutableStateOf(false) }
    var showAddIncomeDialog by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState()

    val balance = items.sumOf {
        when (it) {
            is Income -> it.amount
            is Expense -> -it.amount
            else -> 0.0
        }
    }
    val goal = items.find { it is Goal } as? Goal

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
                SummaryCard(balance = balance, goal = goal)
            }
            items(items) { item ->
                when (item) {
                    is Goal -> {}
                    is Expense -> ExpenseCard(expense = item)
                    is Income -> IncomeCard(income = item)
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
                        modifier = Modifier.clickable {
                            showAddGoalDialog = true
                            showBottomSheet = false
                        }
                    )
                    ListItem(
                        headlineContent = { Text("収入を追加") },
                        leadingContent = { Icon(Icons.Filled.TrendingUp, contentDescription = null) },
                        modifier = Modifier.clickable {
                            showAddIncomeDialog = true
                            showBottomSheet = false
                        }
                    )
                    ListItem(
                        headlineContent = { Text("支出を追加") },
                        leadingContent = { Icon(Icons.Filled.TrendingDown, contentDescription = null) },
                        modifier = Modifier.clickable {
                            showAddExpenseDialog = true
                            showBottomSheet = false
                        }
                    )
                }
            }
        }

        if (showAddExpenseDialog) {
            AddExpenseDialog(onDismiss = { showAddExpenseDialog = false }) { description, amount ->
                viewModel.insertExpense(description, amount)
                showAddExpenseDialog = false
            }
        }
        if (showAddGoalDialog) {
            AddGoalDialog(onDismiss = { showAddGoalDialog = false }, goal = goal) { name, amount ->
                viewModel.insertGoal(name, amount)
                showAddGoalDialog = false
            }
        }
        if (showAddIncomeDialog) {
            AddIncomeDialog(onDismiss = { showAddIncomeDialog = false }) { description, amount ->
                viewModel.insertIncome(description, amount)
                showAddIncomeDialog = false
            }
        }
    }
}

@Composable
fun SummaryCard(balance: Double, goal: Goal?) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = "現在の残高", style = MaterialTheme.typography.titleMedium)
            val balanceColor by animateColorAsState(
                targetValue = when {
                    balance > 0 -> Color(0xFF2E7D32)
                    balance < 0 -> Color(0xFFC62828)
                    else -> Color.Gray
                },
                animationSpec = tween(500)
            )
            Text(
                text = "%,.0f".format(balance),
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = balanceColor
            )
            Spacer(modifier = Modifier.height(16.dp))
            if (goal != null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Outlined.Flag, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = goal.name, style = MaterialTheme.typography.titleMedium)
                }
                Spacer(modifier = Modifier.height(8.dp))
                val percentage = if (goal.amount > 0) (balance / goal.amount).toFloat() else 0f
                LinearProgressIndicator(
                    progress = percentage.coerceIn(0f, 1f),
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(text = "達成率: %,.0f".format(percentage * 100) + "%")
                    Text(text = "目標: %,.0f".format(goal.amount))
                }
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
fun ExpenseCard(expense: Expense) {
    Card(modifier = Modifier.fillMaxWidth()) {
        ListItem(
            headlineContent = { Text(expense.description) },
            leadingContent = { Icon(Icons.Filled.TrendingDown, contentDescription = null, tint = Color(0xFFC62828)) },
            trailingContent = { Text("- %,.0f".format(expense.amount), color = Color(0xFFC62828), fontWeight = FontWeight.Bold) }
        )
    }
}

@Composable
fun IncomeCard(income: Income) {
    Card(modifier = Modifier.fillMaxWidth()) {
        ListItem(
            headlineContent = { Text(income.description) },
            leadingContent = { Icon(Icons.Filled.TrendingUp, contentDescription = null, tint = Color(0xFF2E7D32)) },
            trailingContent = { Text("+ %,.0f".format(income.amount), color = Color(0xFF2E7D32), fontWeight = FontWeight.Bold) }
        )
    }
}

@Composable
fun AddExpenseDialog(onDismiss: () -> Unit, onAdd: (String, Double) -> Unit) {
    var amount by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("支出を追加") },
        text = {
            Column {
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("内容") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = { Text("金額") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                amount.toDoubleOrNull()?.let { onAdd(description, it) }
            }, enabled = amount.toDoubleOrNull() != null && description.isNotBlank()) {
                Text("追加")
            }
        },
        dismissButton = {
            Button(onClick = onDismiss) {
                Text("キャンセル")
            }
        }
    )
}

@Composable
fun AddGoalDialog(onDismiss: () -> Unit, goal: Goal?, onAdd: (String, Double) -> Unit) {
    var name by remember { mutableStateOf(goal?.name ?: "") }
    var amount by remember { mutableStateOf(goal?.amount?.toString() ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("目標を編集") },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("目標の名前") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = { Text("目標金額") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                amount.toDoubleOrNull()?.let { onAdd(name, it) }
            }, enabled = amount.toDoubleOrNull() != null && name.isNotBlank()) {
                Text("保存")
            }
        },
        dismissButton = {
            Button(onClick = onDismiss) {
                Text("キャンセル")
            }
        }
    )
}

@Composable
fun AddIncomeDialog(onDismiss: () -> Unit, onAdd: (String, Double) -> Unit) {
    var amount by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("収入を追加") },
        text = {
            Column {
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("詳細") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = { Text("収入額") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                amount.toDoubleOrNull()?.let { onAdd(description, it) }
            }, enabled = amount.toDoubleOrNull() != null && description.isNotBlank()) {
                Text("追加")
            }
        },
        dismissButton = {
            Button(onClick = onDismiss) {
                Text("キャンセル")
            }
        }
    )
}
