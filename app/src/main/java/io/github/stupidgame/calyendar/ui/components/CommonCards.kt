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
    val goals = uiState.monthGoals.sortedWith(compareBy(FinancialGoal::day, FinancialGoal::id))
    val nextGoal = uiState.spanningGoal
    val available = when {
        uiState.isCurrentMonth -> uiState.todayAvailableBalance
        goals.isNotEmpty() -> uiState.availableMoneyAfterMonthGoals
        else -> uiState.currentBalance
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = cardShape,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("お金と目標", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            if (goals.isEmpty() && nextGoal == null) {
                Text(
                    if (uiState.hasTransactions) "今月の目標はありません" else "予定や収支を追加して、この月の見通しを作りましょう",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                goals.forEach { goal -> GoalRow(goal) }
                if (goals.isEmpty() && nextGoal != null) {
                    Text("次の目標", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    GoalRow(nextGoal)
                }
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    if (uiState.isCurrentMonth) "現在使える金額" else "月の見通し",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text("%,d 円".format(available), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
        }
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
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = cardShape,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("この日時点で使える金額", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
            Text(
                "%,d 円".format(balance),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}
