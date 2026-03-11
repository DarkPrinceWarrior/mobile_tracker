package com.example.mobile_tracker.presentation.monitoring

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mobile_tracker.data.local.db.dao.BindingDao
import com.example.mobile_tracker.data.local.db.dao.DeviceDao
import com.example.mobile_tracker.data.local.db.dao.EmployeeDao
import com.example.mobile_tracker.data.local.db.dao.OperationLogDao
import com.example.mobile_tracker.data.local.db.dao.PacketQueueDao
import com.example.mobile_tracker.data.local.db.dao.ShiftContextDao
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class MonitoringAlertPreview(
    val id: String,
    val employeeId: String,
    val employeeName: String,
    val severity: WorkerIncidentSeverity,
    val description: String,
    val timestamp: Long,
)

data class MonitoringState(
    val siteName: String = "",
    val shiftDate: String = "",
    val shiftType: String = "day",
    val isLoading: Boolean = true,
    val workers: List<WorkerMonitoringSnapshot> = emptyList(),
    val zoneSummaries: List<MonitoringZoneSummary> = emptyList(),
    val alertsPreview: List<MonitoringAlertPreview> = emptyList(),
    val error: String? = null,
) {
    val activeCount: Int
        get() = workers.count { it.status == WorkerMonitoringStatus.Active }

    val idleCount: Int
        get() = workers.count { it.status == WorkerMonitoringStatus.Idle }

    val offlineCount: Int
        get() = workers.count { it.status == WorkerMonitoringStatus.Offline }

    val totalWorkers: Int
        get() = workers.size

    val activeWorkersPreview: List<WorkerMonitoringSnapshot>
        get() = workers
            .filter { it.status != WorkerMonitoringStatus.Offline }
            .sortedWith(
                compareByDescending<WorkerMonitoringSnapshot> { it.status == WorkerMonitoringStatus.Active }
                    .thenByDescending { it.efficiencyPercent }
                    .thenByDescending { it.smrPercent },
            )
            .take(4)

    val topZoneLabel: String
        get() = zoneSummaries
            .maxByOrNull { it.totalWorkers }
            ?.takeIf { it.totalWorkers > 0 }
            ?.zone
            ?.id
            ?.let(::formatZoneLabel)
            .orEmpty()

    val efficiencyPercent: Int
        get() = if (workers.isEmpty()) {
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
) : ViewModel() {

    private val _state = MutableStateFlow(MonitoringState())
    val state: StateFlow<MonitoringState> = _state.asStateFlow()

    private var observeJob: Job? = null

    init {
        loadContext()
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
                MonitoringState(
                    siteName = state.value.siteName,
                    shiftDate = state.value.shiftDate,
                    shiftType = state.value.shiftType,
                    isLoading = false,
                    workers = workers,
                    zoneSummaries = buildZoneSummaries(workers),
                    alertsPreview = buildAlertPreview(workers),
                    error = null,
                )
            }.collect { newState ->
                _state.value = newState
            }
        }
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
