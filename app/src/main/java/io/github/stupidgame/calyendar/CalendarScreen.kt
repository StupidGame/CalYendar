package io.github.stupidgame.calyendar

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.stupidgame.calyendar.data.CalendarUiState
import io.github.stupidgame.calyendar.data.CalendarViewModel
import io.github.stupidgame.calyendar.data.DayState
import io.github.stupidgame.calyendar.ui.components.MonthlyGoalCard
import io.github.stupidgame.calyendar.ui.components.DayCell
import io.github.stupidgame.calyendar.ui.components.getGradientColor
import java.text.DateFormatSymbols
import java.util.Calendar
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
    viewModel: CalendarViewModel,
    onDayClick: (Int) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        WeekdaysHeader()

        val calendar = Calendar.getInstance().apply {
            set(uiState.year, uiState.month, 1)
        }
        val firstDayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
        val emptyCells = (firstDayOfWeek - Calendar.SUNDAY + 7) % 7
        LazyVerticalGrid(
            columns = GridCells.Fixed(7),
            modifier = Modifier.heightIn(max = 500.dp) // Adjust max height as needed
        ) {
            items(emptyCells) {
                Box(modifier = Modifier.aspectRatio(1f))
            }
            items(uiState.dayStates.values.toList()) { day ->
                DayCell(day, uiState.year, uiState.month, onClick = { onDayClick(day.dayOfMonth) })
            }
        }

        MonthlyGoalCard(uiState = uiState)
    }
}

@Composable
fun WeekdaysHeader() {
    val weekdays = DateFormatSymbols(Locale.JAPAN).shortWeekdays
    Row(modifier = Modifier.fillMaxWidth()) {
        for (i in Calendar.SUNDAY..Calendar.SATURDAY) {
            val textColor = when (i) {
                Calendar.SUNDAY -> Color(0xFFD32F2F) // Red
                Calendar.SATURDAY -> Color(0xFF1976D2) // Blue
                else -> MaterialTheme.colorScheme.onSurface
            }
            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(text = weekdays[i], fontSize = 12.sp, color = textColor)
            }
        }
    }
}
