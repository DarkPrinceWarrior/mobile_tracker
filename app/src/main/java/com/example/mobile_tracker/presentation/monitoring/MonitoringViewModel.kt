package com.example.mobile_tracker.presentation.monitoring

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

data class MonitoringAlertPreview(
    val id: String,
    val employeeId: String,
    val employeeName: String,
    val severity: WorkerIncidentSeverity,
    val description: String,
    val timestamp: Long,
)

private data class WorkerRealtimeSnapshot(
    val batteryPercent: Int? = null,
    val watchOn: Boolean? = null,
)

data class MonitoringState(
    val siteName: String = "",
    val shiftDate: String = "",
    val shiftType: String = "day",
    val isLoading: Boolean = true,
    val isPreview: Boolean = false,
    val workers: List<WorkerMonitoringSnapshot> = emptyList(),
    val zoneSummaries: List<MonitoringZoneSummary> = emptyList(),
    val alertsPreview: List<MonitoringAlertPreview> = emptyList(),
    val error: String? = null,
) {
    val activeCount: Int
        get() = if (isPreview) 24 else workers.count { it.status == WorkerMonitoringStatus.Active }

    val idleCount: Int
        get() = if (isPreview) 8 else workers.count { it.status == WorkerMonitoringStatus.Idle }

    val offlineCount: Int
        get() = if (isPreview) 5 else workers.count { it.status == WorkerMonitoringStatus.Offline }

    val totalWorkers: Int
        get() = if (isPreview) 37 else workers.size

    val activeWorkersPreview: List<WorkerMonitoringSnapshot>
        get() = if (isPreview) {
            workers.take(3)
        } else {
            workers
                .filter { it.status != WorkerMonitoringStatus.Offline }
                .sortedWith(
                    compareByDescending<WorkerMonitoringSnapshot> { it.status == WorkerMonitoringStatus.Active }
                        .thenByDescending { it.efficiencyPercent }
                        .thenByDescending { it.smrPercent },
                )
                .take(4)
        }

    val topZoneLabel: String
        get() = if (isPreview) {
            "\u0417\u043E\u043D\u0430 \u04102"
        } else {
            zoneSummaries
                .maxByOrNull { it.totalWorkers }
                ?.takeIf { it.totalWorkers > 0 }
                ?.zone
                ?.id
                ?.let(::formatZoneLabel)
                .orEmpty()
        }

    val efficiencyPercent: Int
        get() = if (isPreview) {
            75
        } else if (workers.isEmpty()) {
            0
        } else {
            workers.map { it.efficiencyPercent }.average().toInt()
        }
}

