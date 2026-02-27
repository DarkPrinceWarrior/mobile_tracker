package com.example.mobile_tracker.presentation.employees

import com.example.mobile_tracker.domain.model.Employee

data class EmployeeSearchState(
    val query: String = "",
    val results: List<Employee> = emptyList(),
    val selectedEmployeeId: String? = null,
    val isLoading: Boolean = false,
    val isSyncing: Boolean = false,
    val error: String? = null,
    val totalCount: Int = 0,
)

sealed interface EmployeeSearchIntent {
    data class SelectEmployee(
        val employeeId: String,
    ) : EmployeeSearchIntent

    data class UpdateQuery(
        val query: String,
    ) : EmployeeSearchIntent

    data object Search : EmployeeSearchIntent
    data object Refresh : EmployeeSearchIntent
    data object SyncEmployees : EmployeeSearchIntent
}

sealed interface EmployeeSearchEffect {
    data class ShowError(
        val message: String,
    ) : EmployeeSearchEffect

    data object SyncCompleted : EmployeeSearchEffect
}
