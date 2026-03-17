package com.example.mobile_tracker.presentation.navigation

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.mobile_tracker.presentation.alerts.AlertsScreen
import com.example.mobile_tracker.presentation.binding.issue.IssueScreen
import com.example.mobile_tracker.presentation.binding.return_device.ReturnScreen
import com.example.mobile_tracker.presentation.context_selection.ContextSelectionScreen
import com.example.mobile_tracker.presentation.devices.DeviceListScreen
import com.example.mobile_tracker.presentation.employees.EmployeeSearchScreen
import com.example.mobile_tracker.presentation.home.HomeScreen
import com.example.mobile_tracker.presentation.login.LoginScreen
import com.example.mobile_tracker.presentation.journal.JournalScreen
import com.example.mobile_tracker.presentation.maps.MapsScreen
import com.example.mobile_tracker.presentation.monitoring.MonitoringScreen
import com.example.mobile_tracker.presentation.nfc_scan.NfcScanScreen
import com.example.mobile_tracker.presentation.qr_scan.QrScanScreen
import com.example.mobile_tracker.presentation.register_watch.RegisterWatchScreen
import com.example.mobile_tracker.presentation.settings.SettingsScreen
import com.example.mobile_tracker.presentation.summary.SummaryScreen
import com.example.mobile_tracker.presentation.upload.UploadScreen
import com.example.mobile_tracker.presentation.workers.WorkersScreen
import com.example.mobile_tracker.presentation.worker_detail.WorkerDetailScreen

