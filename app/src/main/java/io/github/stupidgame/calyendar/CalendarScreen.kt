package io.github.stupidgame.calyendar

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.stupidgame.calyendar.data.CalendarUiState
import io.github.stupidgame.calyendar.data.CalendarViewModel
import io.github.stupidgame.calyendar.ui.components.DayCell
import io.github.stupidgame.calyendar.ui.components.MonthlyGoalCard
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth

@Composable
fun CalendarScreen(viewModel: CalendarViewModel, year: Int, month: Int, onDayClick: (Int) -> Unit) {
    val uiState by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        if (uiState.year != year || uiState.month != month || uiState.dayStates.isEmpty()) {
            Box(modifier = Modifier.fillMaxWidth().height(240.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            MonthOverview(uiState)
            CalendarGrid(uiState, onDayClick)
            MonthlyGoalCard(uiState)
        }
    }
}

@Composable
private fun MonthOverview(uiState: CalendarUiState) {
    val eventCount = uiState.dayStates.values.sumOf { it.events.size + it.icalEvents.size }
    val transactionCount = uiState.dayStates.values.sumOf { it.transactions.size }

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = "${uiState.month + 1}月の見通し",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "予定 ${eventCount}件  ·  収支 ${transactionCount}件",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun CalendarGrid(uiState: CalendarUiState, onDayClick: (Int) -> Unit) {
    val yearMonth = YearMonth.of(uiState.year, uiState.month + 1)
    val firstDay = LocalDate.of(uiState.year, uiState.month + 1, 1)
    val leadingDays = firstDay.dayOfWeek.sundayFirstIndex()
    val weekCount = (leadingDays + yearMonth.lengthOfMonth() + 6) / 7

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            WeekdaysHeader()
            repeat(weekCount) { week ->
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    repeat(7) { weekday ->
                        val day = week * 7 + weekday - leadingDays + 1
                        val state = uiState.dayStates[day]
                        if (state == null) {
                            Box(modifier = Modifier.weight(1f).height(68.dp))
                        } else {
                            DayCell(
                                dayState = state,
                                year = uiState.year,
                                month = uiState.month,
                                modifier = Modifier.weight(1f),
                                onClick = { onDayClick(day) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun WeekdaysHeader() {
    val labels = listOf("日", "月", "火", "水", "木", "金", "土")
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        labels.forEachIndexed { index, label ->
            val color = when (index) {
                0 -> MaterialTheme.colorScheme.error
                6 -> MaterialTheme.colorScheme.primary
                else -> MaterialTheme.colorScheme.onSurfaceVariant
            }
            Box(modifier = Modifier.weight(1f).height(28.dp), contentAlignment = Alignment.Center) {
                Text(label, style = MaterialTheme.typography.labelMedium, color = color)
            }
        }
    }
}

private fun DayOfWeek.sundayFirstIndex(): Int = value % 7
