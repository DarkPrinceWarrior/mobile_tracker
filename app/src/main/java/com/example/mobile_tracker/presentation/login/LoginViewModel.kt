package com.example.mobile_tracker.presentation.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mobile_tracker.data.local.datastore.UserPreferencesManager
import com.example.mobile_tracker.data.local.secure.SecureStorage
import com.example.mobile_tracker.data.remote.api.AuthApi
import io.ktor.client.call.body
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.ServerResponseException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber

class LoginViewModel(
    private val authApi: AuthApi,
    private val secureStorage: SecureStorage,
    private val preferencesManager: UserPreferencesManager,
) : ViewModel() {

    private val _state = MutableStateFlow(LoginState())
    val state: StateFlow<LoginState> = _state.asStateFlow()

    private val _effect = Channel<LoginEffect>()
    val effect = _effect.receiveAsFlow()

    fun onIntent(intent: LoginIntent) {
        when (intent) {
            is LoginIntent.EmailChanged -> {
                _state.update {
                    it.copy(email = intent.value, error = null)
                }
            }

            is LoginIntent.PasswordChanged -> {
                _state.update {
                    it.copy(
                        password = intent.value,
                        error = null,
                    )
                }
            }

            is LoginIntent.TogglePasswordVisibility -> {
                _state.update {
                    it.copy(
                        isPasswordVisible = !it.isPasswordVisible,
                    )
                }
            }

            is LoginIntent.LoginClicked -> login()
        }
    }

    private fun login() {
        val current = _state.value
        if (current.email.isBlank() || current.password.isBlank()) {
            _state.update {
                it.copy(error = "Введите email и пароль")
            }
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }

            try {
                val response = authApi.loginParsed(
                    email = current.email.trim(),
                    password = current.password,
                )

                secureStorage.accessToken = response.accessToken
                secureStorage.refreshToken = response.refreshToken

                preferencesManager.saveUserData(
                    userId = response.user.id,
                    email = response.user.email,
                    name = response.user.fullName,
                    role = response.user.role,
                    scopeType = response.user.scopeType,
                    scopeIds = response.user.scopeIds,
                )

                _state.update { it.copy(isLoading = false) }
                _effect.send(
                    LoginEffect.NavigateToContextSelection,
                )
            } catch (e: ClientRequestException) {
                val code = e.response.status.value
                val msg = when (code) {
                    401 -> "Неверный email или пароль"
                    423 -> "Аккаунт заблокирован. " +
                        "Обратитесь к администратору."
                    else -> "Ошибка авторизации (код $code)"
                }
                _state.update {
                    it.copy(isLoading = false, error = msg)
                }
                Timber.w(e, "Login failed: $code")
            } catch (e: ServerResponseException) {
                _state.update {
                    it.copy(
                        isLoading = false,
                        error = "Сервер недоступен. " +
                            "Попробуйте позже.",
                    )
                }
                Timber.e(e, "Server error during login")
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        isLoading = false,
                        error = "Нет подключения к серверу. " +
                            "Проверьте интернет.",
                    )
                }
                Timber.e(e, "Login error")
            }
        }
    }
}
