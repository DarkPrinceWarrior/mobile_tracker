package com.example.mobile_tracker.presentation.devices

import com.example.mobile_tracker.domain.model.Device

data class DeviceListState(
    val devices: List<Device> = emptyList(),
    val isLoading: Boolean = false,
    val isSyncing: Boolean = false,
    val error: String? = null,
    val filterStatus: String? = null,
    val searchQuery: String = "",
    val availableCount: Int = 0,
    val issuedCount: Int = 0,
    val totalCount: Int = 0,
)

sealed interface DeviceListIntent {
    data class FilterByStatus(
        val status: String?,
    ) : DeviceListIntent

    data class Search(
        val query: String,
    ) : DeviceListIntent

    data object Refresh : DeviceListIntent
    data object SyncDevices : DeviceListIntent
}

sealed interface DeviceListEffect {
    data class ShowError(
        val message: String,
    ) : DeviceListEffect

    data object SyncCompleted : DeviceListEffect
}
