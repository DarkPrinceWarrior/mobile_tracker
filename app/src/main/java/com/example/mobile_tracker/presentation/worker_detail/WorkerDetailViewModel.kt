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
import com.example.mobile_tracker.data.remote.api.SensorSamplesApi
import com.example.mobile_tracker.data.remote.api.ZonesApi
import com.example.mobile_tracker.data.remote.dto.ShiftActivityResponse
import com.example.mobile_tracker.data.remote.dto.ShiftMetricsResponse
import com.example.mobile_tracker.data.remote.dto.batteryPercent
import com.example.mobile_tracker.data.remote.dto.heartRateBpm
import com.example.mobile_tracker.data.remote.dto.wearOn
import com.example.mobile_tracker.presentation.monitoring.WorkerIncident
import com.example.mobile_tracker.presentation.monitoring.WorkerMonitoringSnapshot
import com.example.mobile_tracker.presentation.monitoring.WorkerRouteVisit
import com.example.mobile_tracker.presentation.monitoring.buildWorkerMonitoringSnapshots
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
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

/**
 * Контейнер для реальных сенсорных данных, полученных с бэкенда.
 * null = данные ещё не загружены; значения заменяют мок-данные когда доступны.
 */
data class RealSensorData(
    val watchOn: Boolean? = null,        // last wear event: "on_wrist" / "off_wrist"
    val batteryPercent: Int? = null,     // last battery event level (rounded)
    val heartRateBpm: Int? = null,       // last heart_rate event bpm
)

