package io.github.stupidgame.curyendar

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import io.github.stupidgame.curyendar.data.CalendarViewModel
import io.github.stupidgame.curyendar.data.DetailViewModel
import io.github.stupidgame.curyendar.data.DetailViewModelFactory
import io.github.stupidgame.curyendar.data.Expense
import io.github.stupidgame.curyendar.data.Goal
import io.github.stupidgame.curyendar.data.Income
import io.github.stupidgame.curyendar.ui.theme.CuryendarTheme
import java.util.Calendar
import java.util.Locale

class MainActivity : ComponentActivity() {
    private val openDocumentLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            contentResolver.openInputStream(it)?.use {
                // TODO
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CuryendarTheme {
                CuryendarApp(onImport = { openDocumentLauncher.launch(arrayOf("text/calendar")) })
            }
        }
    }
}

@Composable
fun CuryendarApp(onImport: () -> Unit) {
    val navController = rememberNavController()
    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = "calendar",
            modifier = Modifier.padding(innerPadding)
        ) {
            composable("calendar") {
                CalendarScreen(navController = navController, onImport = onImport)
            }
            composable("detail/{year}/{month}/{day}") { backStackEntry ->
                val year = backStackEntry.arguments?.getString("year")?.toInt() ?: 0
                val month = backStackEntry.arguments?.getString("month")?.toInt() ?: 0
                val day = backStackEntry.arguments?.getString("day")?.toInt() ?: 0
                DetailScreen(year = year, month = month, day = day)
            }
        }
    }
}

@Composable
fun CalendarScreen(
    navController: NavController, onImport: () -> Unit, modifier: Modifier = Modifier,
    viewModel: CalendarViewModel = viewModel()
) {
    val calendar = Calendar.getInstance()
    var year by remember { mutableIntStateOf(calendar.get(Calendar.YEAR)) }
    var month by remember { mutableIntStateOf(calendar.get(Calendar.MONTH)) }
    var url by remember { mutableStateOf("") }
    val context = LocalContext.current

    Column(modifier = modifier.background(Color.White)) {
        CalendarHeader(
            year = year,
            month = month,
            onPrevMonth = {
                if (month == 0) {
                    month = 11
                    year--
                } else {
                    month--
                }
            },
            onNextMonth = {
                if (month == 11) {
                    month = 0
                    year++
                } else {
                    month++
                }
            }
        )
        Row {
            Button(onClick = onImport) {
                Text("iCalファイルをインポート")
            }
            TextField(value = url, onValueChange = { url = it }, label = { Text("WebcalのURL") })
            Button(onClick = {
                viewModel.importWebcal(url) {
                    Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
                }
            }) {
                Text("Webcalをインポート")
            }
        }
        CalendarGrid(year = year, month = month, navController = navController, viewModel = viewModel)
    }
}

@Composable
fun CalendarHeader(
    year: Int,
    month: Int,
    onPrevMonth: () -> Unit,
    onNextMonth: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onPrevMonth) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Previous Month")
        }
        Text(text = String.format(Locale.getDefault(), "%d年 %d月", year, month + 1))
        IconButton(onClick = onNextMonth) {
            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Next Month")
        }
    }
}

@Composable
fun CalendarGrid(year: Int, month: Int, navController: NavController, viewModel: CalendarViewModel) {
    val calendar = Calendar.getInstance()
    calendar.set(year, month, 1)
    val daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
    val firstDayOfWeek = calendar.get(Calendar.DAY_OF_WEEK) - 1 // Sunday is 0

    val today = Calendar.getInstance()
    val currentYear = today.get(Calendar.YEAR)
    val currentMonth = today.get(Calendar.MONTH)
    val currentDay = today.get(Calendar.DAY_OF_MONTH)

    val holidays by viewModel.holidays.collectAsState(initial = emptyList())

    DayOfWeekHeader()

    LazyVerticalGrid(columns = GridCells.Fixed(7), modifier = Modifier.border(1.dp, Color.Gray)) {
        // Add empty cells for padding
        items(firstDayOfWeek) { }

        items(daysInMonth) { day ->
            val dayOfMonth = day + 1
            val dayOfWeek = (firstDayOfWeek + day) % 7
            val isToday = year == currentYear && month == currentMonth && dayOfMonth == currentDay
            val isHoliday = holidays.any { 
                val cal = Calendar.getInstance()
                cal.time = it.dateStart.value
                cal.get(Calendar.YEAR) == year && cal.get(Calendar.MONTH) == month && cal.get(Calendar.DAY_OF_MONTH) == dayOfMonth
             }

            val context = LocalContext.current
            val detailViewModel: DetailViewModel = viewModel(
                factory = DetailViewModelFactory(
                    (context.applicationContext as CuryendarApplication).database.curyendarDao(),
                    year, month, dayOfMonth
                )
            )
            val items by detailViewModel.items.collectAsState(initial = emptyList())

            val balance = items.sumOf {
                when(it) {
                    is Income -> it.amount
                    is Expense -> -it.amount
                    else -> 0.0
                }
            }
            val goal = items.find { it is Goal } as? Goal

            val color = when {
                isHoliday -> Color.Red
                dayOfWeek == 0 -> Color.Red // Sunday
                dayOfWeek == 6 -> Color.Blue // Saturday
                else -> Color.Black
            }
            DayCell(
                day = dayOfMonth,
                color = color,
                isToday = isToday,
                balance = balance,
                goal = goal,
                onClick = { navController.navigate("detail/$year/$month/$dayOfMonth") }
            )
        }
    }
}

@Composable
fun DayOfWeekHeader() {
    Row(modifier = Modifier.fillMaxWidth().border(1.dp, Color.Gray)) {
        val daysOfWeek = listOf("日", "月", "火", "水", "木", "金", "土")
        for (day in daysOfWeek) {
            Text(
                text = day,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun DayCell(
    day: Int,
    color: Color,
    isToday: Boolean,
    balance: Double,
    goal: Goal?,
    onClick: () -> Unit
) {
    val goalColor = goal?.let {
        when {
            balance >= it.amount -> Color.Green
            balance < 0 -> Color.Red
            else -> {
                val percentage = balance / it.amount
                Color(red = (1 - percentage).toFloat(), green = percentage.toFloat(), blue = 0f)
            }
        }
    } ?: color

    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .border(0.5.dp, Color.Gray)
            .clickable(onClick = onClick)
            .then(
                if (isToday) {
                    Modifier.border(1.dp, Color.Black, CircleShape)
                } else {
                    Modifier
                }
            ),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = day.toString(), color = goalColor, fontSize = 16.sp)
            if (balance != 0.0) {
                Text(text = "残: $balance", fontSize = 10.sp)
            }
            goal?.let {
                val percentage = (balance / it.amount * 100).toInt()
                Text(text = "$percentage%", fontSize = 10.sp)
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun CalendarScreenPreview() {
    CuryendarTheme {
        CuryendarApp(onImport = {})
    }
}
