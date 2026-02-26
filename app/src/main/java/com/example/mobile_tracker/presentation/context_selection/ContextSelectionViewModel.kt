package com.example.mobile_tracker.presentation.context_selection

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mobile_tracker.data.local.datastore.UserPreferencesManager
import com.example.mobile_tracker.data.local.db.dao.ShiftContextDao
import com.example.mobile_tracker.data.local.db.entity.ShiftContextEntity
import com.example.mobile_tracker.data.repository.ReferenceRepository
import com.example.mobile_tracker.domain.model.Site
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class ContextSelectionViewModel(
    private val shiftContextDao: ShiftContextDao,
    private val preferencesManager: UserPreferencesManager,
    private val referenceRepository: ReferenceRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(ContextSelectionState())
    val state: StateFlow<ContextSelectionState> =
        _state.asStateFlow()

    private val _effect = Channel<ContextSelectionEffect>()
    val effect = _effect.receiveAsFlow()

    init {
        loadInitialData()
    }

    private fun loadInitialData() {
        viewModelScope.launch {
            val today = LocalDate.now().format(
                DateTimeFormatter.ISO_LOCAL_DATE,
            )
            _state.update { it.copy(shiftDate = today) }

            val prefs =
                preferencesManager.userPreferences.first()
            val sites = prefs.scopeIds.mapIndexed { i, id ->
                Site(
                    id = id,
                    name = "Площадка ${i + 1}",
                )
            }

            _state.update {
                it.copy(
                    sites = sites,
                    selectedSite = sites.firstOrNull(),
                )
            }
        }
    }

    fun onIntent(intent: ContextSelectionIntent) {
        when (intent) {
            is ContextSelectionIntent.SiteSelected -> {
                _state.update {
                    it.copy(selectedSite = intent.site)
                }
            }

            is ContextSelectionIntent.DateChanged -> {
                _state.update {
                    it.copy(shiftDate = intent.date)
                }
            }

            is ContextSelectionIntent.ShiftTypeChanged -> {
                _state.update {
                    it.copy(shiftType = intent.type)
                }
            }

            is ContextSelectionIntent.StartWork -> saveContext()
        }
    }

    private fun saveContext() {
        val current = _state.value
        val site = current.selectedSite
        if (site == null) {
            _state.update {
                it.copy(error = "Выберите площадку")
            }
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }

            val prefs =
                preferencesManager.userPreferences.first()

            shiftContextDao.save(
                ShiftContextEntity(
                    siteId = site.id,
                    siteName = site.name,
                    shiftDate = current.shiftDate,
                    shiftType = current.shiftType,
                    operatorId = prefs.userId,
                    operatorName = prefs.userName,
                    updatedAt = System.currentTimeMillis(),
                ),
            )

            _state.update { it.copy(isLoading = false) }

            launch {
                referenceRepository.syncAll(site.id)
            }

            _effect.send(ContextSelectionEffect.NavigateToHome)
        }
    }
}
