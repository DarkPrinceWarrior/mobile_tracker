package com.example.mobile_tracker.presentation.navigation

import kotlinx.serialization.Serializable

@Serializable
sealed interface Route {
    @Serializable
    data object Login : Route

    @Serializable
    data object ContextSelection : Route

    @Serializable
    data object Home : Route
}
