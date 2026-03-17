package com.example.mobile_tracker.presentation.alerts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mobile_tracker.data.local.db.dao.BindingDao
import com.example.mobile_tracker.data.local.db.dao.OperationLogDao
import com.example.mobile_tracker.data.local.db.dao.PacketQueueDao
import com.example.mobile_tracker.data.local.db.dao.ShiftContextDao
import com.example.mobile_tracker.data.remote.api.AnomaliesApi
import com.example.mobile_tracker.data.remote.dto.AnomalyItem
import com.example.mobile_tracker.data.remote.dto.UpdateAnomalyRequest
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber

enum class AlertSeverity {
    Critical,
    Warning,
    Info,
}

enum class AlertReviewStatus {
    New,
    InProgress,
    Closed,
}

enum class AlertDestination {
    Devices,
    Return,
    Journal,
    WorkerDetail,
}

enum class AlertCategory {
    PacketError,
    PacketPending,
    BindingUnsynced,
    BindingUploadRequired,
    LogError,
    LogPending,
    AnomalyBackend,
}

data class OperatorAlertItem(
    val id: String,
    val severity: AlertSeverity,
    val category: AlertCategory,
    val subject: String,
    val details: String,
    val timestamp: Long,
    val destination: AlertDestination,
    val employeeId: String? = null,
    val deviceId: String? = null,
    val reviewStatus: AlertReviewStatus = AlertReviewStatus.New,
    val comment: String = "",
) {
    val searchBlob: String
        get() = listOfNotNull(
            subject,
            details,
            employeeId,
            deviceId,
        ).joinToString(" ")
}

data class AlertsState(
    val siteName: String = "",
    val shiftDate: String = "",
    val isLoading: Boolean = true,
    val query: String = "",
    val selectedSeverity: AlertSeverity? = null,
    val selectedReviewStatus: AlertReviewStatus? = null,
    val selectedAlertId: String? = null,
    val draftComment: String = "",
    val alerts: List<OperatorAlertItem> = emptyList(),
    val error: String? = null,
) {
    val filteredAlerts: List<OperatorAlertItem>
        get() = alerts.filter { alert ->
            val matchesSeverity = selectedSeverity == null || alert.severity == selectedSeverity
            val matchesReviewStatus = selectedReviewStatus == null || alert.reviewStatus == selectedReviewStatus
            val normalizedQuery = query.trim()
            val matchesQuery = normalizedQuery.isBlank() ||
                alert.searchBlob.contains(normalizedQuery, ignoreCase = true)
            matchesSeverity && matchesReviewStatus && matchesQuery
        }

    val criticalCount: Int
        get() = alerts.count { it.severity == AlertSeverity.Critical }

    val warningCount: Int
        get() = alerts.count { it.severity == AlertSeverity.Warning }

    val infoCount: Int
        get() = alerts.count { it.severity == AlertSeverity.Info }

    val newCount: Int
        get() = alerts.count { it.reviewStatus == AlertReviewStatus.New }

    val inProgressCount: Int
        get() = alerts.count { it.reviewStatus == AlertReviewStatus.InProgress }

    val closedCount: Int
        get() = alerts.count { it.reviewStatus == AlertReviewStatus.Closed }
}

sealed interface AlertsIntent {
    data class SelectAlert(val id: String) : AlertsIntent
    data class SetSeverity(val severity: AlertSeverity?) : AlertsIntent
    data class SetReviewStatusFilter(val status: AlertReviewStatus?) : AlertsIntent
    data class UpdateQuery(val query: String) : AlertsIntent
    data class UpdateDraftComment(val comment: String) : AlertsIntent
    data class UpdateSelectedAlertStatus(val status: AlertReviewStatus) : AlertsIntent
    data object SaveDraftComment : AlertsIntent
    data object DismissError : AlertsIntent
}

