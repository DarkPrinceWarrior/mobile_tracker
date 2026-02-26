package com.example.mobile_tracker.presentation.employees

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mobile_tracker.data.local.db.dao.EmployeeDao
import com.example.mobile_tracker.data.local.db.dao.ShiftContextDao
import com.example.mobile_tracker.data.remote.dto.toDomain
import com.example.mobile_tracker.data.repository.ReferenceRepository
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber

@OptIn(FlowPreview::class)
class EmployeeSearchViewModel(
    private val employeeDao: EmployeeDao,
    private val shiftContextDao: ShiftContextDao,
    private val repository: ReferenceRepository,
) : ViewModel() {

    private val _state =
        MutableStateFlow(EmployeeSearchState())
    val state: StateFlow<EmployeeSearchState> =
        _state.asStateFlow()

    private val _effect =
        MutableSharedFlow<EmployeeSearchEffect>()
    val effect: SharedFlow<EmployeeSearchEffect> =
        _effect.asSharedFlow()

    private val queryFlow = MutableStateFlow("")
    private var currentSiteId: String? = null

    init {
        loadSiteContext()
        observeQuery()
    }

    fun onIntent(intent: EmployeeSearchIntent) {
        when (intent) {
            is EmployeeSearchIntent.UpdateQuery -> {
                _state.update {
                    it.copy(query = intent.query)
                }
                queryFlow.value = intent.query
            }
            EmployeeSearchIntent.Search ->
                performSearch(_state.value.query)
            EmployeeSearchIntent.Refresh ->
                loadAllEmployees()
            EmployeeSearchIntent.SyncEmployees ->
                syncFromServer()
        }
    }

    private fun loadSiteContext() {
        viewModelScope.launch {
            val ctx = shiftContextDao.get()
            if (ctx != null) {
                currentSiteId = ctx.siteId
                loadAllEmployees()
            } else {
                _state.update {
                    it.copy(
                        error = "Контекст не выбран",
                    )
                }
            }
        }
    }

    private fun observeQuery() {
        queryFlow
            .debounce(300L)
            .distinctUntilChanged()
            .onEach { query ->
                if (query.isBlank()) {
                    loadAllEmployees()
                } else {
                    performSearch(query)
                }
            }
            .launchIn(viewModelScope)
    }

    private fun loadAllEmployees() {
        val siteId = currentSiteId ?: return
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            employeeDao.observeBySite(siteId)
                .collect { entities ->
                    val employees =
                        entities.map { it.toDomain() }
                    _state.update {
                        it.copy(
                            results = employees,
                            isLoading = false,
                            error = null,
                            totalCount =
                                employees.size,
                        )
                    }
                }
        }
    }

    private fun performSearch(query: String) {
        val siteId = currentSiteId ?: return
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }

            val byNumber =
                employeeDao.findByPersonnelNumber(
                    query,
                )
            if (byNumber != null) {
                _state.update {
                    it.copy(
                        results =
                            listOf(byNumber.toDomain()),
                        isLoading = false,
                        error = null,
                        totalCount = 1,
                    )
                }
                return@launch
            }

            val searchResults =
                employeeDao.search(query, siteId)
            _state.update {
                it.copy(
                    results = searchResults.map {
                        e -> e.toDomain()
                    },
                    isLoading = false,
                    error = null,
                    totalCount =
                        searchResults.size,
                )
            }
        }
    }

    private fun syncFromServer() {
        val siteId = currentSiteId ?: return
        viewModelScope.launch {
            _state.update { it.copy(isSyncing = true) }
            repository.syncEmployees(siteId).fold(
                onSuccess = {
                    _state.update {
                        it.copy(isSyncing = false)
                    }
                    _effect.emit(
                        EmployeeSearchEffect
                            .SyncCompleted,
                    )
                },
                onFailure = { e ->
                    Timber.e(
                        e, "Employee sync failed",
                    )
                    _state.update {
                        it.copy(
                            isSyncing = false,
                            error = e.message,
                        )
                    }
                    _effect.emit(
                        EmployeeSearchEffect.ShowError(
                            e.message
                                ?: "Ошибка синхронизации",
                        ),
                    )
                },
            )
        }
    }
}
