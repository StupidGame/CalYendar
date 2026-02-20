package io.github.stupidgame.calyendar

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import io.github.stupidgame.calyendar.data.CalendarViewModel
import io.github.stupidgame.calyendar.data.CalendarViewModelFactory
import io.github.stupidgame.calyendar.ui.theme.CalYendarTheme
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.util.Locale

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

    val requestPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean -> }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    val today = LocalDate.now()
    var year by remember { mutableIntStateOf(today.year) }
    var month by remember { mutableIntStateOf(today.monthValue - 1) }

    val calendarViewModel: CalendarViewModel = viewModel(factory = CalendarViewModelFactory(app.repository))

    LaunchedEffect(year, month) {
        calendarViewModel.loadMonth(year, month)
    }

    var isImportIcsAsHoliday by remember { mutableStateOf(false) }
    val openDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri: Uri? ->
        uri?.let {
            calendarViewModel.importIcs(it, context.contentResolver, isImportIcsAsHoliday) { message ->
                Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            }
        }
    }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Text(stringResource(R.string.app_name), modifier = Modifier.padding(16.dp), style = MaterialTheme.typography.headlineSmall)
                Divider()
                NavigationDrawerItem(
                    label = { Text("カレンダー") },
                    selected = currentRoute == "calendar",
                    onClick = {
                        navController.navigate("calendar") {
                            popUpTo(navController.graph.startDestinationId)
                            launchSingleTop = true
                        }
                        scope.launch { drawerState.close() }
                    }
                )
                NavigationDrawerItem(
                    label = { Text("設定") },
                    selected = currentRoute == "settings",
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
                val title = when {
                    currentRoute?.startsWith("detail") == true -> {
                        val args = navBackStackEntry?.arguments
                        val dYear = args?.getString("year")?.toInt() ?: year
                        val dMonth = args?.getString("month")?.toInt() ?: month
                        val dDay = args?.getString("day")?.toInt() ?: 1
                        "${dYear}年 ${dMonth + 1}月 ${dDay}日"
                    }
                    currentRoute == "settings" -> "設定"
                    else -> String.format(Locale.getDefault(), "%d年 %d月", year, month + 1)
                }

                TopAppBar(
                    title = { Text(title) },
                    navigationIcon = {
                        if (currentRoute == "calendar") {
                            IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                Icon(Icons.Default.Menu, contentDescription = "メニュー")
                            }
                        } else {
                            IconButton(onClick = { navController.popBackStack("calendar", false) }) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "戻る")
                            }
                        }
                    },
                    actions = {
                        if (currentRoute == "calendar") {
                            IconButton(onClick = {
                                if (month == 0) {
                                    month = 11
                                    year--
                                } else {
                                    month--
                                }
                            }) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "前の月")
                            }
                            IconButton(onClick = {
                                if (month == 11) {
                                    month = 0
                                    year++
                                } else {
                                    month++
                                }
                            }) {
                                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "次の月")
                            }
                        } else if (currentRoute?.startsWith("detail") == true) {
                            val args = navBackStackEntry?.arguments
                            val dYear = args?.getString("year")?.toInt() ?: year
                            val dMonth = args?.getString("month")?.toInt() ?: month
                            val dDay = args?.getString("day")?.toInt() ?: 1

                            IconButton(onClick = {
                                val currentData = LocalDate.of(dYear, dMonth + 1, dDay).minusDays(1)
                                val newYear = currentData.year
                                val newMonth = currentData.monthValue - 1
                                val newDay = currentData.dayOfMonth
                                year = newYear
                                month = newMonth
                                navController.navigate("detail/$newYear/$newMonth/$newDay") {
                                    popUpTo("calendar") {}
                                }
                            }) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "前の日")
                            }
                            IconButton(onClick = {
                                val currentData = LocalDate.of(dYear, dMonth + 1, dDay).plusDays(1)
                                val newYear = currentData.year
                                val newMonth = currentData.monthValue - 1
                                val newDay = currentData.dayOfMonth
                                year = newYear
                                month = newMonth
                                navController.navigate("detail/$newYear/$newMonth/$newDay") {
                                    popUpTo("calendar") {}
                                }
                            }) {
                                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "次の日")
                            }
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
                        viewModel = calendarViewModel,
                        onDayClick = { day ->
                            navController.navigate("detail/$year/$month/$day")
                        }
                    )
                }
                composable("detail/{year}/{month}/{day}") { backStackEntry ->
                    val detailYear = backStackEntry.arguments?.getString("year")?.toInt() ?: 0
                    val detailMonth = backStackEntry.arguments?.getString("month")?.toInt() ?: 0
                    val detailDay = backStackEntry.arguments?.getString("day")?.toInt() ?: 0
                    RealDetailScreen(year = detailYear, month = detailMonth, day = detailDay)
                }
                composable("settings") {
                    SettingsScreen(
                        calendarViewModel = calendarViewModel,
                        onImportIcsClick = { isHoliday ->
                            isImportIcsAsHoliday = isHoliday
                            openDocumentLauncher.launch(arrayOf("text/calendar"))
                        }
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
