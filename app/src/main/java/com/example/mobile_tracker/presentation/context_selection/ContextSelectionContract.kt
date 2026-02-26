package com.example.mobile_tracker.presentation.context_selection

import com.example.mobile_tracker.domain.model.Site

data class ContextSelectionState(
    val sites: List<Site> = emptyList(),
    val selectedSite: Site? = null,
    val shiftDate: String = "",
    val shiftType: String = "day",
    val isLoading: Boolean = false,
    val error: String? = null,
)

sealed interface ContextSelectionIntent {
    data class SiteSelected(val site: Site) : ContextSelectionIntent
    data class DateChanged(val date: String) : ContextSelectionIntent
    data class ShiftTypeChanged(
        val type: String,
    ) : ContextSelectionIntent
    data object StartWork : ContextSelectionIntent
}

sealed interface ContextSelectionEffect {
    data object NavigateToHome : ContextSelectionEffect
}