class MonitoringViewModel(
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

    private val _state = MutableStateFlow(MonitoringState())
    val state: StateFlow<MonitoringState> = _state.asStateFlow()

    /** Реальные метрики с бэкенда, ключ = employeeId */
    private val realMetrics = MutableStateFlow<Map<String, ShiftMetricsResponse>>(emptyMap())

    /** Реальные зоны с бэкенда */
    private val realZones = MutableStateFlow<List<ZoneDto>>(emptyList())
    /** Реальные battery/wear данные с бэкенда, ключ = employeeId */
    private val realSensors = MutableStateFlow<Map<String, WorkerRealtimeSnapshot>>(emptyMap())

    private var observeJob: Job? = null
    private var refreshJob: Job? = null

    init {
        loadContext()
    }

    private fun loadContext() {
        viewModelScope.launch {
            val context = shiftContextDao.get()
            if (context == null) {
                _state.value = MonitoringState(
                    isLoading = false,
                    error = "Контекст смены не выбран",
                )
                return@launch
            }

            _state.update {
                it.copy(
                    siteName = context.siteName,
                    shiftDate = context.shiftDate,
                    shiftType = context.shiftType,
                )
            }

            // Загружаем реальные данные с бэкенда
            startRealtimeRefresh(context.siteId)
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
            Timber.d("Loaded realtime monitoring data for ${latestShifts.size} shifts")
        } catch (e: Exception) {
            Timber.w(e, "Failed to load realtime monitoring data")
        }
    }

    private fun fetchBackendZones(siteId: String) {
        viewModelScope.launch {
            try {
                val response = zonesApi.getSiteZones(siteId = siteId, pageSize = 200)
                realZones.value = response.items
                Timber.d("Monitoring: Loaded ${response.items.size} zones from backend")
            } catch (e: Exception) {
                Timber.w(e, "Monitoring: Failed to load zones from backend")
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
                realMetrics,
                realZones,
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
                val metrics = values[6] as Map<String, ShiftMetricsResponse>
                @Suppress("UNCHECKED_CAST")
                val zones = values[7] as List<ZoneDto>
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

                // Обогащаем реальными метриками с бэкенда
                val enrichedWorkers = if (metrics.isNotEmpty()) {
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

                val sensorEnrichedWorkers = if (sensors.isNotEmpty()) {
                    enrichedWorkers.map { worker ->
                        val sensor = sensors[worker.employeeId] ?: return@map worker
                        worker.copy(
                            batteryPercent = sensor.batteryPercent ?: worker.batteryPercent,
                            watchOn = sensor.watchOn ?: worker.watchOn,
                        )
                    }
                } else {
                    enrichedWorkers
                }

                if (sensorEnrichedWorkers.isEmpty()) {
                    MonitoringState(
                        siteName = state.value.siteName,
                        shiftDate = state.value.shiftDate,
                        shiftType = state.value.shiftType,
                        isLoading = false,
                        isPreview = false,
                        workers = emptyList(),
                        zoneSummaries = emptyList(),
                        alertsPreview = emptyList(),
                        error = null,
                    )
                } else {
                    val realAlerts = buildAlertPreview(sensorEnrichedWorkers)
                    MonitoringState(
                        siteName = state.value.siteName,
                        shiftDate = state.value.shiftDate,
                        shiftType = state.value.shiftType,
                        isLoading = false,
                        isPreview = false,
                        workers = sensorEnrichedWorkers,
                        zoneSummaries = buildZoneSummariesFromApi(sensorEnrichedWorkers, zones),
                        alertsPreview = realAlerts,
                        error = null,
                    )
                }
            }.collect { newState ->
                _state.value = newState
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

private fun buildZoneSummariesFromApi(
    workers: List<WorkerMonitoringSnapshot>,
    zones: List<ZoneDto>,
): List<MonitoringZoneSummary> {
    if (zones.isEmpty()) return emptyList()

    val cols = 3.coerceAtMost(zones.size)
    val rows = (zones.size + cols - 1) / cols

    return zones.mapIndexed { index, zone ->
        val col = index % cols
        val row = index / cols
        val widthRatio = 1f / cols
        val heightRatio = 1f / rows

        MonitoringZoneSummary(
            zone = MonitoringZoneDefinition(
                id = zone.name,
                xRatio = col * widthRatio + 0.02f,
                yRatio = row * heightRatio + 0.02f,
                widthRatio = widthRatio - 0.04f,
                heightRatio = heightRatio - 0.04f,
            ),
            totalWorkers = 0,
            activeWorkers = 0,
            idleWorkers = 0,
        )
    }
}

private fun buildAlertPreview(
    workers: List<WorkerMonitoringSnapshot>,
    nowMillis: Long = System.currentTimeMillis(),
): List<MonitoringAlertPreview> = workers
    .flatMap { worker ->
        worker.incidents.map { incident ->
            MonitoringAlertPreview(
                id = incident.id,
                employeeId = worker.employeeId,
                employeeName = worker.fullName,
                severity = incident.severity,
                description = incidentDescription(incident.kind, incident.note),
                timestamp = incident.timestamp,
            )
        }
    }
    .filter { it.timestamp >= nowMillis - 60L * 60_000L }
    .sortedWith(
        compareByDescending<MonitoringAlertPreview> { incidentSeverityRank(it.severity) }
            .thenByDescending { it.timestamp },
    )
    .distinctBy { it.employeeId to it.description }
    .take(3)

private fun incidentDescription(kind: WorkerIncidentKind, note: String?): String = note?.takeIf {
    it.isNotBlank()
} ?: when (kind) {
    WorkerIncidentKind.PacketError -> "Ошибка выгрузки данных"
    WorkerIncidentKind.PacketPending -> "Пакет ожидает отправки"
    WorkerIncidentKind.BindingUnsynced -> "Привязка не синхронизирована"
    WorkerIncidentKind.UploadRequired -> "Требуется выгрузка данных"
    WorkerIncidentKind.LowBattery -> "Низкий заряд часов"
    WorkerIncidentKind.InactiveTooLong -> "Бездействие более 20 минут"
    WorkerIncidentKind.WatchDisconnected -> "Снял часы"
    WorkerIncidentKind.OperationError -> "Ошибка операции"
    WorkerIncidentKind.OperationPending -> "Операция ожидает завершения"
}

private fun incidentSeverityRank(severity: WorkerIncidentSeverity): Int = when (severity) {
    WorkerIncidentSeverity.Critical -> 3
    WorkerIncidentSeverity.Warning -> 2
    WorkerIncidentSeverity.Info -> 1
}

// ── Preview / mock data (matches the Figma design mockup) ──────────────────

private fun buildPreviewState(): MonitoringState {
    val now = System.currentTimeMillis()
    return MonitoringState(
        siteName = "Площадка А",
        shiftDate = java.time.LocalDate.now().toString(),
        shiftType = "day",
        isLoading = false,
        isPreview = true,
        workers = buildPreviewWorkers(now),
        zoneSummaries = emptyList(),
        alertsPreview = buildPreviewAlerts(now),
        error = null,
    )
}

private fun buildPreviewWorkers(now: Long): List<WorkerMonitoringSnapshot> = listOf(
    previewWorker("p1", "Сидоро А. А.", "Бетонщик А1", 78, 100, 75, now),
    previewWorker("p2", "Сидоро А. А.", "Бетонщик А1", 78, 100, 75, now),
    previewWorker("p3", "Сидоро А. А.", "Бетонщик А1", 78, 100, 75, now),
)

private fun buildPreviewAlerts(now: Long): List<MonitoringAlertPreview> = listOf(
    MonitoringAlertPreview(
        id = "preview-1",
        employeeId = "p1",
        employeeName = "Петров П. С.",
        severity = WorkerIncidentSeverity.Critical,
        description = "Снял часы",
        timestamp = now - 5L * 60_000L,
    ),
    MonitoringAlertPreview(
        id = "preview-2",
        employeeId = "p2",
        employeeName = "Иванова М. А.",
        severity = WorkerIncidentSeverity.Warning,
        description = "Бездействие более 20 минут",
        timestamp = now - 12L * 60_000L,
    ),
    MonitoringAlertPreview(
        id = "preview-3",
        employeeId = "p3",
        employeeName = "Козлов Д. В.",
        severity = WorkerIncidentSeverity.Info,
        description = "Низкий заряд часов",
        timestamp = now - 25L * 60_000L,
    ),
)

private fun previewWorker(
    id: String,
    name: String,
    role: String,
    heartRate: Int,
    smr: Int,
    efficiency: Int,
    now: Long,
): WorkerMonitoringSnapshot = WorkerMonitoringSnapshot(
    employeeId = id,
    fullName = name,
    roleLabel = role,
    companyName = null,
    brigadeName = null,
    personnelNumber = null,
    passNumber = null,
    status = WorkerMonitoringStatus.Active,
    zoneId = "А2",
    mapXRatio = null,
    mapYRatio = null,
    shiftStartAt = now - 4L * 3_600_000L,
    activeDurationMinutes = 240L,
    heartRate = heartRate,
    temperatureCelsius = 36.6f,
    steps = 4320,
    batteryPercent = 85,
    watchOn = true,
    watchModel = "SmartSite Pro X1",
    deviceId = null,
    watchIssuedAt = null,
    lastSeenAt = now - 60_000L,
    smrPercent = smr,
    efficiencyPercent = efficiency,
    activeBinding = null,
    device = null,
    route = emptyList(),
    incidents = emptyList(),
    recentLogs = emptyList(),
)
