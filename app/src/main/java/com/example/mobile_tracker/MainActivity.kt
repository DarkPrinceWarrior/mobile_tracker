package com.example.mobile_tracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.navigation.compose.rememberNavController
import com.example.mobile_tracker.presentation.navigation.AppNavGraph
import com.example.mobile_tracker.presentation.navigation.Route
import com.example.mobile_tracker.ui.theme.Mobile_trackerTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            Mobile_trackerTheme {
                val navController = rememberNavController()
                AppNavGraph(
                    navController = navController,
                    startDestination = Route.Login,
                )
            }
        }
    }
}