package com.example.mobile_tracker.presentation.alerts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mobile_tracker.data.local.db.dao.BindingDao
import com.example.mobile_tracker.data.local.db.dao.OperationLogDao
import com.example.mobile_tracker.data.local.db.dao.PacketQueueDao
import com.example.mobile_tracker.data.local.db.dao.ShiftContextDao
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class AlertSeverity {
    Critical,
    Warning,
    Info,
}

enum class AlertDestination {
    Devices,
    Return,
    Journal,
}

enum class AlertCategory {
    PacketError,
    PacketPending,
    BindingUnsynced,
    BindingUploadRequired,
    LogError,
    LogPending,
}

data class OperatorAlertItem(
    val id: String,
    val severity: AlertSeverity,
    val category: AlertCategory,
    val subject: String,
    val details: String,
    val timestamp: Long,
    val destination: AlertDestination,
)

data class AlertsState(
    val siteName: String = "",
    val shiftDate: String = "",
    val isLoading: Boolean = true,
    val selectedSeverity: AlertSeverity? = null,
    val selectedAlertId: String? = null,
    val alerts: List<OperatorAlertItem> = emptyList(),
    val error: String? = null,
) {
    val filteredAlerts: List<OperatorAlertItem>
        get() = selectedSeverity?.let { severity ->
            alerts.filter { it.severity == severity }
        } ?: alerts

    val criticalCount: Int
        get() = alerts.count { it.severity == AlertSeverity.Critical }

    val warningCount: Int
        get() = alerts.count { it.severity == AlertSeverity.Warning }

    val infoCount: Int
        get() = alerts.count { it.severity == AlertSeverity.Info }
}

sealed interface AlertsIntent {
    data class SelectAlert(val id: String) : AlertsIntent
    data class SetSeverity(val severity: AlertSeverity?) : AlertsIntent
    data object DismissError : AlertsIntent
}

class AlertsViewModel(
    private val shiftContextDao: ShiftContextDao,
    private val packetQueueDao: PacketQueueDao,
    private val bindingDao: BindingDao,
    private val operationLogDao: OperationLogDao,
) : ViewModel() {

    private val _state = MutableStateFlow(AlertsState())
    val state: StateFlow<AlertsState> = _state.asStateFlow()

    private var alertsJob: Job? = null

    init {
        loadContext()
    }

    fun onIntent(intent: AlertsIntent) {
        when (intent) {
            is AlertsIntent.SelectAlert -> _state.update { it.copy(selectedAlertId = intent.id) }
            is AlertsIntent.SetSeverity -> _state.update { state ->
                val filtered = intent.severity?.let { severity ->
                    state.alerts.filter { it.severity == severity }
                } ?: state.alerts
                state.copy(
                    selectedSeverity = intent.severity,
                    selectedAlertId = filtered.firstOrNull()?.id,
                )
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

            observeAlerts(
                siteId = context.siteId,
                shiftDate = context.shiftDate,
            )
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
            ) { packets, bindings, logs ->
                buildList {
                    packets.forEach { packet ->
                        add(
                            OperatorAlertItem(
                                id = "packet-${packet.packetId}",
                                severity = if (packet.status == "error") {
                                    AlertSeverity.Critical
                                } else {
                                    AlertSeverity.Warning
                                },
                                category = if (packet.status == "error") {
                                    AlertCategory.PacketError
                                } else {
                                    AlertCategory.PacketPending
                                },
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
                                    subject = "${binding.deviceId} · ${binding.employeeName}",
                                    details = "Операция выдачи или возврата ещё не подтверждена сервером",
                                    timestamp = binding.createdAt,
                                    destination = AlertDestination.Return,
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
                                    subject = "${binding.deviceId} · ${binding.employeeName}",
                                    details = "Данные с часов ещё не выгружены и могут потребоваться перед возвратом",
                                    timestamp = binding.boundAt,
                                    destination = AlertDestination.Devices,
                                ),
                            )
                        }

                    logs
                        .filter { it.status != "success" }
                        .forEach { log ->
                            add(
                                OperatorAlertItem(
                                    id = "log-${log.id}",
                                    severity = if (log.status == "error") {
                                        AlertSeverity.Critical
                                    } else {
                                        AlertSeverity.Warning
                                    },
                                    category = if (log.status == "error") {
                                        AlertCategory.LogError
                                    } else {
                                        AlertCategory.LogPending
                                    },
                                    subject = listOfNotNull(log.employeeName, log.deviceId)
                                        .joinToString(" · ")
                                        .ifBlank { log.type },
                                    details = log.errorMessage
                                        ?: log.details
                                        ?: "Операция требует внимания оператора",
                                    timestamp = log.createdAt,
                                    destination = AlertDestination.Journal,
                                ),
                            )
                        }
                }.sortedByDescending { it.timestamp }
            }.collect { alerts ->
                _state.update { current ->
                    current.copy(
                        isLoading = false,
                        alerts = alerts,
                        selectedAlertId = current.selectedAlertId
                            ?.takeIf { selectedId -> alerts.any { it.id == selectedId } }
                            ?: alerts.firstOrNull()?.id,
                        error = null,
                    )
                }
            }
        }
    }
}
