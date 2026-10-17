package io.github.stupidgame.calyendar.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.stupidgame.calyendar.data.DayState
import java.time.DayOfWeek
import java.time.LocalDate
import java.util.Locale

@Composable
fun DayCell(
    dayState: DayState,
    year: Int,
    month: Int,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val date = LocalDate.of(year, month + 1, dayState.dayOfMonth)
    val isToday = date == LocalDate.now()
    val isGoalDay = dayState.goal?.let { it.year == year && it.month == month && it.day == date.dayOfMonth } == true
    val eventCount = dayState.events.size + dayState.icalEvents.size
    val contentColor = when {
        dayState.isHoliday || date.dayOfWeek == DayOfWeek.SUNDAY -> MaterialTheme.colorScheme.error
        date.dayOfWeek == DayOfWeek.SATURDAY -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.onSurface
    }
    val description = buildString {
        append("${month + 1}月${date.dayOfMonth}日")
        if (isToday) append("、今日")
        if (isGoalDay) append("、目標日")
        if (eventCount > 0) append("、予定${eventCount}件")
        if (dayState.transactions.isNotEmpty()) append("、収支${dayState.transactions.size}件")
        append("、残高${dayState.balance}円")
    }

    Surface(
        modifier = modifier.height(68.dp).semantics { contentDescription = description }.clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = if (isToday) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
        border = if (isToday) BorderStroke(1.dp, MaterialTheme.colorScheme.primary) else null
    ) {
        Column(
            modifier = Modifier.padding(vertical = 5.dp, horizontal = 2.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = dayState.dayOfMonth.toString(),
                color = contentColor,
                fontWeight = if (isToday) FontWeight.Bold else FontWeight.Medium,
                style = MaterialTheme.typography.labelLarge
            )
            Text(
                text = when {
                    isGoalDay -> "目標"
                    dayState.transactions.isNotEmpty() -> compactYen(dayState.balance)
                    else -> ""
                },
                color = if (isGoalDay) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 9.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (eventCount > 0) "●" else " ",
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 9.sp,
                    lineHeight = 10.sp
                )
                if (dayState.predictionDiff?.let { it < 0L } == true) {
                    Text("•", color = MaterialTheme.colorScheme.error, fontSize = 9.sp, lineHeight = 10.sp)
                }
            }
        }
    }
}

private fun compactYen(amount: Long): String = when {
    amount in -9_999L..9_999L -> String.format(Locale.JAPAN, "%,d", amount)
    else -> String.format(Locale.JAPAN, "%.1f万", amount / 10_000.0)
}
