package io.github.stupidgame.curyendar

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.stupidgame.curyendar.data.CalendarViewModel
import io.github.stupidgame.curyendar.data.CalendarViewModelFactory
import io.github.stupidgame.curyendar.data.DayState
import io.github.stupidgame.curyendar.data.TransactionType
import java.text.DateFormatSymbols
import java.util.Calendar
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen() {
    val context = LocalContext.current
    val viewModel: CalendarViewModel = viewModel(
        factory = CalendarViewModelFactory(
            (context.applicationContext as CuryendarApplication).database.curyendarDao()
        )
    )
    val uiState by viewModel.uiState.collectAsState()

    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let {
            viewModel.importIcs(it, context) {
                Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Initial load
    LaunchedEffect(Unit) {
        val calendar = Calendar.getInstance()
        viewModel.loadMonth(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH))
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { importLauncher.launch(arrayOf("text/calendar")) }) {
                Icon(Icons.Default.Share, contentDescription = "Import iCal")
            }
        }
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding)) {
            MonthNavigator(uiState.year, uiState.month, onPrevious = {
                val prevCalendar = Calendar.getInstance().apply {
                    set(uiState.year, uiState.month, 1)
                    add(Calendar.MONTH, -1)
                }
                viewModel.loadMonth(prevCalendar.get(Calendar.YEAR), prevCalendar.get(Calendar.MONTH))
            }, onNext = {
                val nextCalendar = Calendar.getInstance().apply {
                    set(uiState.year, uiState.month, 1)
                    add(Calendar.MONTH, 1)
                }
                viewModel.loadMonth(nextCalendar.get(Calendar.YEAR), nextCalendar.get(Calendar.MONTH))
            })

            WeekdaysHeader()

            val calendar = Calendar.getInstance().apply {
                set(uiState.year, uiState.month, 1)
            }
            val firstDayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
            val emptyCells = (firstDayOfWeek - Calendar.SUNDAY + 7) % 7

            LazyVerticalGrid(columns = GridCells.Fixed(7)) {
                items(emptyCells) {
                    Box(modifier = Modifier.aspectRatio(1f))
                }
                items(uiState.dayStates.values.toList()) { day ->
                    DayCell(day)
                }
            }
        }
    }

}

@Composable
fun MonthNavigator(
    year: Int,
    month: Int,
    onPrevious: () -> Unit,
    onNext: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onPrevious) {
            Icon(Icons.Default.ArrowBack, contentDescription = "Previous Month")
        }
        Text(
            text = String.format(Locale.JAPAN, "%d年 %d月", year, month + 1),
            style = MaterialTheme.typography.headlineMedium
        )
        IconButton(onClick = onNext) {
            Icon(Icons.Default.ArrowForward, contentDescription = "Next Month")
        }
    }
}

@Composable
fun WeekdaysHeader() {
    val weekdays = DateFormatSymbols(Locale.JAPAN).shortWeekdays
    Row(modifier = Modifier.fillMaxWidth()) {
        for (i in Calendar.SUNDAY..Calendar.SATURDAY) {
            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(text = weekdays[i], fontSize = 12.sp)
            }
        }
    }
}

@Composable
fun DayCell(dayState: DayState) {
    val goal = dayState.goal
    val balance = dayState.balance
    val percentage = if (goal != null && goal.amount > 0) (balance.toFloat() / goal.amount.toFloat()) else 0f
    val cardColor = when {
        goal == null -> MaterialTheme.colorScheme.surface
        percentage >= 1f -> Color(0xFF66BB6A).copy(alpha = 0.3f)
        else -> Color(0xFFEF5350).copy(alpha = 0.3f)
    }

    Card(
        modifier = Modifier
            .padding(2.dp)
            .aspectRatio(1f),
        colors = CardDefaults.cardColors(containerColor = cardColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(4.dp)
        ) {
            Text(
                text = dayState.dayOfMonth.toString(),
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(2.dp))

            dayState.events.take(1).forEach {
                Text(it.title, fontSize = 8.sp, maxLines = 1, color = MaterialTheme.colorScheme.onSurface)
            }
            dayState.icalEvents.take(1).forEach {
                Text(it.summary.value, fontSize = 8.sp, maxLines = 1, color = MaterialTheme.colorScheme.onSurface)
            }

            val income = dayState.transactions.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
            val expense = dayState.transactions.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }

            if (income > 0) {
                Text("+%,d".format(income), color = Color(0xFF2E7D32), fontSize = 8.sp, maxLines = 1)
            }
            if (expense > 0) {
                Text("-%,d".format(expense), color = Color(0xFFC62828), fontSize = 8.sp, maxLines = 1)
            }

            if (goal != null) {
                val difference = balance - goal.amount
                Text(
                    text = if (difference >= 0) "達成!" else "あと%,d".format(-difference),
                    fontSize = 8.sp,
                    color = if (difference >= 0) Color(0xFF66BB6A) else Color.Gray,
                    maxLines = 1
                )
            }
        }
    }
}
