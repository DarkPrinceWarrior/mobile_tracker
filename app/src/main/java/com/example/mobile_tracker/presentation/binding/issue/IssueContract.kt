package com.example.mobile_tracker.presentation.binding.issue

import com.example.mobile_tracker.domain.model.Device
import com.example.mobile_tracker.domain.model.Employee

enum class IssueStep {
    IDENTIFY_EMPLOYEE,
    SELECT_DEVICE,
    CONFIRM,
}

data class IssueState(
    val step: IssueStep = IssueStep.IDENTIFY_EMPLOYEE,
    val searchQuery: String = "",
    val allEmployees: List<Employee> = emptyList(),
    val filteredEmployees: List<Employee> = emptyList(),
    val isLoadingEmployees: Boolean = false,
    val selectedEmployee: Employee? = null,
    val deviceSearchQuery: String = "",
    val availableDevices: List<Device> = emptyList(),
    val filteredDevices: List<Device> = emptyList(),
    val selectedDevice: Device? = null,
    val isLoading: Boolean = false,
    val isIssuing: Boolean = false,
    val error: String? = null,
    val validationError: String? = null,
)

sealed interface IssueIntent {
    data class UpdateSearchQuery(
        val query: String,
    ) : IssueIntent

    data class UpdateDeviceSearchQuery(
        val query: String,
    ) : IssueIntent

    data class SelectEmployee(
        val employee: Employee,
    ) : IssueIntent

    data class SelectDevice(
        val device: Device,
    ) : IssueIntent

    data class ApplyScannedDevice(
        val value: String,
    ) : IssueIntent

    data object ContinueWithSelectedDevice : IssueIntent

    data object ConfirmIssue : IssueIntent
    data object GoBack : IssueIntent
    data object Reset : IssueIntent
    data object DismissError : IssueIntent
}

sealed interface IssueEffect {
    data class ShowSuccess(
        val employeeName: String,
        val deviceId: String,
    ) : IssueEffect

    data class ShowError(
        val message: String,
    ) : IssueEffect
}