class WorkerDetailViewModel(
    private val shiftContextDao: ShiftContextDao,
    private val employeeDao: EmployeeDao,
    private val deviceDao: DeviceDao,
    private val bindingDao: BindingDao,
    private val operationLogDao: OperationLogDao,
    private val packetQueueDao: PacketQueueDao,
    private val shiftsApi: ShiftsApi,
    private val zonesApi: ZonesApi,
    private val sensorSamplesApi: SensorSamplesApi,
) : ViewModel() {

    private val _state = MutableStateFlow(WorkerDetailState())
    val state: StateFlow<WorkerDetailState> = _state.asStateFlow()

    private var siteId: String? = null
    private var shiftDate: String? = null
    private var shiftType: String = "day"
    private var currentEmployeeId: String? = null
    private var observeJob: Job? = null
    private var refreshJob: Job? = null
    /** device_id для которого уже загружены сенсорные данные — не перегружаем повторно */
    private var lastFetchedDeviceId: String? = null
    /** shift_id текущего сотрудника — передаётся в sensor-samples запросы */
    private var currentShiftId: String? = null

    /** Реальные метрики с бэкенда */
    private val realMetrics = MutableStateFlow<ShiftMetricsResponse?>(null)
    private val realRoute = MutableStateFlow<List<WorkerRouteVisit>>(emptyList())
    /** Реальные сенсорные данные (wear, battery, hr) */
    private val realSensorData = MutableStateFlow(RealSensorData())

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
        realSensorData.value = RealSensorData()
        currentShiftId = null
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
        startRealtimeRefresh(employeeId)
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

    private suspend fun fetchRealData(employeeId: String) {
        try {
            val shiftsResponse = shiftsApi.getShifts(
                employeeId = employeeId,
                pageSize = 5,
            )
            val shift = shiftsResponse.items.firstOrNull() ?: return

            // Метрики (avg hr, productivity, etc.)
            try {
                val metrics = shiftsApi.getShiftMetrics(shift.id)
                realMetrics.value = metrics
                _state.update { it.copy(shiftMetrics = metrics) }
                Timber.d("Loaded metrics for shift ${shift.id}")
            } catch (e: Exception) {
                Timber.w(e, "Failed to load shift metrics")
            }

            // Активность
            try {
                val activity = shiftsApi.getShiftActivity(shift.id)
                _state.update { it.copy(shiftActivity = activity) }
                Timber.d("Loaded activity: ${activity.totalIntervals} intervals")
            } catch (e: Exception) {
                Timber.w(e, "Failed to load shift activity")
            }

            // Маршрут по зонам
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

            // ── Сенсорные данные грузим по shift_id ──
            currentShiftId = shift.id
            fetchSensorData(shift.id)

        } catch (e: Exception) {
            Timber.w(e, "Failed to load shifts for employee $employeeId")
        }
    }

    private fun startRealtimeRefresh(employeeId: String) {
        refreshJob?.cancel()
        refreshJob = viewModelScope.launch {
            while (isActive && currentEmployeeId == employeeId) {
                fetchRealData(employeeId)
                delay(SENSOR_REFRESH_INTERVAL_MS)
            }
        }
    }

    /**
     * Загружает актуальные сенсорные данные для часов через shift_id.
     * GET /api/v1/sensor-samples/{stream}?shift_id={id}&sort=-ts_ms&limit=1
     * shift_id гарантирует данные только текущей смены (device_id может вернуть данные прошлого владельца).
     */
    private fun fetchSensorData(shiftId: String) {
        viewModelScope.launch {
            var updated = RealSensorData()

            // ⌚ Надеты / сняты: payload.state = "on" | "off"
            try {
                val isOn = sensorSamplesApi.getWear(shiftId)?.wearOn()
                if (isOn != null) {
                    updated = updated.copy(watchOn = isOn)
                    Timber.d("Wear: ${if (isOn) "on" else "off"} (shift=$shiftId)")
                }
            } catch (e: Exception) {
                Timber.w(e, "Failed to load wear for shift $shiftId")
            }

            // 🔋 Заряд: payload.level = 0.0–1.0 → × 100 = %
            try {
                val pct = sensorSamplesApi.getBattery(shiftId)?.batteryPercent()
                if (pct != null) {
                    updated = updated.copy(batteryPercent = pct)
                    Timber.d("Battery: $pct% (shift=$shiftId)")
                }
            } catch (e: Exception) {
                Timber.w(e, "Failed to load battery for shift $shiftId")
            }

            // ❤️ Пульс: stream «heart-rate» (через дефис), payload.bpm
            try {
                val bpm = sensorSamplesApi.getHeartRate(shiftId)?.heartRateBpm()
                if (bpm != null && bpm > 0) {
                    updated = updated.copy(heartRateBpm = bpm)
                    Timber.d("Heart rate: $bpm bpm (shift=$shiftId)")
                }
            } catch (e: Exception) {
                Timber.w(e, "Failed to load heart-rate for shift $shiftId")
            }

            realSensorData.value = updated
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
                bindingDao.observeActive(currentSiteId),
                operationLogDao.observeByShift(currentSiteId, currentShiftDate),
                packetQueueDao.observeUnsent(currentSiteId),
                realMetrics,
                realRoute,
                realSensorData,
            ) { values ->
                @Suppress("UNCHECKED_CAST")
                val employees = values[0] as List<com.example.mobile_tracker.data.local.db.entity.EmployeeEntity>
                @Suppress("UNCHECKED_CAST")
                val devices = values[1] as List<com.example.mobile_tracker.data.local.db.entity.DeviceEntity>
                @Suppress("UNCHECKED_CAST")
                val bindings = values[2] as List<com.example.mobile_tracker.data.local.db.entity.BindingEntity>
                @Suppress("UNCHECKED_CAST")
                val activeBindings = values[3] as List<com.example.mobile_tracker.data.local.db.entity.BindingEntity>
                @Suppress("UNCHECKED_CAST")
                val logs = values[4] as List<com.example.mobile_tracker.data.local.db.entity.OperationLogEntity>
                @Suppress("UNCHECKED_CAST")
                val packets = values[5] as List<com.example.mobile_tracker.data.local.db.entity.PacketQueueEntity>
                val metrics = values[6] as ShiftMetricsResponse?
                @Suppress("UNCHECKED_CAST")
                val routeVisits = values[7] as List<WorkerRouteVisit>
                val sensorData = values[8] as RealSensorData

                val mergedBindings = (bindings + activeBindings)
                    .associateBy { it.id }
                    .values
                    .toList()

                val workers = buildWorkerMonitoringSnapshots(
                    employees = employees,
                    devices = devices,
                    bindings = mergedBindings,
                    logs = logs,
                    packets = packets,
                    shiftDate = currentShiftDate,
                    shiftType = shiftType,
                )

                var worker = workers.firstOrNull { it.employeeId == targetEmployeeId }

                // ── Сенсорные данные загружаются по shift_id через fetchRealData ──
                // observer не триггерит sensor fetch — shift_id нужен, а он есть только после shifts API.

                // Обогащаем метриками
                if (worker != null && metrics != null) {
                    worker = worker.copy(
                        heartRate = if (metrics.avgHrBpm > 0) metrics.avgHrBpm else worker.heartRate,
                        smrPercent = metrics.productivityPercent.toInt().takeIf { it > 0 } ?: worker.smrPercent,
                        efficiencyPercent = metrics.productivityPercent.toInt().takeIf { it > 0 } ?: worker.efficiencyPercent,
                        activeDurationMinutes = (metrics.onSiteDurationSec / 60).toLong().takeIf { it > 0 } ?: worker.activeDurationMinutes,
                    )
                }

                // Обогащаем маршрутом
                if (worker != null && routeVisits.isNotEmpty()) {
                    worker = worker.copy(route = routeVisits)
                }

                // Обогащаем сенсорными данными — sensor data всегда приоритетнее Room/мока
                if (worker != null) {
                    worker = worker.copy(
                        watchOn = sensorData.watchOn ?: worker.watchOn,
                        batteryPercent = sensorData.batteryPercent ?: worker.batteryPercent,
                        // HR из sensor stream перезаписывает всё кроме свежих метрик бэка
                        heartRate = sensorData.heartRateBpm ?: worker.heartRate,
                    )
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

    override fun onCleared() {
        observeJob?.cancel()
        refreshJob?.cancel()
        super.onCleared()
    }

    private companion object {
        private const val SENSOR_REFRESH_INTERVAL_MS = 30_000L
    }
}
