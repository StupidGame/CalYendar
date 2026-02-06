package io.github.stupidgame.CalYendar

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.stupidgame.CalYendar.data.CalendarUiState
import io.github.stupidgame.CalYendar.data.CalendarViewModel
import io.github.stupidgame.CalYendar.data.DayState
import io.github.stupidgame.CalYendar.data.TransactionType
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
        val totalGoal = uiState.dayStates.values.mapNotNull { it.goal }.sumOf { it.amount }

        LazyVerticalGrid(
            columns = GridCells.Fixed(7),
            modifier = Modifier.heightIn(max = 500.dp) // Adjust max height as needed
        ) {
            items(emptyCells) {
                Box(modifier = Modifier.aspectRatio(1f))
            }
            items(uiState.dayStates.values.toList()) { day ->
                DayCell(day, uiState.year, uiState.month, totalGoal, onClick = { onDayClick(day.dayOfMonth) })
            }
        }

        MonthlyGoalCard(uiState = uiState)
    }
}

@Composable
fun MonthlyGoalCard(uiState: CalendarUiState) {
    val goalsInMonth = uiState.dayStates.values
        .mapNotNull { it.goal }
        .filter { it.year == uiState.year && it.month == uiState.month }
        .distinct()

    if (goalsInMonth.isEmpty()) return

    val totalGoalInMonth = goalsInMonth.sumOf { it.amount }
    val difference = uiState.currentBalance - totalGoalInMonth

    val cardColor by animateColorAsState(
        targetValue = getGradientColor(difference, totalGoalInMonth),
        label = ""
    )
    val contentColor = if (cardColor.luminance() > 0.5f) {
        Color.Black
    } else {
        Color.White
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(containerColor = cardColor),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("今月の目標", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = contentColor)
            Spacer(modifier = Modifier.height(8.dp))

            goalsInMonth.forEach { goal ->
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(goal.name, color = contentColor)
                    Text("%,d".format(goal.amount), color = contentColor)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("目標合計", color = contentColor)
                Text("%,d".format(totalGoalInMonth), color = contentColor)
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("現在残高", color = contentColor)
                Text("%,d".format(uiState.currentBalance), color = contentColor)
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("差額", fontWeight = FontWeight.Bold, color = contentColor)
                Text("%,d".format(difference), fontWeight = FontWeight.Bold, color = contentColor)
            }
        }
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

@Composable
fun DayCell(dayState: DayState, year: Int, month: Int, totalGoal: Long, onClick: () -> Unit) {
    val predictionDiff = dayState.predictionDiff
    
    val cardColor = when {
        predictionDiff != null -> getGradientColor(predictionDiff, totalGoal)
        else -> MaterialTheme.colorScheme.surface
    }
    val contentColor = if (cardColor.luminance() > 0.5f) Color.Black else Color.White

    val calendar = Calendar.getInstance().apply { set(year, month, dayState.dayOfMonth) }
    val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
    
    val dateTextColor = if (cardColor != MaterialTheme.colorScheme.surface) {
        contentColor
    } else {
        when {
            dayState.isHoliday || dayOfWeek == Calendar.SUNDAY -> Color(0xFFD32F2F)
            dayOfWeek == Calendar.SATURDAY -> Color(0xFF1976D2)
            else -> MaterialTheme.colorScheme.onSurface
        }
    }

    val today = java.time.LocalDate.now()
    val currentDayDate = java.time.LocalDate.of(year, month + 1, dayState.dayOfMonth)
    val isToday = currentDayDate.isEqual(today)

    Card(
        modifier = Modifier
            .padding(2.dp)
            .aspectRatio(1f)
            .let { 
                if (isToday) it.border(3.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(12.dp)) 
                else it 
            }
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = cardColor),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(2.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Date
            Text(
                text = dayState.dayOfMonth.toString(),
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                fontSize = 12.sp,
                color = dateTextColor
            )
            
            // Events (Dot indicators or very small text)
            if (dayState.events.isNotEmpty() || dayState.icalEvents.isNotEmpty()) {
                Row(horizontalArrangement = Arrangement.Center, modifier = Modifier.fillMaxWidth()) {
                    repeat(dayState.events.size) {
                        Box(modifier = Modifier.size(4.dp).background(MaterialTheme.colorScheme.primary, CircleShape))
                        Spacer(modifier = Modifier.width(1.dp))
                    }
                    repeat(dayState.icalEvents.size) {
                        Box(modifier = Modifier.size(4.dp).background(Color.Cyan, CircleShape))
                        Spacer(modifier = Modifier.width(1.dp))
                    }
                }
            }

            // Income / Expense
            val income = dayState.transactions.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
            val expense = dayState.transactions.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }

            if (income > 0) {
                Text("収+%,d".format(income), color = Color(0xFF2E7D32), fontSize = 7.sp, maxLines = 1, lineHeight = 8.sp)
            }
            if (expense > 0) {
                Text("支-%,d".format(expense), color = Color(0xFFC62828), fontSize = 7.sp, maxLines = 1, lineHeight = 8.sp)
            }

            Spacer(modifier = Modifier.weight(1f))

            // Prediction / Goal Difference
            if (predictionDiff != null) {
                val prefix = if (predictionDiff >= 0) "余" else "不"
                val color = if (predictionDiff >= 0) Color(0xFF1B5E20) else Color(0xFFB71C1C)
                Text(
                    text = "$prefix%,d".format(predictionDiff),
                    fontSize = 9.sp,
                    color = color,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    textAlign = TextAlign.Center
                )
            } else if (dayState.goal != null) {
                 val diff = dayState.balance - dayState.goal.amount
                 Text(
                    text = "残%,d".format(diff), // Current Balance - Goal (negative usually)
                     fontSize = 8.sp,
                     color = if (cardColor.luminance() > 0.5f) Color.Black else Color.White,
                     maxLines = 1
                 )
            }
        }
    }
}

private fun getGradientColor(difference: Long, totalGoal: Long): Color {
    val green = Color(0xFFA5D6A7) // Light Green
    val yellow = Color(0xFFFFF59D) // Light Yellow
    val red = Color(0xFFEF9A9A)   // Light Red

    if (totalGoal <= 0L) {
        return if (difference >= 0) green else red
    }

    //黒字の場合は緑
    if (difference >= 0) {
        return green
    }

    // 差額がマイナスの場合、目標額に対する割合を計算
    // 0% (目標達成) -> 黄色, -100% (目標の2倍の赤字) -> 赤
    val fraction = (-difference).toFloat() / totalGoal.toFloat()

    return lerp(yellow, red, fraction.coerceIn(0f, 1f))
}