@OptIn(ExperimentalMaterial3Api::class)
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
                onNavigateToMonitoring = {
                    navController.navigate(Route.Monitoring)
                },
                onNavigateToMaps = {
                    navController.navigate(Route.Maps)
                },
                onNavigateToIssue = {
                    navController.navigate(Route.Issue())
                },
                onNavigateToReturn = {
                    navController.navigate(Route.Return())
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
                onNavigateToAlerts = {
                    navController.navigate(Route.Alerts)
                },
                onNavigateToRegisterWatch = {
                    navController.navigate(
                        Route.QrScan(mode = QrScanMode.RegisterWatch),
                    )
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
                onOpenWorkerDetail = { employeeId ->
                    navController.navigate(Route.WorkerDetail(employeeId))
                },
            )
        }

        composable<Route.Monitoring> {
            MonitoringScreen(
                onBack = { navController.popBackStack() },
                onNavigateToWorkers = {
                    navController.navigate(Route.Workers)
                },
                onNavigateToMaps = {
                    navController.navigate(Route.Maps)
                },
                onNavigateToAlerts = {
                    navController.navigate(Route.Alerts)
                },
                onOpenWorkerDetail = { employeeId ->
                    navController.navigate(Route.WorkerDetail(employeeId))
                },
            )
        }

        composable<Route.Workers> {
            WorkersScreen(
                onBack = { navController.popBackStack() },
                onNavigateToMonitoring = {
                    navController.navigate(Route.Monitoring)
                },
                onNavigateToMaps = {
                    navController.navigate(Route.Maps)
                },
                onNavigateToAlerts = {
                    navController.navigate(Route.Alerts)
                },
                onOpenWorkerDetail = { employeeId ->
                    navController.navigate(Route.WorkerDetail(employeeId))
                },
            )
        }

        composable<Route.Maps> {
            MapsScreen(
                onBack = { navController.popBackStack() },
                onOpenWorkerDetail = { employeeId ->
                    navController.navigate(Route.WorkerDetail(employeeId))
                },
            )
        }

        composable<Route.WorkerDetail> { backStackEntry ->
            WorkerDetailScreen(
                employeeId = backStackEntry.arguments?.getString("employeeId").orEmpty(),
                onBack = { navController.popBackStack() },
            )
        }

        composable<Route.Issue> { backStackEntry ->
            IssueScreen(
                scannedDeviceId = backStackEntry.arguments?.getString("scannedDeviceId"),
                scannedPassNumber = backStackEntry.arguments?.getString("scannedPassNumber"),
                onBack = { navController.popBackStack() },
                onCompleted = { navController.popBackStack() },
                onOpenQrScan = {
                    navController.navigate(
                        Route.QrScan(mode = QrScanMode.IssueDevice),
                    )
                },
                onOpenNfcScan = {
                    navController.navigate(
                        Route.NfcScan(mode = NfcScanMode.IdentifyEmployee),
                    )
                },
            )
        }

        composable<Route.Return> { backStackEntry ->
            ReturnScreen(
                scannedDeviceId = backStackEntry.arguments?.getString("scannedDeviceId"),
                onBack = { navController.popBackStack() },
                onCompleted = { navController.popBackStack() },
                onOpenQrScan = {
                    navController.navigate(
                        Route.QrScan(mode = QrScanMode.ReturnDevice),
                    )
                },
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
                onCompleted = { navController.popBackStack() },
                onOpenQrScan = {
                    navController.navigate(
                        Route.QrScan(
                            mode = QrScanMode.UploadDevice,
                            currentDeviceId = route?.getString("deviceId").orEmpty(),
                            employeeId = route?.getString("employeeId"),
                            employeeName = route?.getString("employeeName"),
                            bindingId = route?.getLong("bindingId"),
                        ),
                    )
                },
            )
        }

        composable<Route.Journal> {
            JournalScreen(
                onBack = { navController.popBackStack() },
            )
        }

        composable<Route.QrScan> { backStackEntry ->
            val route = backStackEntry.arguments
            val mode = route?.getString("mode")
                ?.let(QrScanMode::valueOf)
                ?: QrScanMode.IssueDevice
            val currentDeviceId = route?.getString("currentDeviceId").orEmpty()
            val employeeId = route?.getString("employeeId")
            val employeeName = route?.getString("employeeName")
            val bindingId = route?.getLong("bindingId")

            QrScanScreen(
                mode = mode,
                onBack = { navController.popBackStack() },
                onConfirmResult = { scannedValue ->
                    when (mode) {
                        QrScanMode.IssueDevice -> {
                            navController.navigate(
                                Route.Issue(scannedDeviceId = scannedValue),
                            ) {
                                popUpTo(Route.Issue()) { inclusive = true }
                            }
                        }

                        QrScanMode.ReturnDevice -> {
                            navController.navigate(
                                Route.Return(scannedDeviceId = scannedValue),
                            ) {
                                popUpTo(Route.Return()) { inclusive = true }
                            }
                        }

                        QrScanMode.UploadDevice -> {
                            navController.navigate(
                                Route.Upload(
                                    deviceId = scannedValue,
                                    employeeId = employeeId,
                                    employeeName = employeeName,
                                    bindingId = bindingId,
                                ),
                            ) {
                                popUpTo(
                                    Route.Upload(
                                        deviceId = currentDeviceId,
                                        employeeId = employeeId,
                                        employeeName = employeeName,
                                        bindingId = bindingId,
                                    ),
                                ) {
                                    inclusive = true
                                }
                            }
                        }

                        QrScanMode.RegisterWatch -> {
                            navController.navigate(
                                Route.RegisterWatch(scannedValue = scannedValue),
                            ) {
                                popUpTo(Route.Home) { inclusive = false }
                            }
                        }
                    }
                },
            )
        }

        composable<Route.NfcScan> { backStackEntry ->
            val route = backStackEntry.arguments
            val mode = route?.getString("mode")
                ?.let(NfcScanMode::valueOf)
                ?: NfcScanMode.IdentifyEmployee

            NfcScanScreen(
                mode = mode,
                onBack = { navController.popBackStack() },
                onConfirmResult = { scannedValue ->
                    when (mode) {
                        NfcScanMode.IdentifyEmployee -> {
                            navController.navigate(
                                Route.Issue(scannedPassNumber = scannedValue),
                            ) {
                                popUpTo(Route.Issue()) { inclusive = true }
                            }
                        }
                    }
                },
            )
        }

        composable<Route.Alerts> {
            AlertsScreen(
                onBack = { navController.popBackStack() },
                onNavigateToDevices = {
                    navController.navigate(Route.DeviceList)
                },
                onNavigateToReturn = {
                    navController.navigate(Route.Return())
                },
                onNavigateToJournal = {
                    navController.navigate(Route.Journal)
                },
                onNavigateToWorkerDetail = { employeeId ->
                    navController.navigate(Route.WorkerDetail(employeeId))
                },
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

        composable<Route.RegisterWatch> { backStackEntry ->
            val scannedValue = backStackEntry.arguments?.getString("scannedValue").orEmpty()
            RegisterWatchScreen(
                scannedValue = scannedValue,
                onBack = { navController.popBackStack() },
                onCompleted = {
                    navController.navigate(Route.Home) {
                        popUpTo(Route.Home) { inclusive = true }
                    }
                },
            )
        }
    }
}
