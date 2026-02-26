package com.example.mobile_tracker.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.mobile_tracker.presentation.context_selection.ContextSelectionScreen
import com.example.mobile_tracker.presentation.home.HomeScreen
import com.example.mobile_tracker.presentation.login.LoginScreen

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
            )
        }
    }
}
