package com.example.mobile_tracker.presentation.navigation

import kotlinx.serialization.Serializable

@Serializable
enum class QrScanMode {
    IssueDevice,
    ReturnDevice,
    UploadDevice,
}

@Serializable
enum class NfcScanMode {
    IdentifyEmployee,
}

@Serializable
sealed interface Route {
    @Serializable
    data object Login : Route

    @Serializable
    data object ContextSelection : Route

    @Serializable
    data object Home : Route

    @Serializable
    data object DeviceList : Route

    @Serializable
    data object EmployeeSearch : Route

    @Serializable
    data object Monitoring : Route

    @Serializable
    data object Workers : Route

    @Serializable
    data object Maps : Route

    @Serializable
    data class WorkerDetail(val employeeId: String) : Route

    @Serializable
    data class Issue(
        val scannedDeviceId: String? = null,
        val scannedPassNumber: String? = null,
    ) : Route

    @Serializable
    data class Return(
        val scannedDeviceId: String? = null,
    ) : Route

    @Serializable
    data class Upload(
        val deviceId: String = "",
        val employeeId: String? = null,
        val employeeName: String? = null,
        val bindingId: Long? = null,
    ) : Route

    @Serializable
    data object Journal : Route

    @Serializable
    data class QrScan(
        val mode: QrScanMode,
        val currentDeviceId: String = "",
        val employeeId: String? = null,
        val employeeName: String? = null,
        val bindingId: Long? = null,
    ) : Route

    @Serializable
    data class NfcScan(
        val mode: NfcScanMode,
    ) : Route

    @Serializable
    data object Alerts : Route

    @Serializable
    data object Summary : Route

    @Serializable
    data object Settings : Route
}
