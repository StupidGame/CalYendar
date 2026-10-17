package io.github.stupidgame.calyendar.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.stupidgame.calyendar.data.CalendarUiState
import io.github.stupidgame.calyendar.data.Event
import io.github.stupidgame.calyendar.data.FinancialGoal
import io.github.stupidgame.calyendar.data.ImportedEvent
import io.github.stupidgame.calyendar.data.Transaction
import io.github.stupidgame.calyendar.data.TransactionType
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val cardShape = RoundedCornerShape(20.dp)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TransactionCard(transaction: Transaction, onLongClick: () -> Unit, onClick: () -> Unit) {
    val tint = if (transaction.type == TransactionType.EXPENSE) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
    val amountLabel = when (transaction.type) {
        TransactionType.INCOME -> "収入 · +"
        TransactionType.EXPENSE -> "支出 · −"
        TransactionType.GOAL -> "目標 · "
    }
    Card(modifier = Modifier.fillMaxWidth().combinedClickable(onClick = onClick, onLongClick = onLongClick), shape = cardShape) {
        ListItem(
            headlineContent = { Text(transaction.name, maxLines = 2, overflow = TextOverflow.Ellipsis) },
            supportingContent = {
                Text(
                    "$amountLabel%,d 円".format(transaction.amount),
                    color = tint,
                    fontWeight = FontWeight.SemiBold
                )
            },
            leadingContent = {
                Icon(
                    when (transaction.type) {
                        TransactionType.INCOME -> Icons.Filled.TrendingUp
                        TransactionType.EXPENSE -> Icons.Filled.TrendingDown
                        TransactionType.GOAL -> Icons.Outlined.Flag
                    },
                    null,
                    tint = tint
                )
            },
            trailingContent = {
                IconButton(onClick = onLongClick) {
                    Icon(Icons.Filled.Delete, contentDescription = "${transaction.name}を削除")
                }
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun EventCard(event: Event, onLongClick: () -> Unit, onClick: () -> Unit) {
    val formatter = SimpleDateFormat("HH:mm", Locale.JAPAN)
    val timing = "${formatter.format(Date(event.startTime))} – ${formatter.format(Date(event.endTime))}"
    Card(modifier = Modifier.fillMaxWidth().combinedClickable(onClick = onClick, onLongClick = onLongClick), shape = cardShape) {
        ListItem(
            headlineContent = { Text(event.title, maxLines = 2, overflow = TextOverflow.Ellipsis) },
            supportingContent = { Text(if (event.isHoliday) "祝日 · $timing" else timing) },
            leadingContent = { Icon(Icons.Filled.Event, null, tint = MaterialTheme.colorScheme.primary) },
            trailingContent = {
                IconButton(onClick = onLongClick) { Icon(Icons.Filled.Delete, contentDescription = "${event.title}を削除") }
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun IcalEventCard(event: ImportedEvent, onLongClick: () -> Unit) {
    val formatter = SimpleDateFormat("HH:mm", Locale.JAPAN)
    val start = event.event.dateStart?.value
    val end = event.event.dateEnd?.value
    val timing = if (start != null && end != null) {
        "${formatter.format(start)} – ${formatter.format(end)}"
    } else {
        "終日"
    }
    Card(modifier = Modifier.fillMaxWidth().combinedClickable(onClick = {}, onLongClick = onLongClick), shape = cardShape) {
        ListItem(
            headlineContent = { Text(event.event.summary?.value ?: "名称のない予定", maxLines = 2, overflow = TextOverflow.Ellipsis) },
            supportingContent = { Text("$timing · 読み込み済み") },
            leadingContent = { Icon(Icons.Filled.Event, null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
            trailingContent = {
                IconButton(onClick = onLongClick) { Icon(Icons.Filled.Delete, contentDescription = "読み込んだ予定を削除") }
            }
        )
    }
}

@Composable
fun MonthlyGoalCard(uiState: CalendarUiState) {
    val goals = uiState.activeMonthGoals.sortedWith(compareBy(FinancialGoal::day, FinancialGoal::id))
    val nextGoal = uiState.spanningGoal
    val difference = when {
        goals.isNotEmpty() -> uiState.goalComparisonBalance - goals.sumOf(FinancialGoal::amount)
        nextGoal != null -> uiState.spanningGoalBalance - (uiState.spanningGoalTargetAmount ?: nextGoal.amount)
        else -> null
    }
    val cardColor = when {
        difference == null -> MaterialTheme.colorScheme.surface
        difference < 0L -> MaterialTheme.colorScheme.errorContainer
        else -> MaterialTheme.colorScheme.primaryContainer
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = cardShape,
        colors = CardDefaults.cardColors(containerColor = cardColor)
    ) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            when {
                goals.isNotEmpty() -> {
                    Text("今月の目標", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    goals.forEach { GoalRow(it) }
                    val target = goals.sumOf(FinancialGoal::amount)
                    AmountRow("目標合計", target)
                    AmountRow("最後の目標までの残高", uiState.currentBalance)
                    AmountRow("差額", difference ?: 0L, emphasized = true)
                }
                nextGoal != null -> {
                    Text("次の目標", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    GoalRow(nextGoal)
                    val target = uiState.spanningGoalTargetAmount ?: nextGoal.amount
                    AmountRow("目標合計", target)
                    AmountRow(if (uiState.isCurrentMonth) "現在残高" else "月末時点での残高", uiState.spanningGoalBalance)
                    AmountRow("差額", difference ?: 0L, emphasized = true)
                }
                uiState.monthGoals.isNotEmpty() -> {
                    Text("今月の目標はすべて達成しました", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    AmountRow(
                        if (uiState.isPastMonth) "最終的に残ったお金" else "現時点で使えるお金",
                        if (uiState.isCurrentMonth) uiState.todayAvailableBalance else uiState.availableMoneyAfterMonthGoals,
                        emphasized = true
                    )
                }
                else -> {
                    Text("お金と目標", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    if (uiState.hasTransactions) {
                        AmountRow(
                            if (uiState.isPastMonth) "最終的に残ったお金" else if (uiState.isCurrentMonth) "現在の残高" else "月末時点での残高",
                            if (uiState.isCurrentMonth) uiState.todayBalance else uiState.currentBalance,
                            emphasized = true
                        )
                    } else {
                        Text(
                            "予定や収支を追加して、この月の見通しを作りましょう",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AmountRow(label: String, amount: Long, emphasized: Boolean = false) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyMedium, fontWeight = if (emphasized) FontWeight.Bold else FontWeight.Normal)
        Text(
            "%,d 円".format(amount),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (emphasized) FontWeight.Bold else FontWeight.Medium,
            color = if (emphasized && amount < 0L) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun GoalRow(goal: FinancialGoal) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Icon(Icons.Outlined.Flag, null, tint = MaterialTheme.colorScheme.primary)
        Column(modifier = Modifier.weight(1f)) {
            Text(goal.name, style = MaterialTheme.typography.bodyMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text("${goal.month + 1}月${goal.day}日", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text("%,d 円".format(goal.amount), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun CurrentBalanceCard(balance: Long, modifier: Modifier = Modifier) {
    val positive = balance >= 0L
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = cardShape,
        colors = CardDefaults.cardColors(containerColor = if (positive) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.errorContainer)
    ) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("その日時点の所持金", style = MaterialTheme.typography.bodyMedium, color = if (positive) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onErrorContainer)
            Text(
                "%,d 円".format(balance),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = if (positive) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onErrorContainer
            )
        }
    }
}
