package com.example.mobile_tracker.presentation.worker_detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mobile_tracker.data.local.db.dao.BindingDao
import com.example.mobile_tracker.data.local.db.dao.DeviceDao
import com.example.mobile_tracker.data.local.db.dao.EmployeeDao
import com.example.mobile_tracker.data.local.db.dao.OperationLogDao
import com.example.mobile_tracker.data.local.db.dao.PacketQueueDao
import com.example.mobile_tracker.data.local.db.dao.ShiftContextDao
import com.example.mobile_tracker.data.remote.api.ShiftsApi
import com.example.mobile_tracker.data.remote.api.ZonesApi
import com.example.mobile_tracker.data.remote.dto.ShiftActivityResponse
import com.example.mobile_tracker.data.remote.dto.ShiftMetricsResponse
import com.example.mobile_tracker.presentation.monitoring.WorkerIncident
import com.example.mobile_tracker.presentation.monitoring.WorkerMonitoringSnapshot
import com.example.mobile_tracker.presentation.monitoring.WorkerRouteVisit
import com.example.mobile_tracker.presentation.monitoring.buildWorkerMonitoringSnapshots
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber

data class WorkerDetailState(
    val siteName: String = "",
    val shiftDate: String = "",
    val shiftType: String = "day",
    val isLoading: Boolean = true,
    val worker: WorkerMonitoringSnapshot? = null,
    val shiftMetrics: ShiftMetricsResponse? = null,
    val shiftActivity: ShiftActivityResponse? = null,
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
    private val shiftsApi: ShiftsApi,
    private val zonesApi: ZonesApi,
) : ViewModel() {

    private val _state = MutableStateFlow(WorkerDetailState())
    val state: StateFlow<WorkerDetailState> = _state.asStateFlow()

    private var siteId: String? = null
    private var shiftDate: String? = null
    private var shiftType: String = "day"
    private var currentEmployeeId: String? = null
    private var observeJob: Job? = null

    /** Реальные метрики с бэкенда для текущего сотрудника */
    private val realMetrics = MutableStateFlow<ShiftMetricsResponse?>(null)
    private val realRoute = MutableStateFlow<List<WorkerRouteVisit>>(emptyList())

    init {
        loadContext()
    }

    fun bind(employeeId: String) {
        if (currentEmployeeId == employeeId && observeJob != null) {
            return
        }
        currentEmployeeId = employeeId
        realMetrics.value = null
        realRoute.value = emptyList()
        _state.update {
            it.copy(
                isLoading = true,
                error = null,
                acknowledgedIncidentIds = emptySet(),
                shiftMetrics = null,
                shiftActivity = null,
            )
        }
        observeWorker()
        fetchRealData(employeeId)
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

    private fun fetchRealData(employeeId: String) {
        val currentShiftDate = shiftDate ?: return
        viewModelScope.launch {
            try {
                val shiftsResponse = shiftsApi.getShifts(
                    employeeId = employeeId,
                    dateFrom = currentShiftDate,
                    dateTo = currentShiftDate,
                    pageSize = 1,
                )
                val shift = shiftsResponse.items.firstOrNull() ?: return@launch

                // Загружаем метрики
                try {
                    val metrics = shiftsApi.getShiftMetrics(shift.id)
                    realMetrics.value = metrics
                    _state.update { it.copy(shiftMetrics = metrics) }
                    Timber.d("Loaded metrics for shift ${shift.id}")
                } catch (e: Exception) {
                    Timber.w(e, "Failed to load shift metrics")
                }

                // Загружаем активность
                try {
                    val activity = shiftsApi.getShiftActivity(shift.id)
                    _state.update { it.copy(shiftActivity = activity) }
                    Timber.d("Loaded activity: ${activity.totalIntervals} intervals")
                } catch (e: Exception) {
                    Timber.w(e, "Failed to load shift activity")
                }

                // Загружаем маршрут по зонам
                try {
                    val route = zonesApi.getShiftRoute(shift.id)
                    val routeVisits = route.route.mapIndexed { index, point ->
                        WorkerRouteVisit(
                            zoneId = point.zoneName ?: point.zoneId,
                            startAt = point.enterTsMs,
                            endAt = point.exitTsMs,
                            current = index == route.route.lastIndex && point.exitTsMs == null,
                        )
                    }
                    realRoute.value = routeVisits
                    Timber.d("Loaded route: ${routeVisits.size} visits")
                } catch (e: Exception) {
                    Timber.w(e, "Failed to load shift route")
                }
            } catch (e: Exception) {
                Timber.w(e, "Failed to load shifts for employee $employeeId")
            }
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
                realMetrics,
                realRoute,
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
                val metrics = values[5] as ShiftMetricsResponse?
                @Suppress("UNCHECKED_CAST")
                val routeVisits = values[6] as List<WorkerRouteVisit>

                val workers = buildWorkerMonitoringSnapshots(
                    employees = employees,
                    devices = devices,
                    bindings = bindings,
                    logs = logs,
                    packets = packets,
                    shiftDate = currentShiftDate,
                    shiftType = shiftType,
                )

                var worker = workers.firstOrNull { it.employeeId == targetEmployeeId }

                // Обогащаем реальными метриками
                if (worker != null && metrics != null) {
                    worker = worker.copy(
                        heartRate = if (metrics.avgHrBpm > 0) metrics.avgHrBpm else worker.heartRate,
                        smrPercent = metrics.productivityPercent.toInt().takeIf { it > 0 } ?: worker.smrPercent,
                        efficiencyPercent = metrics.productivityPercent.toInt().takeIf { it > 0 } ?: worker.efficiencyPercent,
                        activeDurationMinutes = (metrics.onSiteDurationSec / 60).toLong().takeIf { it > 0 } ?: worker.activeDurationMinutes,
                    )
                }

                // Обогащаем реальным маршрутом
                if (worker != null && routeVisits.isNotEmpty()) {
                    worker = worker.copy(route = routeVisits)
                }

                worker
            }.collect { worker ->
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
