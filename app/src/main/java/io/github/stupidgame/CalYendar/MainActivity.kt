package io.github.stupidgame.CalYendar

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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import io.github.stupidgame.CalYendar.data.CalendarViewModel
import io.github.stupidgame.CalYendar.data.CalendarViewModelFactory
import io.github.stupidgame.CalYendar.data.DayState
import io.github.stupidgame.CalYendar.data.DetailViewModel
import io.github.stupidgame.CalYendar.data.DetailViewModelFactory
import io.github.stupidgame.CalYendar.ui.theme.CalYendarTheme
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
    ) { isGranted: Boolean ->
        if (isGranted) {
            // Permission is granted. Continue the action or workflow in your
            // app.
        } else {
            // Explain to the user that the feature is unavailable because the
            // features requires a permission that the user has denied. At the
            // same time, respect the user's decision. Don't link to system
            // settings in an effort to convince the user to change their
            // decision.
        }
    }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when {
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED -> {
                    // You can use the API that requires the permission.
                }
                else -> {
                    // You can directly ask for the permission.
                    // The registered ActivityResultCallback gets the result of this request.
                    requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        }
    }

    val calendar = Calendar.getInstance()
    var year by remember { mutableIntStateOf(calendar.get(Calendar.YEAR)) }
    var month by remember { mutableIntStateOf(calendar.get(Calendar.MONTH)) }

    val calendarViewModel: CalendarViewModel = viewModel(factory = CalendarViewModelFactory(app.database.calyendarDao()))

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

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Text(stringResource(R.string.app_name), modifier = Modifier.padding(16.dp), style = MaterialTheme.typography.headlineSmall)
                Divider()
                NavigationDrawerItem(
                    label = { Text("Calendar") },
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
                    label = { Text("Settings") },
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
                    currentRoute == "settings" -> "Settings"
                    else -> String.format(Locale.getDefault(), "%d年 %d月", year, month + 1)
                }

                TopAppBar(
                    title = { Text(title) },
                    navigationIcon = {
                        if (currentRoute == "calendar") {
                            IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                Icon(Icons.Default.Menu, contentDescription = "Menu")
                            }
                        } else {
                            IconButton(onClick = { navController.popBackStack("calendar", false) }) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
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
                        } else if (currentRoute?.startsWith("detail") == true) {
                            val args = navBackStackEntry?.arguments
                            val dYear = args?.getString("year")?.toInt() ?: year
                            val dMonth = args?.getString("month")?.toInt() ?: month
                            val dDay = args?.getString("day")?.toInt() ?: 1

                            IconButton(onClick = {
                                val cal = Calendar.getInstance()
                                cal.set(dYear, dMonth, dDay)
                                cal.add(Calendar.DAY_OF_MONTH, -1)
                                val newYear = cal.get(Calendar.YEAR)
                                val newMonth = cal.get(Calendar.MONTH)
                                val newDay = cal.get(Calendar.DAY_OF_MONTH)
                                year = newYear
                                month = newMonth
                                navController.navigate("detail/$newYear/$newMonth/$newDay") {
                                    popUpTo("calendar") {}
                                }
                            }) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Previous Day")
                            }
                            IconButton(onClick = {
                                val cal = Calendar.getInstance()
                                cal.set(dYear, dMonth, dDay)
                                cal.add(Calendar.DAY_OF_MONTH, 1)
                                val newYear = cal.get(Calendar.YEAR)
                                val newMonth = cal.get(Calendar.MONTH)
                                val newDay = cal.get(Calendar.DAY_OF_MONTH)
                                year = newYear
                                month = newMonth
                                navController.navigate("detail/$newYear/$newMonth/$newDay") {
                                    popUpTo("calendar") {}
                                }
                            }) {
                                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Next Day")
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
                        onImportIcsClick = { openDocumentLauncher.launch(arrayOf("text/calendar")) }
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
