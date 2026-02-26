package com.example.mobile_tracker.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mobile_tracker.data.local.db.dao.ShiftContextDao
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
)

class HomeViewModel(
    private val shiftContextDao: ShiftContextDao,
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
        }
    }
}