class AlertsViewModel(
    private val shiftContextDao: ShiftContextDao,
    private val packetQueueDao: PacketQueueDao,
    private val bindingDao: BindingDao,
    private val operationLogDao: OperationLogDao,
    private val anomaliesApi: AnomaliesApi,
) : ViewModel() {

    private val _state = MutableStateFlow(AlertsState())
    val state: StateFlow<AlertsState> = _state.asStateFlow()

    private val reviewStatusOverrides = MutableStateFlow<Map<String, AlertReviewStatus>>(emptyMap())
    private val commentOverrides = MutableStateFlow<Map<String, String>>(emptyMap())
    private val backendAnomalies = MutableStateFlow<List<OperatorAlertItem>>(emptyList())

    private var alertsJob: Job? = null

    init {
        loadContext()
    }

    fun onIntent(intent: AlertsIntent) {
        when (intent) {
            is AlertsIntent.SelectAlert -> {
                _state.update { current ->
                    val selected = current.filteredAlerts.firstOrNull { it.id == intent.id }
                    current.copy(
                        selectedAlertId = intent.id,
                        draftComment = selected?.comment.orEmpty(),
                    )
                }
            }
            is AlertsIntent.SetSeverity -> {
                _state.update { current ->
                    val updated = current.copy(selectedSeverity = intent.severity)
                    updated.alignSelection()
                }
            }
            is AlertsIntent.SetReviewStatusFilter -> {
                _state.update { current ->
                    val updated = current.copy(selectedReviewStatus = intent.status)
                    updated.alignSelection()
                }
            }
            is AlertsIntent.UpdateQuery -> {
                _state.update { current ->
                    val updated = current.copy(query = intent.query)
                    updated.alignSelection()
                }
            }
            is AlertsIntent.UpdateDraftComment -> _state.update { it.copy(draftComment = intent.comment) }
            is AlertsIntent.UpdateSelectedAlertStatus -> {
                val alertId = _state.value.selectedAlertId ?: return
                reviewStatusOverrides.update { it + (alertId to intent.status) }

                // Если это аномалия с бэкенда — синхронизируем статус
                val alert = _state.value.alerts.firstOrNull { it.id == alertId }
                if (alert?.category == AlertCategory.AnomalyBackend) {
                    syncAnomalyStatus(alertId.removePrefix("anomaly-"), intent.status)
                }

                _state.update { current ->
                    val updated = current.copy()
                    updated.alignSelection()
                }
            }
            AlertsIntent.SaveDraftComment -> {
                val alertId = _state.value.selectedAlertId ?: return
                commentOverrides.update { it + (alertId to _state.value.draftComment.trim()) }
                _state.update { current ->
                    current.copy(error = null)
                }
            }
            AlertsIntent.DismissError -> _state.update { it.copy(error = null) }
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
                )
            }

            // Загружаем реальные аномалии с бэкенда
            fetchBackendAnomalies(context.siteId)

            observeAlerts(
                siteId = context.siteId,
                shiftDate = context.shiftDate,
            )
        }
    }

    private fun fetchBackendAnomalies(siteId: String) {
        viewModelScope.launch {
            try {
                val response = anomaliesApi.getAnomalies(
                    siteId = siteId,
                    status = "open",
                    pageSize = 50,
                )
                backendAnomalies.value = response.items.map { it.toAlertItem() }
                Timber.d("Loaded ${response.items.size} anomalies from backend")
            } catch (e: Exception) {
                Timber.w(e, "Failed to load anomalies from backend")
                // Не критично — локальные алерты продолжат работать
            }
        }
    }

    private fun syncAnomalyStatus(anomalyId: String, status: AlertReviewStatus) {
        viewModelScope.launch {
            try {
                val backendStatus = when (status) {
                    AlertReviewStatus.New -> "open"
                    AlertReviewStatus.InProgress -> "acknowledged"
                    AlertReviewStatus.Closed -> "resolved"
                }
                anomaliesApi.updateAnomaly(
                    anomalyId = anomalyId,
                    request = UpdateAnomalyRequest(status = backendStatus),
                )
                Timber.d("Synced anomaly $anomalyId status → $backendStatus")
            } catch (e: Exception) {
                Timber.w(e, "Failed to sync anomaly status")
            }
        }
    }

    private fun observeAlerts(
        siteId: String,
        shiftDate: String,
    ) {
        alertsJob?.cancel()
        alertsJob = viewModelScope.launch {
            combine(
                packetQueueDao.observeUnsent(siteId),
                bindingDao.observeByShift(siteId, shiftDate),
                operationLogDao.observeByShift(siteId, shiftDate),
                reviewStatusOverrides,
                commentOverrides,
                backendAnomalies,
            ) { values ->
                @Suppress("UNCHECKED_CAST")
                val packets = values[0] as List<com.example.mobile_tracker.data.local.db.entity.PacketQueueEntity>
                @Suppress("UNCHECKED_CAST")
                val bindings = values[1] as List<com.example.mobile_tracker.data.local.db.entity.BindingEntity>
                @Suppress("UNCHECKED_CAST")
                val logs = values[2] as List<com.example.mobile_tracker.data.local.db.entity.OperationLogEntity>
                @Suppress("UNCHECKED_CAST")
                val statusOverrides = values[3] as Map<String, AlertReviewStatus>
                @Suppress("UNCHECKED_CAST")
                val commentOverridesMap = values[4] as Map<String, String>
                @Suppress("UNCHECKED_CAST")
                val anomalyAlerts = values[5] as List<OperatorAlertItem>

                val localAlerts = buildBaseAlerts(
                    packets = packets,
                    bindings = bindings,
                    logs = logs,
                )

                (localAlerts + anomalyAlerts).map { alert ->
                    alert.copy(
                        reviewStatus = statusOverrides[alert.id] ?: alert.reviewStatus,
                        comment = commentOverridesMap[alert.id] ?: alert.comment,
                    )
                }.sortedByDescending { it.timestamp }
            }.collect { alerts ->
                _state.update { current ->
                    val selectedId = current.selectedAlertId
                        ?.takeIf { id -> alerts.any { it.id == id } }
                        ?: alerts.firstOrNull()?.id
                    current.copy(
                        isLoading = false,
                        alerts = alerts,
                        selectedAlertId = selectedId,
                        draftComment = alerts.firstOrNull { it.id == selectedId }?.comment.orEmpty(),
                        error = null,
                    )
                }
            }
        }
    }
}

