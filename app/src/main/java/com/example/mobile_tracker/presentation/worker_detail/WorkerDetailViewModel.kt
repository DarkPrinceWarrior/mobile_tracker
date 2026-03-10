package com.example.mobile_tracker.presentation.worker_detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mobile_tracker.data.local.db.dao.BindingDao
import com.example.mobile_tracker.data.local.db.dao.DeviceDao
import com.example.mobile_tracker.data.local.db.dao.EmployeeDao
import com.example.mobile_tracker.data.local.db.dao.OperationLogDao
import com.example.mobile_tracker.data.local.db.dao.PacketQueueDao
import com.example.mobile_tracker.data.local.db.dao.ShiftContextDao
import com.example.mobile_tracker.presentation.monitoring.WorkerIncident
import com.example.mobile_tracker.presentation.monitoring.WorkerMonitoringSnapshot
import com.example.mobile_tracker.presentation.monitoring.buildWorkerMonitoringSnapshots
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class WorkerDetailState(
    val siteName: String = "",
    val shiftDate: String = "",
    val shiftType: String = "day",
    val isLoading: Boolean = true,
    val worker: WorkerMonitoringSnapshot? = null,
    val acknowledgedIncidentIds: Set<String> = emptySet(),
    val error: String? = null,
) {
    fun isAcknowledged(incident: WorkerIncident): Boolean = incident.id in acknowledgedIncidentIds
}

sealed interface WorkerDetailIntent {
    data class AcknowledgeIncident(val id: String) : WorkerDetailIntent
    data object DismissError : WorkerDetailIntent
}

class WorkerDetailViewModel(
    private val shiftContextDao: ShiftContextDao,
    private val employeeDao: EmployeeDao,
    private val deviceDao: DeviceDao,
    private val bindingDao: BindingDao,
    private val operationLogDao: OperationLogDao,
    private val packetQueueDao: PacketQueueDao,
) : ViewModel() {

    private val _state = MutableStateFlow(WorkerDetailState())
    val state: StateFlow<WorkerDetailState> = _state.asStateFlow()

    private var siteId: String? = null
    private var shiftDate: String? = null
    private var shiftType: String = "day"
    private var currentEmployeeId: String? = null
    private var observeJob: Job? = null

    init {
        loadContext()
    }

    fun bind(employeeId: String) {
        if (currentEmployeeId == employeeId && observeJob != null) {
            return
        }
        currentEmployeeId = employeeId
        _state.update {
            it.copy(
                isLoading = true,
                error = null,
                acknowledgedIncidentIds = emptySet(),
            )
        }
        observeWorker()
    }

    fun onIntent(intent: WorkerDetailIntent) {
        when (intent) {
            is WorkerDetailIntent.AcknowledgeIncident -> _state.update {
                it.copy(
                    acknowledgedIncidentIds = it.acknowledgedIncidentIds + intent.id,
                )
            }
            WorkerDetailIntent.DismissError -> _state.update { it.copy(error = null) }
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

            siteId = context.siteId
            shiftDate = context.shiftDate
            shiftType = context.shiftType

            _state.update {
                it.copy(
                    siteName = context.siteName,
                    shiftDate = context.shiftDate,
                    shiftType = context.shiftType,
                )
            }

            observeWorker()
        }
    }

    private fun observeWorker() {
        val currentSiteId = siteId ?: return
        val currentShiftDate = shiftDate ?: return
        val targetEmployeeId = currentEmployeeId ?: return

        observeJob?.cancel()
        observeJob = viewModelScope.launch {
            combine(
                employeeDao.observeBySite(currentSiteId),
                deviceDao.observeBySite(currentSiteId),
                bindingDao.observeByShift(currentSiteId, currentShiftDate),
                operationLogDao.observeByShift(currentSiteId, currentShiftDate),
                packetQueueDao.observeUnsent(currentSiteId),
            ) { employees, devices, bindings, logs, packets ->
                buildWorkerMonitoringSnapshots(
                    employees = employees,
                    devices = devices,
                    bindings = bindings,
                    logs = logs,
                    packets = packets,
                    shiftDate = currentShiftDate,
                    shiftType = shiftType,
                )
            }.collect { workers ->
                val worker = workers.firstOrNull { it.employeeId == targetEmployeeId }
                _state.update { current ->
                    current.copy(
                        isLoading = false,
                        worker = worker,
                        acknowledgedIncidentIds = current.acknowledgedIncidentIds.intersect(
                            worker?.incidents?.map { it.id }?.toSet() ?: emptySet(),
                        ),
                        error = if (worker == null) {
                            "Сотрудник не найден"
                        } else {
                            null
                        },
                    )
                }
            }
        }
    }
}

