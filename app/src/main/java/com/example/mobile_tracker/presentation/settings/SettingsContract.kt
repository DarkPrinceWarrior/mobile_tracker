package com.example.mobile_tracker.presentation.settings

data class SettingsState(
    val operatorName: String = "",
    val operatorEmail: String = "",
    val siteName: String = "",
    val shiftDate: String = "",
    val shiftType: String = "day",
    val appVersion: String = "",
    val isLoading: Boolean = false,
    val showLogoutDialog: Boolean = false,
    val showClearCacheDialog: Boolean = false,
    val error: String? = null,
)

sealed interface SettingsIntent {
    data object LogoutClicked : SettingsIntent
    data object LogoutConfirmed : SettingsIntent
    data object LogoutDismissed : SettingsIntent
    data object ChangeContextClicked : SettingsIntent
    data object ClearCacheClicked : SettingsIntent
    data object ClearCacheConfirmed : SettingsIntent
    data object ClearCacheDismissed : SettingsIntent
    data object DismissError : SettingsIntent
}

sealed interface SettingsEffect {
    data object NavigateToLogin : SettingsEffect
    data object NavigateToContextSelection : SettingsEffect
    data class ShowMessage(val message: String) : SettingsEffect
}