private fun buildBaseAlerts(
    packets: List<com.example.mobile_tracker.data.local.db.entity.PacketQueueEntity>,
    bindings: List<com.example.mobile_tracker.data.local.db.entity.BindingEntity>,
    logs: List<com.example.mobile_tracker.data.local.db.entity.OperationLogEntity>,
): List<OperatorAlertItem> = buildList {
    packets.forEach { packet ->
        add(
            OperatorAlertItem(
                id = "packet-${packet.packetId}",
                severity = if (packet.status == "error") AlertSeverity.Critical else AlertSeverity.Warning,
                category = if (packet.status == "error") AlertCategory.PacketError else AlertCategory.PacketPending,
                subject = packet.deviceId,
                details = packet.lastError
                    ?: packet.serverStatus
                    ?: if (packet.status == "error") {
                        "Пакет не был отправлен на сервер"
                    } else {
                        "Пакет сохранён локально и ожидает сеть"
                    },
                timestamp = packet.createdAt,
                destination = AlertDestination.Devices,
                employeeId = packet.employeeId,
                deviceId = packet.deviceId,
            ),
        )
    }

    bindings
        .filter { !it.isSynced }
        .forEach { binding ->
            add(
                OperatorAlertItem(
                    id = "binding-sync-${binding.id}",
                    severity = AlertSeverity.Warning,
                    category = AlertCategory.BindingUnsynced,
                    subject = "${binding.employeeName} · ${binding.deviceId}",
                    details = "Операция выдачи или возврата ещё не подтверждена сервером",
                    timestamp = binding.createdAt,
                    destination = AlertDestination.Return,
                    employeeId = binding.employeeId,
                    deviceId = binding.deviceId,
                ),
            )
        }

    bindings
        .filter { it.status == "active" && !it.dataUploaded }
        .forEach { binding ->
            add(
                OperatorAlertItem(
                    id = "binding-upload-${binding.id}",
                    severity = AlertSeverity.Info,
                    category = AlertCategory.BindingUploadRequired,
                    subject = "${binding.employeeName} · ${binding.deviceId}",
                    details = "Данные с часов ещё не выгружены и могут потребоваться перед возвратом",
                    timestamp = binding.boundAt,
                    destination = AlertDestination.WorkerDetail,
                    employeeId = binding.employeeId,
                    deviceId = binding.deviceId,
                ),
            )
        }

    logs
        .filter { it.status != "success" }
        .forEach { log ->
            add(
                OperatorAlertItem(
                    id = "log-${log.id}",
                    severity = if (log.status == "error") AlertSeverity.Critical else AlertSeverity.Warning,
                    category = if (log.status == "error") AlertCategory.LogError else AlertCategory.LogPending,
                    subject = listOfNotNull(log.employeeName, log.deviceId)
                        .joinToString(" · ")
                        .ifBlank { log.type },
                    details = log.errorMessage
                        ?: log.details
                        ?: "Операция требует внимания оператора",
                    timestamp = log.createdAt,
                    destination = if (!log.employeeId.isNullOrBlank()) {
                        AlertDestination.WorkerDetail
                    } else {
                        AlertDestination.Journal
                    },
                    employeeId = log.employeeId,
                    deviceId = log.deviceId,
                ),
            )
        }
}

private fun AlertsState.alignSelection(): AlertsState {
    val selected = filteredAlerts.firstOrNull { it.id == selectedAlertId } ?: filteredAlerts.firstOrNull()
    return copy(
        selectedAlertId = selected?.id,
        draftComment = selected?.comment.orEmpty(),
    )
}

// ── Backend anomaly → OperatorAlertItem ────────────────────

private fun AnomalyItem.toAlertItem(): OperatorAlertItem {
    val severity = when (this.severity) {
        "critical" -> AlertSeverity.Critical
        "high" -> AlertSeverity.Critical
        "medium" -> AlertSeverity.Warning
        else -> AlertSeverity.Info
    }

    val reviewStatus = when (this.status) {
        "acknowledged" -> AlertReviewStatus.InProgress
        "resolved", "false_positive" -> AlertReviewStatus.Closed
        else -> AlertReviewStatus.New
    }

    val description = this.description ?: when (this.anomalyType) {
        "wear_toggle" -> "Частое снятие/надевание часов"
        "off_wrist" -> "Часы сняты с руки"
        "zero_hr" -> "Нулевой пульс — часы не на руке?"
        "data_gap" -> "Пропуск данных"
        "impossible_travel" -> "Невозможное перемещение"
        else -> "Аномалия: ${this.anomalyType}"
    }

    return OperatorAlertItem(
        id = "anomaly-${this.id}",
        severity = severity,
        category = AlertCategory.AnomalyBackend,
        subject = this.deviceId ?: "Устройство",
        details = description,
        timestamp = this.startTsMs,
        destination = AlertDestination.WorkerDetail,
        employeeId = this.employeeId,
        deviceId = this.deviceId,
        reviewStatus = reviewStatus,
        comment = this.comment.orEmpty(),
    )
}

