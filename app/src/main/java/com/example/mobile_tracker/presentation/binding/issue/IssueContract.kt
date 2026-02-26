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
    val personnelQuery: String = "",
    val nameQuery: String = "",
    val searchResults: List<Employee> = emptyList(),
    val selectedEmployee: Employee? = null,
    val availableDevices: List<Device> = emptyList(),
    val selectedDevice: Device? = null,
    val isLoading: Boolean = false,
    val isIssuing: Boolean = false,
    val error: String? = null,
    val validationError: String? = null,
    val isSearching: Boolean = false,
)

sealed interface IssueIntent {
    data class UpdatePersonnelQuery(
        val query: String,
    ) : IssueIntent

    data class UpdateNameQuery(
        val query: String,
    ) : IssueIntent

    data object SearchByPersonnel : IssueIntent
    data object SearchByName : IssueIntent

    data class SelectEmployee(
        val employee: Employee,
    ) : IssueIntent

    data object AutoAssignDevice : IssueIntent

    data class SelectDevice(
        val device: Device,
    ) : IssueIntent

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
