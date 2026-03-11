package com.example.mobile_tracker.presentation.workers

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mobile_tracker.data.local.db.dao.BindingDao
import com.example.mobile_tracker.data.local.db.dao.DeviceDao
import com.example.mobile_tracker.data.local.db.dao.EmployeeDao
import com.example.mobile_tracker.data.local.db.dao.OperationLogDao
import com.example.mobile_tracker.data.local.db.dao.PacketQueueDao
import com.example.mobile_tracker.data.local.db.dao.ShiftContextDao
import com.example.mobile_tracker.presentation.monitoring.WorkerMonitoringSnapshot
import com.example.mobile_tracker.presentation.monitoring.WorkerMonitoringStatus
import com.example.mobile_tracker.presentation.monitoring.buildWorkerMonitoringSnapshots
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class WorkersFilter {
    All,
    Active,
    Idle,
    Offline,
}

data class WorkersState(
    val siteName: String = "",
    val shiftDate: String = "",
    val shiftType: String = "day",
    val query: String = "",
    val filter: WorkersFilter = WorkersFilter.All,
    val isLoading: Boolean = true,
    val workers: List<WorkerMonitoringSnapshot> = emptyList(),
    val error: String? = null,
) {
    val filteredWorkers: List<WorkerMonitoringSnapshot>
        get() = workers.filter { worker ->
            val matchesFilter = when (filter) {
                WorkersFilter.All -> true
                WorkersFilter.Active -> worker.status == WorkerMonitoringStatus.Active
                WorkersFilter.Idle -> worker.status == WorkerMonitoringStatus.Idle
                WorkersFilter.Offline -> worker.status == WorkerMonitoringStatus.Offline
            }
            val normalizedQuery = query.trim()
            val matchesQuery = normalizedQuery.isBlank() || buildString {
                append(worker.fullName)
                append(" ")
                append(worker.roleLabel)
                append(" ")
                append(worker.zoneId.orEmpty())
                append(" ")
                append(worker.personnelNumber.orEmpty())
                append(" ")
                append(worker.deviceId.orEmpty())
            }.contains(normalizedQuery, ignoreCase = true)
            matchesFilter && matchesQuery
        }
}

sealed interface WorkersIntent {
    data class UpdateQuery(val query: String) : WorkersIntent
    data class SetFilter(val filter: WorkersFilter) : WorkersIntent
}

class WorkersViewModel(
    private val shiftContextDao: ShiftContextDao,
    private val employeeDao: EmployeeDao,
    private val deviceDao: DeviceDao,
    private val bindingDao: BindingDao,
    private val operationLogDao: OperationLogDao,
    private val packetQueueDao: PacketQueueDao,
) : ViewModel() {

    private val _state = MutableStateFlow(WorkersState())
    val state: StateFlow<WorkersState> = _state.asStateFlow()

    private var observeJob: Job? = null

    init {
        loadContext()
    }

    fun onIntent(intent: WorkersIntent) {
        when (intent) {
            is WorkersIntent.UpdateQuery -> _state.update { it.copy(query = intent.query) }
            is WorkersIntent.SetFilter -> _state.update { it.copy(filter = intent.filter) }
        }
    }

    private fun loadContext() {
        viewModelScope.launch {
            val context = shiftContextDao.get()
            if (context == null) {
                _state.update {
                    it.copy(
                        isLoading = false,
                        error = "Контекст смены не выбран",
                    )
                }
                return@launch
            }

            _state.update {
                it.copy(
                    siteName = context.siteName,
                    shiftDate = context.shiftDate,
                    shiftType = context.shiftType,
                )
            }

            observeWorkers(
                siteId = context.siteId,
                shiftDate = context.shiftDate,
                shiftType = context.shiftType,
            )
        }
    }

    private fun observeWorkers(
        siteId: String,
        shiftDate: String,
        shiftType: String,
    ) {
        observeJob?.cancel()
        observeJob = viewModelScope.launch {
            combine(
                employeeDao.observeBySite(siteId),
                deviceDao.observeBySite(siteId),
                bindingDao.observeByShift(siteId, shiftDate),
                operationLogDao.observeByShift(siteId, shiftDate),
                packetQueueDao.observeUnsent(siteId),
            ) { employees, devices, bindings, logs, packets ->
                buildWorkerMonitoringSnapshots(
                    employees = employees,
                    devices = devices,
                    bindings = bindings,
                    logs = logs,
                    packets = packets,
                    shiftDate = shiftDate,
                    shiftType = shiftType,
                )
            }.collect { workers ->
                _state.update {
                    it.copy(
                        isLoading = false,
                        workers = workers,
                        error = null,
                    )
                }
            }
        }
    }
}
