package com.example.mobile_tracker.presentation.maps

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mobile_tracker.data.local.db.dao.BindingDao
import com.example.mobile_tracker.data.local.db.dao.DeviceDao
import com.example.mobile_tracker.data.local.db.dao.EmployeeDao
import com.example.mobile_tracker.data.local.db.dao.OperationLogDao
import com.example.mobile_tracker.data.local.db.dao.PacketQueueDao
import com.example.mobile_tracker.data.local.db.dao.ShiftContextDao
import com.example.mobile_tracker.data.remote.api.SensorSamplesApi
import com.example.mobile_tracker.data.remote.api.ShiftsApi
import com.example.mobile_tracker.data.remote.api.ZonesApi
import com.example.mobile_tracker.data.remote.dto.ShiftMetricsResponse
import com.example.mobile_tracker.data.remote.dto.ZoneDto
import com.example.mobile_tracker.data.remote.dto.batteryPercent
import com.example.mobile_tracker.data.remote.dto.wearOn
import com.example.mobile_tracker.presentation.monitoring.MonitoringMapMode
import com.example.mobile_tracker.presentation.monitoring.MonitoringZoneDefinition
import com.example.mobile_tracker.presentation.monitoring.MonitoringZoneSummary
import com.example.mobile_tracker.presentation.monitoring.WorkerMonitoringSnapshot
import com.example.mobile_tracker.presentation.monitoring.WorkerMonitoringStatus
import com.example.mobile_tracker.presentation.monitoring.buildWorkerMonitoringSnapshots
import com.example.mobile_tracker.presentation.monitoring.monitoringZones
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber

private data class WorkerRealtimeSnapshot(
    val batteryPercent: Int? = null,
    val watchOn: Boolean? = null,
)

