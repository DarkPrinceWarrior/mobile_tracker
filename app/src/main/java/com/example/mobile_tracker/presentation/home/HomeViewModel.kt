package com.example.mobile_tracker.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mobile_tracker.data.local.db.dao.BindingDao
import com.example.mobile_tracker.data.local.db.dao.OperationLogDao
import com.example.mobile_tracker.data.local.db.dao.PacketQueueDao
import com.example.mobile_tracker.data.local.db.dao.ShiftContextDao
import com.example.mobile_tracker.data.repository.BindingRepository
import com.example.mobile_tracker.util.NetworkMonitor
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class HomeState(
    val siteName: String = "",
    val shiftDate: String = "",
    val shiftType: String = "day",
    val operatorName: String = "",
    val issuedCount: Int = 0,
    val returnedCount: Int = 0,
    val isOnline: Boolean = true,
    val pendingPacketsCount: Int = 0,
    val errorPacketsCount: Int = 0,
    val activeBindingsCount: Int = 0,
    val uploadRequiredCount: Int = 0,
    val unsyncedBindingsCount: Int = 0,
    val criticalAlertsCount: Int = 0,
    val warningAlertsCount: Int = 0,
    val infoAlertsCount: Int = 0,
) {
    val totalAlertsCount: Int
        get() = criticalAlertsCount + warningAlertsCount + infoAlertsCount
}

class HomeViewModel(
    private val shiftContextDao: ShiftContextDao,
    private val networkMonitor: NetworkMonitor,
    private val packetQueueDao: PacketQueueDao,
    private val bindingDao: BindingDao,
    private val operationLogDao: OperationLogDao,
    private val bindingRepository: BindingRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(HomeState())
    val state: StateFlow<HomeState> = _state.asStateFlow()

    init {
        loadContext()
    }

    private fun loadContext() {
        viewModelScope.launch {
            val ctx = shiftContextDao.get() ?: return@launch
            _state.update {
                it.copy(
                    siteName = ctx.siteName,
                    shiftDate = ctx.shiftDate,
                    shiftType = ctx.shiftType,
                    operatorName = ctx.operatorName,
                )
            }
            bindingRepository.refreshBindings(
                siteId = ctx.siteId,
                shiftDate = ctx.shiftDate,
            )
            observeOperationalSignals(
                siteId = ctx.siteId,
                shiftDate = ctx.shiftDate,
            )
        }
    }

    private fun observeOperationalSignals(
        siteId: String,
        shiftDate: String,
    ) {
        viewModelScope.launch {
            combine(
                networkMonitor.isOnline,
                packetQueueDao.observeUnsent(siteId),
                bindingDao.observeByShift(siteId, shiftDate),
                bindingDao.observeActive(siteId),
                operationLogDao.observeByShift(siteId, shiftDate),
            ) { isOnline, unsentPackets, shiftBindings, activeBindings, logs ->
                val relevantBindings = (shiftBindings + activeBindings).distinctBy { it.id }
                val issued = relevantBindings.size
                val returned = logs.count { log ->
                    log.status == "success" && (
                        log.type == "return" ||
                            (log.type == "status_change" &&
                                log.details?.startsWith("Возврат с проблемой") == true)
                        )
                }
                val uploadRequired = activeBindings.count {
                    it.status == "active" && !it.dataUploaded
                }
                val unsyncedBindings = relevantBindings.count { !it.isSynced }
                val pendingPackets = unsentPackets.count { it.status == "pending" }
                val errorPackets = unsentPackets.count { it.status == "error" }
                val pendingLogs = logs.count { it.status == "pending" }
                val errorLogs = logs.count { it.status == "error" }

                _state.update {
                    it.copy(
                        issuedCount = issued,
                        returnedCount = returned,
                        isOnline = isOnline,
                        pendingPacketsCount = pendingPackets,
                        errorPacketsCount = errorPackets,
                        activeBindingsCount = activeBindings.size,
                        uploadRequiredCount = uploadRequired,
                        unsyncedBindingsCount = unsyncedBindings,
                        criticalAlertsCount = errorPackets + errorLogs,
                        warningAlertsCount = pendingPackets + pendingLogs + unsyncedBindings,
                        infoAlertsCount = uploadRequired,
                    )
                }
            }.collect { }
        }
    }
}
