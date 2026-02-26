package com.example.mobile_tracker.presentation.journal

import com.example.mobile_tracker.data.local.db.entity.OperationLogEntity

data class JournalState(
    val logs: List<OperationLogEntity> = emptyList(),
    val filteredLogs: List<OperationLogEntity> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null,
    val typeFilter: String? = null,
    val statusFilter: String? = null,
    val searchQuery: String = "",
    val availableTypes: List<String> = emptyList(),
)

sealed interface JournalIntent {
    data class SetTypeFilter(val type: String?) : JournalIntent
    data class SetStatusFilter(val status: String?) :
        JournalIntent

    data class SetSearchQuery(val query: String) :
        JournalIntent

    data object Refresh : JournalIntent
    data object DismissError : JournalIntent
}

sealed interface JournalEffect {
    data class ShowError(val message: String) : JournalEffect
}
