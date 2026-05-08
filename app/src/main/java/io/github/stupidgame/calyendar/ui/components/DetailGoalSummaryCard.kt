package io.github.stupidgame.calyendar.ui.components

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
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.stupidgame.calyendar.data.FinancialGoal

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DetailGoalSummaryCard(
    displayBalance: Long,
    goal: FinancialGoal?,
    totalGoalCost: Long,
    onLongClick: () -> Unit,
    onClick: () -> Unit
) {
    val goalTargetAmount = goal?.amount
    val comparisonBalance = displayBalance

    Card(
        modifier =
            Modifier.fillMaxWidth().combinedClickable(onClick = onClick, onLongClick = onLongClick),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            if (goal != null && goalTargetAmount != null) {
                val percentage =
                    if (goalTargetAmount > 0) {
                        comparisonBalance.toFloat() / goalTargetAmount.toFloat()
                    } else if (comparisonBalance >= 0) {
                        1f
                    } else {
                        0f
                    }
                val cardColor = getGradientColor(comparisonBalance, goalTargetAmount)
                val remainingAfterGoal = comparisonBalance - goalTargetAmount
                val diffColor =
                    when {
                        remainingAfterGoal >= 0 -> Color(0xFF2E7D32)
                        remainingAfterGoal >= -goalTargetAmount -> Color(0xFFF9A825)
                        else -> Color(0xFFEF5350)
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
                    Text(text = goal.name, style = MaterialTheme.typography.titleMedium)
                }
                Text(
                    text = "期限: ${goal.year}/${goal.month + 1}/${goal.day}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
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
                    Text(text = "達成率 %.0f%%".format(percentage * 100))
                    Text(text = "目標: %,d".format(goalTargetAmount))
                }
                Text(
                    text =
                        if (remainingAfterGoal >= 0) {
                            "目標達成後の残り: %,d 円".format(remainingAfterGoal)
                        } else {
                            "目標達成まであと %,d 円足りません".format(-remainingAfterGoal)
                        },
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = diffColor
                )
            } else {
                Text(
                    text =
                        if (totalGoalCost > 0L) {
                            "目標をすべて達成しています"
                        } else {
                            "目標を設定すると、残高を先回りで見られます"
                        },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}