data class MapsState(
    val siteName: String = "",
    val shiftDate: String = "",
    val shiftType: String = "day",
    val mode: MonitoringMapMode = MonitoringMapMode.Heatmap,
    val isLoading: Boolean = true,
    val workers: List<WorkerMonitoringSnapshot> = emptyList(),
    val zoneSummaries: List<MonitoringZoneSummary> = emptyList(),
    /** Реальные зоны с бэкенда (null = используются захардкоженные) */
    val backendZones: List<ZoneDto> = emptyList(),
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
    private val shiftsApi: ShiftsApi,
    private val zonesApi: ZonesApi,
    private val sensorSamplesApi: SensorSamplesApi,
) : ViewModel() {

    private val _state = MutableStateFlow(MapsState())
    val state: StateFlow<MapsState> = _state.asStateFlow()

    /** Реальные зоны с бэкенда */
    private val realZones = MutableStateFlow<List<ZoneDto>>(emptyList())
    /** Реальные метрики с бэкенда, ключ = employeeId */
    private val realMetrics = MutableStateFlow<Map<String, ShiftMetricsResponse>>(emptyMap())
    /** Реальные battery/wear данные с бэкенда, ключ = employeeId */
    private val realSensors = MutableStateFlow<Map<String, WorkerRealtimeSnapshot>>(emptyMap())

    private var observeJob: Job? = null
    private var refreshJob: Job? = null

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

            startRealtimeRefresh(context.siteId)
            // Загружаем реальные зоны с бэкенда
            fetchBackendZones(context.siteId)

            observeMonitoringData(
                siteId = context.siteId,
                shiftDate = context.shiftDate,
                shiftType = context.shiftType,
            )
        }
    }

    private fun startRealtimeRefresh(siteId: String) {
        refreshJob?.cancel()
        refreshJob = viewModelScope.launch {
            while (isActive) {
                fetchRealtimeWorkerData(siteId)
                delay(REFRESH_INTERVAL_MS)
            }
        }
    }

    private suspend fun fetchRealtimeWorkerData(siteId: String) {
        try {
            val shiftsResponse = shiftsApi.getShifts(pageSize = 100)
            val latestShifts = shiftsResponse.items
                .asSequence()
                .filter { it.employeeId != null && it.siteId == siteId }
                .distinctBy { it.employeeId }
                .toList()

            val metricsMap = mutableMapOf<String, ShiftMetricsResponse>()
            val sensorMap = mutableMapOf<String, WorkerRealtimeSnapshot>()

            for (shift in latestShifts) {
                val employeeId = shift.employeeId ?: continue

                try {
                    val metrics = shiftsApi.getShiftMetrics(shift.id)
                    metricsMap[employeeId] = metrics
                } catch (e: Exception) {
                    Timber.w(e, "Failed to load metrics for shift ${shift.id}")
                }

                try {
                    sensorMap[employeeId] = WorkerRealtimeSnapshot(
                        batteryPercent = sensorSamplesApi.getBattery(shift.id)?.batteryPercent(),
                        watchOn = sensorSamplesApi.getWear(shift.id)?.wearOn(),
                    )
                } catch (e: Exception) {
                    Timber.w(e, "Failed to load battery/wear for shift ${shift.id}")
                }
            }

            realMetrics.value = metricsMap
            realSensors.value = sensorMap
            Timber.d("Maps: loaded realtime data for ${latestShifts.size} shifts")
        } catch (e: Exception) {
            Timber.w(e, "Maps: failed to load realtime worker data")
        }
    }

    private fun fetchBackendZones(siteId: String) {
        viewModelScope.launch {
            try {
                val response = zonesApi.getSiteZones(siteId = siteId, pageSize = 200)
                realZones.value = response.items
                _state.update { it.copy(backendZones = response.items) }
                Timber.d("Loaded ${response.items.size} zones from backend for site $siteId")
            } catch (e: Exception) {
                Timber.w(e, "Failed to load zones from backend, using hardcoded zones")
                // fallback — используем захардкоженные зоны
            }
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
                bindingDao.observeActive(siteId),
                operationLogDao.observeByShift(siteId, shiftDate),
                packetQueueDao.observeUnsent(siteId),
                realZones,
                realMetrics,
                realSensors,
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
                @Suppress("UNCHECKED_CAST")
                val zones = values[6] as List<ZoneDto>
                @Suppress("UNCHECKED_CAST")
                val metrics = values[7] as Map<String, ShiftMetricsResponse>
                @Suppress("UNCHECKED_CAST")
                val sensors = values[8] as Map<String, WorkerRealtimeSnapshot>

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
                    shiftDate = shiftDate,
                    shiftType = shiftType,
                )

                val enrichedWorkers = if (metrics.isNotEmpty()) {
                    workers.map { worker ->
                        if (worker.activeBinding == null || worker.deviceId == null) {
                            return@map worker
                        }
                        val metric = metrics[worker.employeeId] ?: return@map worker
                        worker.copy(
                            heartRate = if (metric.avgHrBpm > 0) metric.avgHrBpm else worker.heartRate,
                            smrPercent = metric.productivityPercent.toInt().takeIf { it > 0 } ?: worker.smrPercent,
                            efficiencyPercent = metric.productivityPercent.toInt().takeIf { it > 0 } ?: worker.efficiencyPercent,
                            activeDurationMinutes = (metric.onSiteDurationSec / 60).toLong().takeIf { it > 0 }
                                ?: worker.activeDurationMinutes,
                        )
                    }
                } else {
                    workers
                }

                val sensorEnrichedWorkers = if (sensors.isNotEmpty()) {
                    enrichedWorkers.map { worker ->
                        if (worker.activeBinding == null || worker.deviceId == null) {
                            return@map worker
                        }
                        val sensor = sensors[worker.employeeId] ?: return@map worker
                        worker.copy(
                            batteryPercent = sensor.batteryPercent ?: worker.batteryPercent,
                            watchOn = sensor.watchOn ?: worker.watchOn,
                        )
                    }
                } else {
                    enrichedWorkers
                }

                // Строим zoneSummaries с учётом реальных зон
                val zoneSummaries = if (zones.isNotEmpty()) {
                    buildZoneSummariesFromBackend(sensorEnrichedWorkers, zones)
                } else {
                    emptyList()
                }

                sensorEnrichedWorkers to zoneSummaries
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

    override fun onCleared() {
        observeJob?.cancel()
        refreshJob?.cancel()
        super.onCleared()
    }

    private companion object {
        private const val REFRESH_INTERVAL_MS = 30_000L
    }
}

/**
 * Строит сводки по зонам из реальных данных бэкенда.
 * Каждая ZoneDto преобразуется в MonitoringZoneDefinition
 * с равномерным распределением по сетке.
 */
private fun buildZoneSummariesFromBackend(
    workers: List<WorkerMonitoringSnapshot>,
    zones: List<ZoneDto>,
): List<MonitoringZoneSummary> {
    val workersByZone = workers
        .filter { it.zoneId != null && it.status != WorkerMonitoringStatus.Offline }
        .groupBy { it.zoneId.orEmpty() }

    // Раскладываем зоны в сетку (макс. 3 столбца)
    val cols = 3.coerceAtMost(zones.size)
    val rows = (zones.size + cols - 1) / cols

    return zones.mapIndexed { index, zone ->
        val col = index % cols
        val row = index / cols
        val widthRatio = 1f / cols
        val heightRatio = 1f / rows

        val zoneWorkers = workersByZone[zone.name].orEmpty() +
            workersByZone[zone.uuid].orEmpty()
        val uniqueWorkers = zoneWorkers.distinctBy { it.employeeId }

        MonitoringZoneSummary(
            zone = MonitoringZoneDefinition(
                id = zone.name,
                xRatio = col * widthRatio + 0.02f,
                yRatio = row * heightRatio + 0.02f,
                widthRatio = widthRatio - 0.04f,
                heightRatio = heightRatio - 0.04f,
            ),
            totalWorkers = uniqueWorkers.size,
            activeWorkers = uniqueWorkers.count { it.status == WorkerMonitoringStatus.Active },
            idleWorkers = uniqueWorkers.count { it.status == WorkerMonitoringStatus.Idle },
        )
    }
}
