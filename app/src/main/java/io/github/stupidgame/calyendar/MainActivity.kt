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
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Today
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
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
import java.time.LocalDate

private object AppRoute {
    const val CALENDAR = "calendar"
    const val SETTINGS = "settings"
    const val DETAIL = "detail/{year}/{month}/{day}"

    fun detail(date: LocalDate): String = "detail/${date.year}/${date.monthValue - 1}/${date.dayOfMonth}"
}

private fun Bundle?.selectedDate(fallback: LocalDate): LocalDate {
    val year = this?.getString("year")?.toIntOrNull() ?: fallback.year
    val month = this?.getString("month")?.toIntOrNull()?.plus(1) ?: fallback.monthValue
    val day = this?.getString("day")?.toIntOrNull() ?: fallback.dayOfMonth
    return runCatching { LocalDate.of(year, month, day) }.getOrDefault(fallback)
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { CalYendarTheme { CalYendarApp() } }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalYendarApp() {
    val context = LocalContext.current
    val application = context.applicationContext as CalYendarApplication
    val navController = rememberNavController()
    val today = LocalDate.now()
    var selectedYear by rememberSaveable { mutableIntStateOf(today.year) }
    var selectedMonth by rememberSaveable { mutableIntStateOf(today.monthValue) }

    fun showMonth(date: LocalDate) {
        selectedYear = date.year
        selectedMonth = date.monthValue
    }

    val calendarViewModel: CalendarViewModel = viewModel(factory = CalendarViewModelFactory(application.repository))
    val settingsViewModel: SettingsViewModel = viewModel(
        factory = SettingsViewModelFactory(application.appSettingsStore, application.userDataBackupService)
    )

    LaunchedEffect(selectedYear, selectedMonth) {
        calendarViewModel.loadMonth(selectedYear, selectedMonth - 1)
    }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    val backStackEntry by navController.currentBackStackEntryAsState()
    val route = backStackEntry?.destination?.route ?: AppRoute.CALENDAR
    val fallbackDate = LocalDate.of(selectedYear, selectedMonth, 1)
    val detailDate = backStackEntry?.arguments.selectedDate(fallbackDate)

    fun shiftDetail(days: Long) {
        val date = detailDate.plusDays(days)
        showMonth(date)
        navController.navigate(AppRoute.detail(date)) { popUpTo(AppRoute.CALENDAR) }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        when (route) {
                            AppRoute.SETTINGS -> "設定"
                            AppRoute.DETAIL -> "${detailDate.year}年${detailDate.monthValue}月${detailDate.dayOfMonth}日"
                            else -> "${selectedYear}年${selectedMonth}月"
                        }
                    )
                },
                navigationIcon = {
                    if (route != AppRoute.CALENDAR) {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "戻る")
                        }
                    }
                },
                actions = {
                    when (route) {
                        AppRoute.CALENDAR -> {
                            IconButton(onClick = { showMonth(LocalDate.of(selectedYear, selectedMonth, 1).minusMonths(1)) }) {
                                Icon(Icons.Filled.ChevronLeft, contentDescription = "前の月")
                            }
                            IconButton(onClick = { showMonth(LocalDate.now()) }) {
                                Icon(Icons.Outlined.Today, contentDescription = "今月に戻る")
                            }
                            IconButton(onClick = { showMonth(LocalDate.of(selectedYear, selectedMonth, 1).plusMonths(1)) }) {
                                Icon(Icons.Filled.ChevronRight, contentDescription = "次の月")
                            }
                            IconButton(onClick = { navController.navigate(AppRoute.SETTINGS) { launchSingleTop = true } }) {
                                Icon(Icons.Outlined.Settings, contentDescription = "設定")
                            }
                        }
                        AppRoute.DETAIL -> {
                            IconButton(onClick = { shiftDetail(-1) }) {
                                Icon(Icons.Filled.ChevronLeft, contentDescription = "前の日")
                            }
                            IconButton(onClick = { shiftDetail(1) }) {
                                Icon(Icons.Filled.ChevronRight, contentDescription = "次の日")
                            }
                        }
                    }
                }
            )
        }
    ) { padding ->
        NavHost(navController, startDestination = AppRoute.CALENDAR, modifier = Modifier.padding(padding)) {
            composable(AppRoute.CALENDAR) {
                CalendarScreen(calendarViewModel, selectedYear, selectedMonth - 1) { day ->
                    val date = LocalDate.of(selectedYear, selectedMonth, day)
                    navController.navigate(AppRoute.detail(date))
                }
            }
            composable(AppRoute.DETAIL) { entry ->
                val date = entry.arguments.selectedDate(fallbackDate)
                RealDetailScreen(date.year, date.monthValue - 1, date.dayOfMonth)
            }
            composable(AppRoute.SETTINGS) {
                SettingsScreen(calendarViewModel, settingsViewModel)
            }
        }
    }
}
