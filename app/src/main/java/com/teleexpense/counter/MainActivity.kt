package com.teleexpense.counter

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.teleexpense.counter.ui.screens.analytics.AnalyticsScreen
import com.teleexpense.counter.ui.screens.calendar.CalendarScreen
import com.teleexpense.counter.ui.screens.detail.TransactionDetailScreen
import com.teleexpense.counter.ui.screens.home.HomeScreen
import com.teleexpense.counter.ui.screens.onboarding.OnboardingScreen
import com.teleexpense.counter.ui.screens.settings.SettingsScreen
import com.teleexpense.counter.ui.screens.transactions.TransactionsScreen
import com.teleexpense.counter.ui.theme.TeleExpenseTheme
import com.teleexpense.counter.ui.viewmodel.MainViewModel
import com.teleexpense.counter.ui.viewmodel.MainViewModelFactory

class MainActivity : ComponentActivity() {

    private val requestPermission = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { /* handled in UI */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            TeleExpenseTheme {
                val app = application as TeleExpenseApp
                val viewModel: MainViewModel = viewModel(
                    factory = MainViewModelFactory(app.repository)
                )
                val onboardingDone by viewModel.onboardingDone.collectAsState()
                val hasSmsPermission = remember {
                    ContextCompat.checkSelfPermission(this, Manifest.permission.READ_SMS) ==
                            PackageManager.PERMISSION_GRANTED
                }

                if (!onboardingDone) {
                    OnboardingScreen(
                        hasPermission = hasSmsPermission,
                        onRequestPermission = {
                            requestPermission.launch(
                                arrayOf(
                                    Manifest.permission.READ_SMS,
                                    Manifest.permission.RECEIVE_SMS
                                )
                            )
                        },
                        onScanThisMonth = { viewModel.scanThisMonth() },
                        onStartFromToday = { viewModel.startFromToday() },
                        scanState = viewModel.scanState.collectAsState().value
                    )
                } else {
                    MainScaffold(viewModel)
                }
            }
        }
    }
}

sealed class Screen(val route: String, val label: String, val icon: ImageVector, val selectedIcon: ImageVector) {
    data object Home : Screen("home", "Home", Icons.Outlined.Home, Icons.Filled.Home)
    data object Transactions : Screen("transactions", "Transactions", Icons.Outlined.Receipt, Icons.Filled.Receipt)
    data object Calendar : Screen("calendar", "Calendar", Icons.Outlined.CalendarMonth, Icons.Filled.CalendarMonth)
    data object Analytics : Screen("analytics", "Analytics", Icons.Outlined.PieChart, Icons.Filled.PieChart)
    data object Settings : Screen("settings", "Settings", Icons.Outlined.Settings, Icons.Filled.Settings)
}

@Composable
fun MainScaffold(viewModel: MainViewModel) {
    val navController = rememberNavController()
    val items = listOf(Screen.Home, Screen.Transactions, Screen.Calendar, Screen.Analytics, Screen.Settings)
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            if (currentRoute?.startsWith("detail") != true) {
                NavigationBar {
                    items.forEach { screen ->
                        val selected = currentRoute == screen.route
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                navController.navigate(screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = {
                                Icon(
                                    if (selected) screen.selectedIcon else screen.icon,
                                    contentDescription = screen.label
                                )
                            },
                            label = { Text(screen.label) }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Home.route) {
                HomeScreen(viewModel = viewModel, onTransactionClick = { id ->
                    navController.navigate("detail/$id")
                })
            }
            composable(Screen.Transactions.route) {
                TransactionsScreen(viewModel = viewModel, onTransactionClick = { id ->
                    navController.navigate("detail/$id")
                })
            }
            composable(Screen.Calendar.route) {
                CalendarScreen(viewModel = viewModel, onDayClick = { /* handled inside */ })
            }
            composable(Screen.Analytics.route) {
                AnalyticsScreen(viewModel = viewModel)
            }
            composable(Screen.Settings.route) {
                SettingsScreen(viewModel = viewModel)
            }
            composable("detail/{txId}") { backStack ->
                val txId = backStack.arguments?.getString("txId") ?: return@composable
                TransactionDetailScreen(
                    viewModel = viewModel,
                    transactionId = txId,
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}
