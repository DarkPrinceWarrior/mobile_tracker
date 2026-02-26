package com.example.mobile_tracker.presentation.summary

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mobile_tracker.data.local.db.dao.BindingDao
import com.example.mobile_tracker.data.local.db.dao.PacketQueueDao
import com.example.mobile_tracker.data.local.db.dao.ShiftContextDao
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber

class SummaryViewModel(
    private val bindingDao: BindingDao,
    private val packetQueueDao: PacketQueueDao,
    private val shiftContextDao: ShiftContextDao,
) : ViewModel() {

    private val _state = MutableStateFlow(SummaryState())
    val state: StateFlow<SummaryState> = _state.asStateFlow()

    private val _effect = MutableSharedFlow<SummaryEffect>()
    val effect: SharedFlow<SummaryEffect> =
        _effect.asSharedFlow()

    private var siteId: String = ""
    private var shiftDate: String = ""

    init {
        loadContext()
    }

    fun onIntent(intent: SummaryIntent) {
        when (intent) {
            SummaryIntent.Refresh -> refresh()
            SummaryIntent.DismissError ->
                _state.update { it.copy(error = null) }
        }
    }

    private fun loadContext() {
        viewModelScope.launch {
            val ctx = shiftContextDao.get()
            if (ctx != null) {
                siteId = ctx.siteId
                shiftDate = ctx.shiftDate
                _state.update {
                    it.copy(
                        siteName = ctx.siteName,
                        shiftDate = ctx.shiftDate,
                        shiftType = ctx.shiftType,
                    )
                }
                observeMetrics()
            } else {
                _state.update {
                    it.copy(
                        isLoading = false,
                        error = "Контекст смены не выбран",
                    )
                }
            }
        }
    }

    private fun observeMetrics() {
        viewModelScope.launch {
            try {
                combine(
                    bindingDao.observeByShift(
                        siteId,
                        shiftDate,
                    ),
                    packetQueueDao.observePendingCount(),
                    packetQueueDao.observeErrorCount(),
                ) { bindings, pendingCount, errorCount ->
                    val issued = bindings.size
                    val returned = bindings.count {
                        it.status == "closed"
                    }
                    val notReturned = bindings.count {
                        it.status == "active"
                    }
                    val dataUploaded = bindings.count {
                        it.dataUploaded
                    }
                    val unsynced = bindings.count {
                        !it.isSynced
                    }

                    _state.value.copy(
                        isLoading = false,
                        issuedCount = issued,
                        returnedCount = returned,
                        notReturnedCount = notReturned,
                        dataUploadedCount = dataUploaded,
                        pendingPacketsCount = pendingCount,
                        errorPacketsCount = errorCount,
                        unsyncedBindingsCount = unsynced,
                    )
                }.collect { newState ->
                    _state.value = newState
                }
            } catch (e: Exception) {
                Timber.e(e, "Summary metrics failed")
                _state.update {
                    it.copy(
                        isLoading = false,
                        error = e.message
                            ?: "Ошибка загрузки сводки",
                    )
                }
            }
        }
    }

    private fun refresh() {
        _state.update { it.copy(isLoading = true) }
        observeMetrics()
    }
}
