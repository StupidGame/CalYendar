package io.github.stupidgame.calyendar

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Menu
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import io.github.stupidgame.calyendar.data.CalendarViewModel
import io.github.stupidgame.calyendar.data.CalendarViewModelFactory
import io.github.stupidgame.calyendar.data.SettingsViewModel
import io.github.stupidgame.calyendar.data.SettingsViewModelFactory
import io.github.stupidgame.calyendar.ui.theme.CalYendarTheme
import io.github.stupidgame.calyendar.utils.EventNotificationManager
import java.time.LocalDate
import kotlinx.coroutines.launch

private object AppRoute {
    const val CALENDAR = "calendar"
    const val SETTINGS = "settings"
    const val DETAIL_PATTERN = "detail/{year}/{month}/{day}"

    fun detail(year: Int, month: Int, day: Int): String = "detail/$year/$month/$day"
}

private data class CalendarMonthSelection(val year: Int, val month: Int) {
    fun shiftBy(months: Long): CalendarMonthSelection {
        val shifted = LocalDate.of(year, month + 1, 1).plusMonths(months)
        return CalendarMonthSelection(shifted.year, shifted.monthValue - 1)
    }

    fun title(): String = "$year/${month + 1}"
}

private data class DetailDateSelection(val year: Int, val month: Int, val day: Int) {
    fun shiftBy(days: Long): DetailDateSelection {
        val shifted = LocalDate.of(year, month + 1, day).plusDays(days)
        return from(shifted)
    }

    fun title(): String = "$year/${month + 1}/$day"

    companion object {
        fun from(date: LocalDate): DetailDateSelection =
            DetailDateSelection(date.year, date.monthValue - 1, date.dayOfMonth)
    }
}

private fun Bundle?.toDetailDateSelection(
    fallback: CalendarMonthSelection
): DetailDateSelection =
    DetailDateSelection(
        year = this?.getString("year")?.toIntOrNull() ?: fallback.year,
        month = this?.getString("month")?.toIntOrNull() ?: fallback.month,
        day = this?.getString("day")?.toIntOrNull() ?: 1
    )

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CalYendarTheme {
                CalYendarApp()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalYendarApp() {
    val navController = rememberNavController()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val app = context.applicationContext as CalYendarApplication

    val requestPermissionLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission()
        ) {}

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    var monthSelection by
        remember {
            mutableStateOf(
                LocalDate.now().let { today ->
                    CalendarMonthSelection(today.year, today.monthValue - 1)
                }
            )
        }

    val calendarViewModel: CalendarViewModel =
        viewModel(factory = CalendarViewModelFactory(app.repository))
    val settingsViewModel: SettingsViewModel =
        viewModel(
            factory =
                SettingsViewModelFactory(
                    app.repository,
                    app.appSettingsStore,
                    EventNotificationManager(app)
                )
        )

    LaunchedEffect(monthSelection.year, monthSelection.month) {
        calendarViewModel.loadMonth(monthSelection.year, monthSelection.month)
    }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val detailSelection = navBackStackEntry?.arguments.toDetailDateSelection(monthSelection)

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Text(
                    stringResource(R.string.app_name),
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.headlineSmall
                )
                Divider()
                NavigationDrawerItem(
                    label = { Text("Calendar") },
                    selected = currentRoute == AppRoute.CALENDAR,
                    onClick = {
                        navController.navigate(AppRoute.CALENDAR) {
                            popUpTo(navController.graph.startDestinationId)
                            launchSingleTop = true
                        }
                        scope.launch { drawerState.close() }
                    }
                )
                NavigationDrawerItem(
                    label = { Text("Settings") },
                    selected = currentRoute == AppRoute.SETTINGS,
                    onClick = {
                        navController.navigate(AppRoute.SETTINGS) {
                            launchSingleTop = true
                        }
                        scope.launch { drawerState.close() }
                    }
                )
            }
        }
    ) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = {
                val title =
                    when {
                        currentRoute?.startsWith("detail") == true -> detailSelection.title()
                        currentRoute == AppRoute.SETTINGS -> "Settings"
                        else -> monthSelection.title()
                    }

                TopAppBar(
                    title = { Text(title) },
                    navigationIcon = {
                        if (currentRoute == AppRoute.CALENDAR) {
                            IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                Icon(Icons.Default.Menu, contentDescription = "Open menu")
                            }
                        } else {
                            IconButton(
                                onClick = { navController.popBackStack(AppRoute.CALENDAR, false) }
                            ) {
                                Icon(
                                    Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back"
                                )
                            }
                        }
                    },
                    actions = {
                        when {
                            currentRoute == AppRoute.CALENDAR -> {
                                IconButton(
                                    onClick = {
                                        monthSelection = monthSelection.shiftBy(-1L)
                                    }
                                ) {
                                    Icon(
                                        Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = "Previous month"
                                    )
                                }
                                IconButton(
                                    onClick = {
                                        monthSelection = monthSelection.shiftBy(1L)
                                    }
                                ) {
                                    Icon(
                                        Icons.AutoMirrored.Filled.ArrowForward,
                                        contentDescription = "Next month"
                                    )
                                }
                            }

                            currentRoute?.startsWith("detail") == true -> {
                                IconButton(
                                    onClick = {
                                        val previousDate = detailSelection.shiftBy(-1L)
                                        monthSelection =
                                            CalendarMonthSelection(
                                                previousDate.year,
                                                previousDate.month
                                            )
                                        navController.navigate(
                                            AppRoute.detail(
                                                previousDate.year,
                                                previousDate.month,
                                                previousDate.day
                                            )
                                        ) {
                                            popUpTo(AppRoute.CALENDAR) {}
                                        }
                                    }
                                ) {
                                    Icon(
                                        Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = "Previous day"
                                    )
                                }
                                IconButton(
                                    onClick = {
                                        val nextDate = detailSelection.shiftBy(1L)
                                        monthSelection =
                                            CalendarMonthSelection(nextDate.year, nextDate.month)
                                        navController.navigate(
                                            AppRoute.detail(
                                                nextDate.year,
                                                nextDate.month,
                                                nextDate.day
                                            )
                                        ) {
                                            popUpTo(AppRoute.CALENDAR) {}
                                        }
                                    }
                                ) {
                                    Icon(
                                        Icons.AutoMirrored.Filled.ArrowForward,
                                        contentDescription = "Next day"
                                    )
                                }
                            }
                        }
                    }
                )
            }
        ) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = AppRoute.CALENDAR,
                modifier = Modifier.padding(innerPadding)
            ) {
                composable(AppRoute.CALENDAR) {
                    CalendarScreen(
                        viewModel = calendarViewModel,
                        onDayClick = { day ->
                            navController.navigate(
                                AppRoute.detail(monthSelection.year, monthSelection.month, day)
                            )
                        }
                    )
                }
                composable(AppRoute.DETAIL_PATTERN) { backStackEntry ->
                    val selectedDate =
                        backStackEntry.arguments.toDetailDateSelection(monthSelection)
                    RealDetailScreen(
                        year = selectedDate.year,
                        month = selectedDate.month,
                        day = selectedDate.day
                    )
                }
                composable(AppRoute.SETTINGS) {
                    SettingsScreen(
                        calendarViewModel = calendarViewModel,
                        settingsViewModel = settingsViewModel
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun CalYendarAppPreview() {
    CalYendarTheme {
        CalYendarApp()
    }
}
