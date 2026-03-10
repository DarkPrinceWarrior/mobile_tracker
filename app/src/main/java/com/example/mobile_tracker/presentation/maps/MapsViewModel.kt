package com.example.mobile_tracker.presentation.maps

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mobile_tracker.data.local.db.dao.BindingDao
import com.example.mobile_tracker.data.local.db.dao.DeviceDao
import com.example.mobile_tracker.data.local.db.dao.EmployeeDao
import com.example.mobile_tracker.data.local.db.dao.OperationLogDao
import com.example.mobile_tracker.data.local.db.dao.PacketQueueDao
import com.example.mobile_tracker.data.local.db.dao.ShiftContextDao
import com.example.mobile_tracker.presentation.monitoring.MonitoringMapMode
import com.example.mobile_tracker.presentation.monitoring.MonitoringZoneSummary
import com.example.mobile_tracker.presentation.monitoring.WorkerMonitoringSnapshot
import com.example.mobile_tracker.presentation.monitoring.WorkerMonitoringStatus
import com.example.mobile_tracker.presentation.monitoring.buildWorkerMonitoringSnapshots
import com.example.mobile_tracker.presentation.monitoring.buildZoneSummaries
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class MapsState(
    val siteName: String = "",
    val shiftDate: String = "",
    val shiftType: String = "day",
    val mode: MonitoringMapMode = MonitoringMapMode.Heatmap,
    val isLoading: Boolean = true,
    val workers: List<WorkerMonitoringSnapshot> = emptyList(),
    val zoneSummaries: List<MonitoringZoneSummary> = emptyList(),
    val error: String? = null,
) {
    val activeCount: Int
        get() = workers.count { it.status == WorkerMonitoringStatus.Active }

    val idleCount: Int
        get() = workers.count { it.status == WorkerMonitoringStatus.Idle }

    val offlineCount: Int
        get() = workers.count { it.status == WorkerMonitoringStatus.Offline }

    val workersOnMap: List<WorkerMonitoringSnapshot>
        get() = workers.filter {
            it.status != WorkerMonitoringStatus.Offline &&
                it.mapXRatio != null &&
                it.mapYRatio != null
        }
}

sealed interface MapsIntent {
    data class SetMode(val mode: MonitoringMapMode) : MapsIntent
}

class MapsViewModel(
    private val shiftContextDao: ShiftContextDao,
    private val employeeDao: EmployeeDao,
    private val deviceDao: DeviceDao,
    private val bindingDao: BindingDao,
    private val operationLogDao: OperationLogDao,
    private val packetQueueDao: PacketQueueDao,
) : ViewModel() {

    private val _state = MutableStateFlow(MapsState())
    val state: StateFlow<MapsState> = _state.asStateFlow()

    private var observeJob: Job? = null

    init {
        loadContext()
    }

    fun onIntent(intent: MapsIntent) {
        when (intent) {
            is MapsIntent.SetMode -> _state.update { it.copy(mode = intent.mode) }
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

            observeMonitoringData(
                siteId = context.siteId,
                shiftDate = context.shiftDate,
                shiftType = context.shiftType,
            )
        }
    }

    private fun observeMonitoringData(
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
                val workers = buildWorkerMonitoringSnapshots(
                    employees = employees,
                    devices = devices,
                    bindings = bindings,
                    logs = logs,
                    packets = packets,
                    shiftDate = shiftDate,
                    shiftType = shiftType,
                )
                workers to buildZoneSummaries(workers)
            }.collect { (workers, zoneSummaries) ->
                _state.update {
                    it.copy(
                        isLoading = false,
                        workers = workers,
                        zoneSummaries = zoneSummaries,
                        error = null,
                    )
                }
            }
        }
    }
}

