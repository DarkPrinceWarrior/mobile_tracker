package com.example.mobile_tracker.presentation.workers

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mobile_tracker.data.local.db.dao.BindingDao
import com.example.mobile_tracker.data.local.db.dao.DeviceDao
import com.example.mobile_tracker.data.local.db.dao.EmployeeDao
import com.example.mobile_tracker.data.local.db.dao.OperationLogDao
import com.example.mobile_tracker.data.local.db.dao.PacketQueueDao
import com.example.mobile_tracker.data.local.db.dao.ShiftContextDao
import com.example.mobile_tracker.data.remote.api.ShiftsApi
import com.example.mobile_tracker.data.remote.dto.ShiftMetricsResponse
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
import timber.log.Timber

data class WorkersState(
    val siteName: String = "",
    val shiftDate: String = "",
    val shiftType: String = "day",
    val isLoading: Boolean = true,
    val workers: List<WorkerMonitoringSnapshot> = emptyList(),
    val query: String = "",
    val filter: WorkersFilter = WorkersFilter.All,
    val zoneFilter: String? = null,
    val error: String? = null,
) {
    val filteredWorkers: List<WorkerMonitoringSnapshot>
        get() {
            var list = workers
            if (query.isNotBlank()) {
                val q = query.lowercase()
                list = list.filter {
                    it.fullName.lowercase().contains(q) ||
                        it.roleLabel.lowercase().contains(q) ||
                        it.personnelNumber?.lowercase()?.contains(q) == true
                }
            }
            list = when (filter) {
                WorkersFilter.All -> list
                WorkersFilter.Active -> list.filter { it.status == WorkerMonitoringStatus.Active }
                WorkersFilter.Idle -> list.filter { it.status == WorkerMonitoringStatus.Idle }
                WorkersFilter.Offline -> list.filter { it.status == WorkerMonitoringStatus.Offline }
            }
            if (zoneFilter != null) {
                list = list.filter { it.zoneId == zoneFilter }
            }
            return list
        }

    val activeCount: Int get() = workers.count { it.status == WorkerMonitoringStatus.Active }
    val idleCount: Int get() = workers.count { it.status == WorkerMonitoringStatus.Idle }
    val offlineCount: Int get() = workers.count { it.status == WorkerMonitoringStatus.Offline }
}

enum class WorkersFilter { All, Active, Idle, Offline }

sealed interface WorkersIntent {
    data class UpdateQuery(val query: String) : WorkersIntent
    data class SetFilter(val filter: WorkersFilter) : WorkersIntent
    data class SetZoneFilter(val zoneId: String?) : WorkersIntent
}

class WorkersViewModel(
    private val shiftContextDao: ShiftContextDao,
    private val employeeDao: EmployeeDao,
    private val deviceDao: DeviceDao,
    private val bindingDao: BindingDao,
    private val operationLogDao: OperationLogDao,
    private val packetQueueDao: PacketQueueDao,
    private val shiftsApi: ShiftsApi,
) : ViewModel() {

    private val _state = MutableStateFlow(WorkersState())
    val state: StateFlow<WorkersState> = _state.asStateFlow()

    /** Реальные метрики с бэкенда, ключ = employeeId */
    private val realMetrics = MutableStateFlow<Map<String, ShiftMetricsResponse>>(emptyMap())

    private var observeJob: Job? = null

    init {
        loadContext()
    }

    fun onIntent(intent: WorkersIntent) {
        when (intent) {
            is WorkersIntent.UpdateQuery -> _state.update { it.copy(query = intent.query) }
            is WorkersIntent.SetFilter -> _state.update { it.copy(filter = intent.filter) }
            is WorkersIntent.SetZoneFilter -> _state.update { it.copy(zoneFilter = intent.zoneId) }
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

            // Загружаем реальные метрики смен с бэкенда
            fetchRealShiftMetrics(shiftDate = context.shiftDate)

            observeWorkers(
                siteId = context.siteId,
                shiftDate = context.shiftDate,
                shiftType = context.shiftType,
            )
        }
    }

    private fun fetchRealShiftMetrics(shiftDate: String) {
        viewModelScope.launch {
            try {
                val shiftsResponse = shiftsApi.getShifts(
                    dateFrom = shiftDate,
                    dateTo = shiftDate,
                    pageSize = 100,
                )
                val metricsMap = mutableMapOf<String, ShiftMetricsResponse>()
                for (shift in shiftsResponse.items) {
                    if (shift.employeeId == null) continue
                    try {
                        val metrics = shiftsApi.getShiftMetrics(shift.id)
                        metricsMap[shift.employeeId] = metrics
                    } catch (e: Exception) {
                        Timber.w(e, "Failed to load metrics for shift ${shift.id}")
                    }
                }
                realMetrics.value = metricsMap
                Timber.d("Workers: Loaded real metrics for ${metricsMap.size} shifts")
            } catch (e: Exception) {
                Timber.w(e, "Workers: Failed to load shifts from backend")
            }
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
                realMetrics,
            ) { values ->
                @Suppress("UNCHECKED_CAST")
                val employees = values[0] as List<com.example.mobile_tracker.data.local.db.entity.EmployeeEntity>
                @Suppress("UNCHECKED_CAST")
                val devices = values[1] as List<com.example.mobile_tracker.data.local.db.entity.DeviceEntity>
                @Suppress("UNCHECKED_CAST")
                val bindings = values[2] as List<com.example.mobile_tracker.data.local.db.entity.BindingEntity>
                @Suppress("UNCHECKED_CAST")
                val logs = values[3] as List<com.example.mobile_tracker.data.local.db.entity.OperationLogEntity>
                @Suppress("UNCHECKED_CAST")
                val packets = values[4] as List<com.example.mobile_tracker.data.local.db.entity.PacketQueueEntity>
                @Suppress("UNCHECKED_CAST")
                val metrics = values[5] as Map<String, ShiftMetricsResponse>

                val workers = buildWorkerMonitoringSnapshots(
                    employees = employees,
                    devices = devices,
                    bindings = bindings,
                    logs = logs,
                    packets = packets,
                    shiftDate = shiftDate,
                    shiftType = shiftType,
                )

                // Обогащаем реальными метриками с бэкенда
                if (metrics.isNotEmpty()) {
                    workers.map { worker ->
                        val m = metrics[worker.employeeId] ?: return@map worker
                        worker.copy(
                            heartRate = if (m.avgHrBpm > 0) m.avgHrBpm else worker.heartRate,
                            smrPercent = m.productivityPercent.toInt().takeIf { it > 0 } ?: worker.smrPercent,
                            efficiencyPercent = m.productivityPercent.toInt().takeIf { it > 0 } ?: worker.efficiencyPercent,
                            activeDurationMinutes = (m.onSiteDurationSec / 60).toLong().takeIf { it > 0 } ?: worker.activeDurationMinutes,
                        )
                    }
                } else {
                    workers
                }
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
