package io.github.stupidgame.curyendar

import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
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
import io.github.stupidgame.curyendar.data.CalendarViewModelFactory
import io.github.stupidgame.curyendar.data.DayState
import io.github.stupidgame.curyendar.data.DetailViewModel
import io.github.stupidgame.curyendar.data.DetailViewModelFactory
import io.github.stupidgame.curyendar.ui.theme.CuryendarTheme
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId
import java.util.Calendar
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CuryendarTheme {
                CuryendarApp()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CuryendarApp() {
    val navController = rememberNavController()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val app = context.applicationContext as CuryendarApplication

    val calendar = Calendar.getInstance()
    var year by remember { mutableIntStateOf(calendar.get(Calendar.YEAR)) }
    var month by remember { mutableIntStateOf(calendar.get(Calendar.MONTH)) }

    val calendarViewModel: CalendarViewModel = viewModel(factory = CalendarViewModelFactory(app.database.curyendarDao()))

    LaunchedEffect(year, month) {
        calendarViewModel.loadMonth(year, month)
    }

    val openDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri: Uri? ->
        uri?.let {
            calendarViewModel.importIcs(it, context) { message ->
                Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            }
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Text("Curyendar", modifier = Modifier.padding(16.dp), style = MaterialTheme.typography.headlineSmall)
                Divider()
                NavigationDrawerItem(
                    label = { Text("Calendar") },
                    selected = true, // Calendar is the main screen
                    onClick = {
                        navController.navigate("calendar") {
                            popUpTo(navController.graph.startDestinationId)
                            launchSingleTop = true
                        }
                        scope.launch { drawerState.close() }
                    }
                )
                NavigationDrawerItem(
                    label = { Text("Settings") },
                    selected = false,
                    onClick = {
                        navController.navigate("settings")
                        scope.launch { drawerState.close() }
                    }
                )
            }
        }
    ) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = {
                TopAppBar(
                    title = {
                        Text(String.format(Locale.getDefault(), "%d年 %d月", year, month + 1))
                    },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Default.Menu, contentDescription = "Menu")
                        }
                    },
                    actions = {
                        IconButton(onClick = {
                            if (month == 0) {
                                month = 11
                                year--
                            } else {
                                month--
                            }
                        }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Previous Month")
                        }
                        IconButton(onClick = {
                            if (month == 11) {
                                month = 0
                                year++
                            } else {
                                month++
                            }
                        }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Next Month")
                        }
                    }
                )
            }
        ) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = "calendar",
                modifier = Modifier.padding(innerPadding)
            ) {
                composable("calendar") {
                    CalendarScreen(
                        navController = navController,
                        viewModel = calendarViewModel
                    )
                }
                composable("detail/{year}/{month}/{day}") { backStackEntry ->
                    val detailYear = backStackEntry.arguments?.getString("year")?.toInt() ?: 0
                    val detailMonth = backStackEntry.arguments?.getString("month")?.toInt() ?: 0
                    val detailDay = backStackEntry.arguments?.getString("day")?.toInt() ?: 0
                    RealDetailScreen(year = detailYear, month = detailMonth, day = detailDay)
                }
                composable("settings") {
                    SettingsScreen(onImport = { openDocumentLauncher.launch(arrayOf("text/calendar")) })
                }
            }
        }
    }
}

@Composable
fun CalendarScreen(
    navController: NavController,
    modifier: Modifier = Modifier,
    viewModel: CalendarViewModel
) {
    val uiState by viewModel.uiState.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 8.dp)
    ) {
        Spacer(modifier = Modifier.height(8.dp))
        if (uiState.dayStates.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            CalendarGrid(year = uiState.year, month = uiState.month, dayStates = uiState.dayStates, navController = navController)
        }
    }
}

@Composable
fun CalendarGrid(year: Int, month: Int, dayStates: Map<Int, DayState>, navController: NavController) {
    val calendar = Calendar.getInstance()
    calendar.set(year, month, 1)
    val daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
    val firstDayOfWeek = calendar.get(Calendar.DAY_OF_WEEK) - 1 // Sunday is 0
    val today = LocalDate.now()
    val goalDate = dayStates.values.firstOrNull { it.goal != null }?.let { LocalDate.of(year, month + 1, it.dayOfMonth) }

    DayOfWeekHeader()
    Spacer(modifier = Modifier.height(8.dp))

    LazyVerticalGrid(
        columns = GridCells.Fixed(7),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(firstDayOfWeek) { Box(modifier = Modifier.size(100.dp)) }

        items(daysInMonth) { day ->
            val dayOfMonth = day + 1
            val dayState = dayStates[dayOfMonth]
            if (dayState != null) {
                val date = LocalDate.of(year, month + 1, dayOfMonth)
                DayCell(
                    dayState = dayState,
                    date = date,
                    goalDate = goalDate,
                    onClick = { navController.navigate("detail/$year/$month/$dayOfMonth") }
                )
            }
        }
    }
}

