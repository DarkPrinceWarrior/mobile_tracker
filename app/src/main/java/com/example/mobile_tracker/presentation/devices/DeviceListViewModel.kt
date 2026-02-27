package com.example.mobile_tracker.presentation.devices

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mobile_tracker.data.local.db.dao.DeviceDao
import com.example.mobile_tracker.data.local.db.dao.ShiftContextDao
import com.example.mobile_tracker.data.remote.dto.toDomain
import com.example.mobile_tracker.data.repository.ReferenceRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber

class DeviceListViewModel(
    private val deviceDao: DeviceDao,
    private val shiftContextDao: ShiftContextDao,
    private val repository: ReferenceRepository,
) : ViewModel() {

    private val _state =
        MutableStateFlow(DeviceListState())
    val state: StateFlow<DeviceListState> =
        _state.asStateFlow()

    private val _effect =
        MutableSharedFlow<DeviceListEffect>()
    val effect: SharedFlow<DeviceListEffect> =
        _effect.asSharedFlow()

    private var currentSiteId: String? = null

    init {
        loadDevices()
    }

    fun onIntent(intent: DeviceListIntent) {
        when (intent) {
            is DeviceListIntent.SelectDevice ->
                _state.update {
                    it.copy(
                        selectedDeviceId = intent.deviceId,
                    )
                }
            is DeviceListIntent.FilterByStatus ->
                applyFilter(intent.status)
            is DeviceListIntent.Search ->
                applySearch(intent.query)
            DeviceListIntent.Refresh ->
                loadDevices()
            DeviceListIntent.SyncDevices ->
                syncFromServer()
        }
    }

    private fun loadDevices() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }

            val ctx = shiftContextDao.get()
            if (ctx == null) {
                _state.update {
                    it.copy(
                        isLoading = false,
                        error = "Контекст не выбран",
                    )
                }
                return@launch
            }

            currentSiteId = ctx.siteId

            deviceDao.observeBySite(ctx.siteId)
                .collect { entities ->
                    val all =
                        entities.map { it.toDomain() }
                    val cur = _state.value
                    val filtered = filterList(
                        all,
                        cur.filterStatus,
                        cur.searchQuery,
                    )
                    _state.update {
                        it.copy(
                            allDevices = all,
                            devices = filtered,
                            isLoading = false,
                            error = null,
                            availableCount =
                                all.count { d ->
                                    d.localStatus ==
                                        "available"
                                },
                            issuedCount =
                                all.count { d ->
                                    d.localStatus ==
                                        "issued"
                                },
                            totalCount = all.size,
                        )
                    }
                }
        }
    }

    private fun syncFromServer() {
        val siteId = currentSiteId ?: return
        viewModelScope.launch {
            _state.update { it.copy(isSyncing = true) }
            repository.syncDevices(siteId).fold(
                onSuccess = {
                    _state.update {
                        it.copy(isSyncing = false)
                    }
                    _effect.emit(
                        DeviceListEffect.SyncCompleted,
                    )
                },
                onFailure = { e ->
                    Timber.e(e, "Device sync failed")
                    _state.update {
                        it.copy(
                            isSyncing = false,
                            error = e.message,
                        )
                    }
                    _effect.emit(
                        DeviceListEffect.ShowError(
                            e.message
                                ?: "Ошибка синхронизации",
                        ),
                    )
                },
            )
        }
    }

    private fun applyFilter(status: String?) {
        _state.update { cur ->
            cur.copy(
                filterStatus = status,
                devices = filterList(
                    cur.allDevices,
                    status,
                    cur.searchQuery,
                ),
            )
        }
    }

    private fun applySearch(query: String) {
        _state.update { cur ->
            cur.copy(
                searchQuery = query,
                devices = filterList(
                    cur.allDevices,
                    cur.filterStatus,
                    query,
                ),
            )
        }
    }

    private fun filterList(
        all: List<com.example.mobile_tracker
            .domain.model.Device>,
        status: String?,
        query: String,
    ): List<com.example.mobile_tracker
        .domain.model.Device> =
        all.filter { device ->
            val matchesStatus = status == null ||
                device.localStatus == status
            val matchesQuery = query.isBlank() ||
                device.deviceId.contains(
                    query,
                    ignoreCase = true,
                ) ||
                device.serialNumber?.contains(
                    query,
                    ignoreCase = true,
                ) == true ||
                device.employeeName?.contains(
                    query,
                    ignoreCase = true,
                ) == true
            matchesStatus && matchesQuery
        }
}
