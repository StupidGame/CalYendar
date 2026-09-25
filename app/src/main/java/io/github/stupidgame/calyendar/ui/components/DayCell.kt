package io.github.stupidgame.calyendar.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.stupidgame.calyendar.data.DayState
import io.github.stupidgame.calyendar.data.TransactionType
import java.time.DayOfWeek
import java.time.LocalDate
import java.util.Locale
import kotlin.math.abs

@Composable
fun DayCell(dayState: DayState, year: Int, month: Int, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val date = LocalDate.of(year, month + 1, dayState.dayOfMonth)
    val isToday = date == LocalDate.now()
    val isGoalDay = dayState.goal?.let { it.year == year && it.month == month && it.day == date.dayOfMonth } == true
    val income = dayState.transactions.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
    val expense = dayState.transactions.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
    val target = dayState.goalTargetAmount ?: dayState.goal?.amount
    val difference = dayState.predictionDiff ?: if (isGoalDay && target != null) dayState.balance - target else null
    val manualEventCount = dayState.events.size
    val importedEventCount = dayState.icalEvents.size
    val background = when {
        difference != null && difference < 0 -> MaterialTheme.colorScheme.errorContainer
        difference != null -> MaterialTheme.colorScheme.primaryContainer
        else -> MaterialTheme.colorScheme.surface
    }
    val dateColor = when {
        dayState.isHoliday || date.dayOfWeek == DayOfWeek.SUNDAY -> MaterialTheme.colorScheme.error
        date.dayOfWeek == DayOfWeek.SATURDAY -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.onSurface
    }
    val description = buildString {
        append("${month + 1}月${date.dayOfMonth}日")
        if (isToday) append("、今日")
        if (isGoalDay) append("、目標日")
        if (manualEventCount > 0) append("、予定${manualEventCount}件")
        if (importedEventCount > 0) append("、読み込んだ予定${importedEventCount}件")
        if (income != 0L) append("、収入${income}円")
        if (expense != 0L) append("、支出${expense}円")
        if (target != null && isGoalDay) append("、目標${target}円")
        if (difference != null) append(if (difference >= 0) "、余裕${difference}円" else "、不足${abs(difference)}円")
        append("、残高${dayState.balance}円")
    }

    Surface(
        modifier = modifier.height(104.dp).semantics { contentDescription = description }.clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        color = background,
        border = if (isToday) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 2.dp, vertical = 3.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(dayState.dayOfMonth.toString(), color = dateColor, fontWeight = FontWeight.Bold, fontSize = 12.sp, lineHeight = 14.sp)
            Row(
                modifier = Modifier.fillMaxWidth().height(7.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(manualEventCount.coerceAtMost(3)) { EventDot(MaterialTheme.colorScheme.primary) }
                repeat(importedEventCount.coerceAtMost(3)) { EventDot(MaterialTheme.colorScheme.tertiary) }
            }
            DayCellLine(if (income != 0L) "収+${compactYen(income)}" else "", MaterialTheme.colorScheme.primary)
            DayCellLine(if (expense != 0L) "支−${compactYen(expense)}" else "", MaterialTheme.colorScheme.error)
            if (isGoalDay && target != null) DayCellLine("目${compactYen(target)}", MaterialTheme.colorScheme.onSurface)
            if (difference != null) {
                DayCellLine(
                    "${if (difference >= 0) "余" else "不"}${compactYen(abs(difference))}",
                    if (difference >= 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                    bold = true
                )
            }
        }
    }
}

@Composable
private fun EventDot(color: Color) {
    Box(Modifier.padding(horizontal = 1.dp).size(4.dp).background(color, CircleShape))
}

@Composable
private fun DayCellLine(text: String, color: Color, bold: Boolean = false) {
    Text(
        text = text,
        modifier = Modifier.fillMaxWidth().height(12.dp),
        color = color,
        fontSize = 8.sp,
        lineHeight = 10.sp,
        fontWeight = if (bold) FontWeight.Bold else FontWeight.Medium,
        textAlign = TextAlign.Center,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
    )
}

private fun compactYen(amount: Long): String = when {
    amount in -9_999L..9_999L -> amount.toString()
    amount in -99_999_999L..99_999_999L -> String.format(Locale.JAPAN, "%.1f万", amount / 10_000.0)
    else -> String.format(Locale.JAPAN, "%.1f億", amount / 100_000_000.0)
}