// Japanese holiday determination logic
fun isJapaneseHoliday(date: LocalDate): Boolean {
    val year = date.year
    val month = date.monthValue
    val day = date.dayOfMonth

    // Fixed holidays
    if (month == 1 && day == 1) return true // New Year's Day
    if (month == 2 && day == 11) return true // National Foundation Day
    if (month == 2 && day == 23) return true // Emperor's Birthday
    if (month == 4 && day == 29) return true // Showa Day
    if (month == 5 && day == 3) return true // Constitution Memorial Day
    if (month == 5 && day == 4) return true // Greenery Day
    if (month == 5 && day == 5) return true // Children's Day
    if (month == 7 && date.dayOfWeek.value == 1 && day >= 15 && day <= 21) return true // Marine Day (3rd Monday of July)
    if (month == 8 && day == 11) return true // Mountain Day
    if (month == 9 && date.dayOfWeek.value == 1 && day >= 15 && day <= 21) return true // Respect for the Aged Day (3rd Monday of September)
    if (month == 10 && date.dayOfWeek.value == 1 && day >= 8 && day <= 14) return true // Sports Day (2nd Monday of October)
    if (month == 11 && day == 3) return true // Culture Day
    if (month == 11 && day == 23) return true // Labor Thanksgiving Day

    // Vernal Equinox Day and Autumnal Equinox Day (simplified)
    if (month == 3 && (day == 20 || day == 21)) return true
    if (month == 9 && (day == 22 || day == 23)) return true

    return false
}

@Composable
fun DayOfWeekHeader() {
    Row(modifier = Modifier.fillMaxWidth()) {
        val daysOfWeek = listOf("日", "月", "火", "水", "木", "金", "土")
        for ((index, day) in daysOfWeek.withIndex()) {
            val color = when (index) {
                0 -> Color.Red
                6 -> Color.Blue
                else -> MaterialTheme.colorScheme.onBackground
            }
            Text(
                text = day,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f),
                color = color,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun DayCell(
    dayState: DayState,
    date: LocalDate,
    goalDate: LocalDate?,
    onClick: () -> Unit
) {
    val today = LocalDate.now()
    val isHoliday = isJapaneseHoliday(date) || date.dayOfWeek.value == 7
    val percentage = dayState.goal?.let { if (it.amount > 0) (dayState.balance.toFloat() / it.amount.toFloat()) else 0f } ?: 0f
    val cardColor by animateColorAsState(
        targetValue = goalDate?.let {
            if (date.isBefore(today) || date.isAfter(it)) {
                MaterialTheme.colorScheme.surfaceVariant
            } else {
                when {
                    percentage >= 1f -> Color(0xFF1B5E20) // Dark Green
                    percentage >= 0.5f -> Color(0xFFF9A825) // Dark Yellow
                    else -> Color(0xFFB71C1C) // Dark Red
                }
            }
        } ?: MaterialTheme.colorScheme.surfaceVariant
    )

    var cellModifier = Modifier
        .fillMaxSize()
        .heightIn(min = 120.dp)
        .clickable(onClick = onClick)
    if (date.isEqual(today)) {
        cellModifier = cellModifier.border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(8.dp))
    }

    Card(
        modifier = cellModifier,
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = cardColor)
    ) {
        Column(
            modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = dayState.dayOfMonth.toString(),
                color = if (isHoliday) Color.Red else if (date.dayOfWeek.value == 6) Color.Blue else MaterialTheme.colorScheme.onSurface,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))

            if (goalDate != null && !date.isBefore(today) && !date.isAfter(goalDate)) {
                dayState.goal?.let {
                    val difference = dayState.balance - it.amount
                    Text(
                        text = if (difference >= 0) "+%,d".format(difference) else "%,d".format(difference),
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(text = "%.0f%%".format(percentage * 100), color = MaterialTheme.colorScheme.onSurface, fontSize = 10.sp)
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            dayState.icalEvents.forEach {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 2.dp)) {
                    Box(modifier = Modifier.size(4.dp).background(Color.Cyan, CircleShape))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = it.summary?.value ?: "", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp, maxLines = 1)
                }
            }
            dayState.events.forEach {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 2.dp)) {
                    Box(modifier = Modifier.size(4.dp).background(MaterialTheme.colorScheme.primary, CircleShape))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = it.title, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp, maxLines = 1)
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun CuryendarAppPreview() {
    CuryendarTheme {
        CuryendarApp()
    }
}
