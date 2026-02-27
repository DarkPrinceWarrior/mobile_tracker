package com.example.mobile_tracker.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.mobile_tracker.presentation.binding.issue.IssueScreen
import com.example.mobile_tracker.presentation.binding.return_device.ReturnScreen
import com.example.mobile_tracker.presentation.context_selection.ContextSelectionScreen
import com.example.mobile_tracker.presentation.devices.DeviceListScreen
import com.example.mobile_tracker.presentation.employees.EmployeeSearchScreen
import com.example.mobile_tracker.presentation.home.HomeScreen
import com.example.mobile_tracker.presentation.login.LoginScreen
import com.example.mobile_tracker.presentation.journal.JournalScreen
import com.example.mobile_tracker.presentation.settings.SettingsScreen
import com.example.mobile_tracker.presentation.summary.SummaryScreen
import com.example.mobile_tracker.presentation.upload.UploadScreen

@Composable
fun AppNavGraph(
    navController: NavHostController,
    startDestination: Route,
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
    ) {
        composable<Route.Login> {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate(Route.ContextSelection) {
                        popUpTo(Route.Login) { inclusive = true }
                    }
                },
            )
        }

        composable<Route.ContextSelection> {
            ContextSelectionScreen(
                onContextSelected = {
                    navController.navigate(Route.Home) {
                        popUpTo(Route.ContextSelection) {
                            inclusive = true
                        }
                    }
                },
            )
        }

        composable<Route.Home> {
            HomeScreen(
                onLogout = {
                    navController.navigate(Route.Login) {
                        popUpTo(Route.Home) { inclusive = true }
                    }
                },
                onNavigateToDevices = {
                    navController.navigate(Route.DeviceList)
                },
                onNavigateToEmployees = {
                    navController.navigate(Route.EmployeeSearch)
                },
                onNavigateToIssue = {
                    navController.navigate(Route.Issue)
                },
                onNavigateToReturn = {
                    navController.navigate(Route.Return)
                },
                onNavigateToJournal = {
                    navController.navigate(Route.Journal)
                },
                onNavigateToSummary = {
                    navController.navigate(Route.Summary)
                },
                onNavigateToSettings = {
                    navController.navigate(Route.Settings)
                },
            )
        }

        composable<Route.DeviceList> {
            DeviceListScreen(
                onBack = { navController.popBackStack() },
            )
        }

        composable<Route.EmployeeSearch> {
            EmployeeSearchScreen(
                onBack = { navController.popBackStack() },
            )
        }

        composable<Route.Issue> {
            IssueScreen(
                onBack = { navController.popBackStack() },
            )
        }

        composable<Route.Return> {
            ReturnScreen(
                onBack = { navController.popBackStack() },
            )
        }

        composable<Route.Upload> { backStackEntry ->
            val route = backStackEntry.arguments
            UploadScreen(
                deviceId = route?.getString("deviceId")
                    ?: "",
                employeeId = route?.getString(
                    "employeeId",
                ),
                employeeName = route?.getString(
                    "employeeName",
                ),
                bindingId = route?.getLong("bindingId"),
                onBack = { navController.popBackStack() },
            )
        }

        composable<Route.Journal> {
            JournalScreen(
                onBack = { navController.popBackStack() },
            )
        }

        composable<Route.Summary> {
            SummaryScreen(
                onBack = { navController.popBackStack() },
            )
        }

        composable<Route.Settings> {
            SettingsScreen(
                onBack = { navController.popBackStack() },
                onNavigateToLogin = {
                    navController.navigate(Route.Login) {
                        popUpTo(0) { inclusive = true }
                    }
                },
                onNavigateToContextSelection = {
                    navController.navigate(
                        Route.ContextSelection,
                    ) {
                        popUpTo(Route.Home) {
                            inclusive = true
                        }
                    }
                },
            )
        }
    }
}
