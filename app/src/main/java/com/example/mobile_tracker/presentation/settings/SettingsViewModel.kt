package com.example.mobile_tracker.presentation.settings

import android.content.Context
import android.content.pm.PackageManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mobile_tracker.data.local.datastore.UserPreferencesManager
import com.example.mobile_tracker.data.local.db.dao.ShiftContextDao
import com.example.mobile_tracker.data.local.secure.SecureStorage
import com.example.mobile_tracker.data.repository.ReferenceRepository
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber

class SettingsViewModel(
    private val secureStorage: SecureStorage,
    private val preferencesManager: UserPreferencesManager,
    private val shiftContextDao: ShiftContextDao,
    private val referenceRepository: ReferenceRepository,
    private val appContext: Context,
) : ViewModel() {

    private val _state = MutableStateFlow(SettingsState())
    val state: StateFlow<SettingsState> = _state.asStateFlow()

    private val _effect = Channel<SettingsEffect>()
    val effect = _effect.receiveAsFlow()

    init {
        loadSettings()
    }

    fun onIntent(intent: SettingsIntent) {
        when (intent) {
            SettingsIntent.LogoutClicked ->
                _state.update {
                    it.copy(showLogoutDialog = true)
                }
            SettingsIntent.LogoutConfirmed -> logout()
            SettingsIntent.LogoutDismissed ->
                _state.update {
                    it.copy(showLogoutDialog = false)
                }
            SettingsIntent.ChangeContextClicked ->
                changeContext()
            SettingsIntent.ClearCacheClicked ->
                _state.update {
                    it.copy(showClearCacheDialog = true)
                }
            SettingsIntent.ClearCacheConfirmed ->
                clearCache()
            SettingsIntent.ClearCacheDismissed ->
                _state.update {
                    it.copy(showClearCacheDialog = false)
                }
            SettingsIntent.DismissError ->
                _state.update { it.copy(error = null) }
        }
    }

    private fun loadSettings() {
        viewModelScope.launch {
            try {
                val context = shiftContextDao.get()
                val version = try {
                    val info = appContext.packageManager
                        .getPackageInfo(
                            appContext.packageName,
                            0,
                        )
                    info.versionName ?: "1.0.0"
                } catch (_: PackageManager.NameNotFoundException) {
                    "1.0.0"
                }

                preferencesManager.userPreferences
                    .collect { prefs ->
                        _state.update {
                            it.copy(
                                operatorName =
                                    prefs.userName,
                                operatorEmail =
                                    prefs.userEmail,
                                siteName =
                                    context?.siteName ?: "",
                                shiftDate =
                                    context?.shiftDate ?: "",
                                shiftType =
                                    context?.shiftType
                                        ?: "day",
                                appVersion = version,
                            )
                        }
                    }
            } catch (e: Exception) {
                Timber.e(e, "Failed to load settings")
                _state.update {
                    it.copy(
                        error = e.message
                            ?: "Ошибка загрузки настроек",
                    )
                }
            }
        }
    }

    private fun logout() {
        viewModelScope.launch {
            _state.update {
                it.copy(
                    isLoading = true,
                    showLogoutDialog = false,
                )
            }
            try {
                secureStorage.clearTokens()
                preferencesManager.clearAll()
                shiftContextDao.clear()
                _effect.send(SettingsEffect.NavigateToLogin)
            } catch (e: Exception) {
                Timber.e(e, "Logout failed")
                _state.update {
                    it.copy(
                        isLoading = false,
                        error = "Ошибка выхода: ${e.message}",
                    )
                }
            }
        }
    }

    private fun changeContext() {
        viewModelScope.launch {
            shiftContextDao.clear()
            _effect.send(
                SettingsEffect.NavigateToContextSelection,
            )
        }
    }

    private fun clearCache() {
        viewModelScope.launch {
            _state.update {
                it.copy(
                    isLoading = true,
                    showClearCacheDialog = false,
                )
            }
            try {
                referenceRepository.clearAll()
                _state.update { it.copy(isLoading = false) }
                _effect.send(
                    SettingsEffect.ShowMessage(
                        "Кэш очищен",
                    ),
                )
            } catch (e: Exception) {
                Timber.e(e, "Clear cache failed")
                _state.update {
                    it.copy(
                        isLoading = false,
                        error = "Ошибка: ${e.message}",
                    )
                }
            }
        }
    }
}
