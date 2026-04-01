package com.example.mobile_tracker.presentation.login

data class LoginState(
    val email: String = "operator.kazan@company.com",
    val password: String = "operator123",
    val isLoading: Boolean = false,
    val isPasswordVisible: Boolean = false,
    val error: String? = null,
)

sealed interface LoginIntent {
    data class EmailChanged(val value: String) : LoginIntent
    data class PasswordChanged(val value: String) : LoginIntent
    data object TogglePasswordVisibility : LoginIntent
    data object LoginClicked : LoginIntent
}

sealed interface LoginEffect {
    data object NavigateToContextSelection : LoginEffect
}
