package com.example.mobile_tracker.presentation.journal

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mobile_tracker.data.local.db.dao.OperationLogDao
import com.example.mobile_tracker.data.local.db.dao.ShiftContextDao
import com.example.mobile_tracker.data.local.db.entity.OperationLogEntity
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber

class JournalViewModel(
    private val operationLogDao: OperationLogDao,
    private val shiftContextDao: ShiftContextDao,
) : ViewModel() {

    private val _state = MutableStateFlow(JournalState())
    val state: StateFlow<JournalState> = _state.asStateFlow()

    private val _effect = MutableSharedFlow<JournalEffect>()
    val effect: SharedFlow<JournalEffect> =
        _effect.asSharedFlow()

    private var siteId: String = ""
    private var shiftDate: String = ""

    init {
        loadContext()
    }

    fun onIntent(intent: JournalIntent) {
        when (intent) {
            is JournalIntent.SetTypeFilter ->
                setTypeFilter(intent.type)
            is JournalIntent.SetStatusFilter ->
                setStatusFilter(intent.status)
            is JournalIntent.SetSearchQuery ->
                setSearchQuery(intent.query)
            JournalIntent.Refresh -> refresh()
            JournalIntent.DismissError ->
                _state.update { it.copy(error = null) }
        }
    }

    private fun loadContext() {
        viewModelScope.launch {
            val ctx = shiftContextDao.get()
            if (ctx != null) {
                siteId = ctx.siteId
                shiftDate = ctx.shiftDate
                observeLogs()
            } else {
                _state.update {
                    it.copy(
                        isLoading = false,
                        error = "Контекст смены не выбран",
                    )
                }
            }
        }
    }

    private fun observeLogs() {
        viewModelScope.launch {
            operationLogDao.observeByShift(siteId, shiftDate)
                .collect { logs ->
                    _state.update {
                        it.copy(
                            logs = logs,
                            isLoading = false,
                            availableTypes = logs
                                .map { l -> l.type }
                                .distinct()
                                .sorted(),
                        )
                    }
                    applyFilters()
                }
        }
    }

    private fun setTypeFilter(type: String?) {
        _state.update { it.copy(typeFilter = type) }
        applyFilters()
    }

    private fun setStatusFilter(status: String?) {
        _state.update { it.copy(statusFilter = status) }
        applyFilters()
    }

    private fun setSearchQuery(query: String) {
        _state.update { it.copy(searchQuery = query) }
        applyFilters()
    }

    private fun applyFilters() {
        val current = _state.value
        val filtered = current.logs.filter { log ->
            matchesType(log, current.typeFilter) &&
                matchesStatus(log, current.statusFilter) &&
                matchesSearch(log, current.searchQuery)
        }
        _state.update { it.copy(filteredLogs = filtered) }
    }

    private fun matchesType(
        log: OperationLogEntity,
        type: String?,
    ): Boolean = type == null || log.type == type

    private fun matchesStatus(
        log: OperationLogEntity,
        status: String?,
    ): Boolean = status == null || log.status == status

    private fun matchesSearch(
        log: OperationLogEntity,
        query: String,
    ): Boolean {
        if (query.isBlank()) return true
        val q = query.lowercase()
        return log.employeeName
            ?.lowercase()?.contains(q) == true ||
            log.deviceId
                ?.lowercase()?.contains(q) == true ||
            log.details
                ?.lowercase()?.contains(q) == true
    }

    private fun refresh() {
        _state.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            try {
                val logs = operationLogDao.observeByShift(
                    siteId,
                    shiftDate,
                )
                logs.collect { list ->
                    _state.update {
                        it.copy(
                            logs = list,
                            isLoading = false,
                            availableTypes = list
                                .map { l -> l.type }
                                .distinct()
                                .sorted(),
                        )
                    }
                    applyFilters()
                }
            } catch (e: Exception) {
                Timber.e(e, "Journal refresh failed")
                _state.update {
                    it.copy(
                        isLoading = false,
                        error = e.message
                            ?: "Ошибка загрузки журнала",
                    )
                }
            }
        }
    }
}
