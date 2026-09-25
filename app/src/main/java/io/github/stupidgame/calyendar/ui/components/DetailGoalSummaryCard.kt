package io.github.stupidgame.calyendar.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.stupidgame.calyendar.data.FinancialGoal

@Composable
fun DetailGoalSummaryCard(
    displayBalance: Long,
    goals: List<FinancialGoal>,
    goalTargetAmount: Long? = goals.takeIf { it.isNotEmpty() }?.sumOf(FinancialGoal::amount),
    totalGoalCost: Long,
    onGoalLongClick: (FinancialGoal) -> Unit,
    onGoalClick: (FinancialGoal) -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp)) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Outlined.Flag, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Text("目標", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }

            if (goals.isEmpty()) {
                Text(
                    if (totalGoalCost > 0L) "設定した目標はすべて達成済みです" else "目標を追加すると達成までの見通しを表示します",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                return@Column
            }

            val target = goalTargetAmount ?: goals.sumOf(FinancialGoal::amount)
            val firstDeadline = goals.minWithOrNull(compareBy(FinancialGoal::year, FinancialGoal::month, FinancialGoal::day))
            if (firstDeadline != null) {
                Text(
                    "次の期限: ${firstDeadline.year}年${firstDeadline.month + 1}月${firstDeadline.day}日",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            goals.forEach { goal ->
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            goal.name,
                            modifier = Modifier.padding(vertical = 2.dp),
                            style = MaterialTheme.typography.bodyLarge,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            "${goal.year}年${goal.month + 1}月${goal.day}日 · %,d 円".format(goal.amount),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = { onGoalClick(goal) }) {
                        Text("編集", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelMedium)
                    }
                    IconButton(onClick = { onGoalLongClick(goal) }) {
                        Icon(Icons.Filled.DeleteOutline, contentDescription = "${goal.name}を削除")
                    }
                }
            }

            val achievementRate = if (target > 0L) displayBalance.toDouble() / target else if (displayBalance >= 0L) 1.0 else 0.0
            LinearProgressIndicator(progress = { achievementRate.toFloat().coerceIn(0f, 1f) }, modifier = Modifier.fillMaxWidth())
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("目標合計 %,d 円".format(target), style = MaterialTheme.typography.bodyMedium)
                Text("達成率 %.0f%%".format(achievementRate * 100), style = MaterialTheme.typography.bodyMedium)
            }
            val difference = displayBalance - target
            Text(
                if (difference >= 0L) "目標達成後の残り: %,d 円".format(difference) else "目標達成まであと %,d 円足りません".format(-difference),
                color = if (difference >= 0L) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}
