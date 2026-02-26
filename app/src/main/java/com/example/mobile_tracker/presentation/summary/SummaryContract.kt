package com.example.mobile_tracker.presentation.summary

data class SummaryState(
    val isLoading: Boolean = true,
    val error: String? = null,
    val issuedCount: Int = 0,
    val returnedCount: Int = 0,
    val notReturnedCount: Int = 0,
    val dataUploadedCount: Int = 0,
    val pendingPacketsCount: Int = 0,
    val errorPacketsCount: Int = 0,
    val unsyncedBindingsCount: Int = 0,
    val siteName: String = "",
    val shiftDate: String = "",
    val shiftType: String = "day",
)

sealed interface SummaryIntent {
    data object Refresh : SummaryIntent
    data object DismissError : SummaryIntent
}

sealed interface SummaryEffect {
    data class ShowError(val message: String) : SummaryEffect
}
