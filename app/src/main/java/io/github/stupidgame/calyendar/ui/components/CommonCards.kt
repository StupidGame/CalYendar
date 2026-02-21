package io.github.stupidgame.calyendar.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SummaryCard(
        displayBalance: Long,
        goal: FinancialGoal?,
        totalGoalCost: Long,
        onLongClick: () -> Unit,
        onClick: () -> Unit
) {
        Card(
                modifier =
                        Modifier.fillMaxWidth()
                                .combinedClickable(onClick = onClick, onLongClick = onLongClick),
                elevation = CardDefaults.cardElevation(4.dp)
        ) {
                Column(modifier = Modifier.padding(16.dp)) {
                        Text(text = "現時点で使えるお金", style = MaterialTheme.typography.titleMedium)
                        val displayAmount =
                                if (goal != null) displayBalance - goal.amount else displayBalance
                        Text(
                                text = "%,d".format(displayAmount),
                                style = MaterialTheme.typography.headlineLarge,
                                fontWeight = FontWeight.Bold,
                                color =
                                        if (displayAmount >= 0) Color(0xFF2E7D32)
                                        else Color(0xFFC62828)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        if (goal != null) {
                                // 百分率を決定
                                // ロジック: (現在の残高) / (目標金額)
                                // 注: predictionBalanceを渡す場合、'displayBalance'には事前に目標が引かれている可能性がある
                                val percentage =
                                        if (goal.amount > 0)
                                                (displayBalance.toFloat() / goal.amount.toFloat())
                                        else if (displayBalance >= 0) 1f else 0f

                                val cardColor =
                                        when {
                                                percentage >= 1f ->
                                                        Color(0xFFA5D6A7) // Pastel Green
                                                percentage >= 0f ->
                                                        Color(0xFFFFF9C4) // Pastel Yellow
                                                else -> Color(0xFFEF9A9A) // Pastel Red
                                        }

                                Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.fillMaxWidth()
                                ) {
                                        Icon(
                                                Icons.Outlined.Flag,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                                text = goal.name,
                                                style = MaterialTheme.typography.titleMedium
                                        )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                LinearProgressIndicator(
                                        progress = { percentage.coerceIn(0f, 1f) },
                                        modifier = Modifier.fillMaxWidth(),
                                        color = cardColor,
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                        Text(text = "達成率: %.0f".format(percentage * 100) + "%")
                                        Text(text = "目標: %,d".format(goal.amount))
                                }
                                val difference = displayBalance - goal.amount

                                val diffColor =
                                        when {
                                                percentage >= 1f -> Color(0xFF2E7D32) // Green
                                                percentage >= 0f -> Color(0xFFF9A825) // Dark Yellow
                                                else -> Color(0xFFEF5350) // Red
                                        }

                                Text(
                                        text =
                                                if (difference >= 0)
                                                        "目標日には %,d 円余ります".format(difference)
                                                else "目標日には %,d 円足りません".format(-difference),
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
        Card(
                modifier =
                        Modifier.fillMaxWidth()
                                .combinedClickable(onClick = onClick, onLongClick = onLongClick)
        ) {
                val (icon, color, sign) =
                        when (transaction.type) {
                                TransactionType.INCOME ->
                                        Triple(Icons.Filled.TrendingUp, Color(0xFF2E7D32), "+")
                                TransactionType.EXPENSE ->
                                        Triple(Icons.Filled.TrendingDown, Color(0xFFC62828), "-")
                                TransactionType.GOAL ->
                                        Triple(
                                                Icons.Filled.Edit,
                                                MaterialTheme.colorScheme.primary,
                                                ""
                                        )
                        }

                ListItem(
                        headlineContent = { Text(transaction.name) },
                        leadingContent = { Icon(icon, contentDescription = null, tint = color) },
                        trailingContent = {
                                Text(
                                        "$sign %,d".format(transaction.amount),
                                        color = color,
                                        fontWeight = FontWeight.Bold
                                )
                        }
                )
        }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun EventCard(event: Event, onLongClick: () -> Unit, onClick: () -> Unit) {
        Card(
                modifier =
                        Modifier.fillMaxWidth()
                                .combinedClickable(onClick = onClick, onLongClick = onLongClick)
        ) {
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
        Card(
                modifier =
                        Modifier.fillMaxWidth()
                                .combinedClickable(onClick = {}, onLongClick = onLongClick)
        ) {
                val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
                val startTime = event.event.dateStart.value?.let { timeFormat.format(it) }
                val endTime = event.event.dateEnd.value?.let { timeFormat.format(it) }

                ListItem(
                        headlineContent = { Text(event.event.summary.value) },
                        supportingContent = {
                                Text(
                                        if (startTime != null && endTime != null)
                                                "$startTime - $endTime"
                                        else "終日"
                                )
                        }
                )
        }
}

@Composable
fun MonthlyGoalCard(uiState: CalendarUiState) {
        val goalsInMonth =
                uiState.dayStates
                        .values
                        .mapNotNull { it.goal }
                        .filter { it.year == uiState.year && it.month == uiState.month }
                        .distinct()

        if (goalsInMonth.isEmpty()) return

        val totalGoalInMonth = goalsInMonth.sumOf { it.amount }
        val difference = uiState.monthlyGoalCurrentBalance - totalGoalInMonth

        val cardColor by
                animateColorAsState(
                        targetValue =
                                getGradientColor(
                                        uiState.monthlyGoalCurrentBalance,
                                        totalGoalInMonth
                                ),
                        label = ""
                )
        val contentColor =
                if (cardColor.luminance() > 0.5f) {
                        Color.Black
                } else {
                        Color.White
                }

        Card(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                colors = CardDefaults.cardColors(containerColor = cardColor),
                shape = RoundedCornerShape(16.dp)
        ) {
                Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                                "今月の目標",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = contentColor
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        goalsInMonth.forEach { goal ->
                                Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                        Text(
                                                goal.name +
                                                        " " +
                                                        "(${goal.month + 1}月${goal.day}日)",
                                                color = contentColor
                                        )
                                        Text("%,d".format(goal.amount), color = contentColor)
                                }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                                Text("目標合計", color = contentColor)
                                Text("%,d".format(totalGoalInMonth), color = contentColor)
                        }
                        val valueColor =
                                if (difference >= 0) Color(0xFF2E7D32) else Color(0xFFC62828)

                        Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                                Text("現在残高", color = contentColor)
                                Text(
                                        "%,d".format(uiState.monthlyGoalCurrentBalance),
                                        color = valueColor,
                                        fontWeight = FontWeight.Bold
                                )
                        }
                        Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                                Text("差額", fontWeight = FontWeight.Bold, color = contentColor)
                                Text(
                                        "%,d".format(difference),
                                        fontWeight = FontWeight.Bold,
                                        color = valueColor
                                )
                        }
                }
        }
}
