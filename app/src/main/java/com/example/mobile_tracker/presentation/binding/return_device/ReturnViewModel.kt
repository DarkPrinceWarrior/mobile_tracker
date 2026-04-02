package com.example.mobile_tracker.presentation.binding.return_device

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mobile_tracker.data.local.db.dao.BindingDao
import com.example.mobile_tracker.data.local.db.dao.DeviceDao
import com.example.mobile_tracker.data.local.db.dao.ShiftContextDao
import com.example.mobile_tracker.data.repository.ReturnDeviceProblemOutcome
import com.example.mobile_tracker.data.remote.dto.toDomain
import com.example.mobile_tracker.data.repository.BindingRepository
import com.example.mobile_tracker.domain.model.DeviceBinding
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber

class ReturnViewModel(
    private val bindingDao: BindingDao,
    private val deviceDao: DeviceDao,
    private val shiftContextDao: ShiftContextDao,
    private val bindingRepository: BindingRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(ReturnState())
    val state: StateFlow<ReturnState> = _state.asStateFlow()

    private val _effect = MutableSharedFlow<ReturnEffect>()
    val effect: SharedFlow<ReturnEffect> =
        _effect.asSharedFlow()

    private var siteId: String = ""
    private var shiftDate: String = ""
    private var operatorId: String = ""

    init {
        loadContext()
    }

    fun onIntent(intent: ReturnIntent) {
        when (intent) {
            is ReturnIntent.SelectBinding ->
                selectBinding(intent.binding)
            is ReturnIntent.ApplyScannedDevice ->
                applyScannedDevice(intent.value)
            ReturnIntent.ConfirmReturn ->
                confirmReturn()
            ReturnIntent.CancelReturn ->
                cancelReturn()
            ReturnIntent.ConfirmReturnWithoutUpload ->
                doReturn()
            ReturnIntent.DismissConfirmDialog ->
                _state.update {
                    it.copy(
                        showConfirmWithoutUpload = false,
                    )
                }
            is ReturnIntent.OpenProblemFlow ->
                openProblemFlow(intent.binding)
            is ReturnIntent.SelectProblemReason ->
                _state.update { it.copy(selectedProblemReason = intent.reason) }
            is ReturnIntent.UpdateProblemComment ->
                _state.update { it.copy(problemComment = intent.value) }
            ReturnIntent.ConfirmProblemReturn ->
                confirmProblemReturn()
            ReturnIntent.DismissProblemDialog ->
                _state.update {
                    it.copy(
                        showProblemDialog = false,
                        problemComment = "",
                        selectedProblemReason = ReturnProblemReason.Lost,
                    )
                }
            ReturnIntent.DismissError ->
                _state.update { it.copy(error = null) }
        }
    }

    private fun loadContext() {
        viewModelScope.launch {
            val ctx = shiftContextDao.get()
            if (ctx != null) {
                siteId = ctx.siteId
                shiftDate = ctx.shiftDate
                operatorId = ctx.operatorId
                bindingRepository.refreshBindings(
                    siteId = siteId,
                    shiftDate = shiftDate,
                ).onFailure { error ->
                    Timber.w(error, "Bindings refresh failed before return flow")
                }
                observeBindings()
            } else {
                _state.update {
                    it.copy(
                        error = "Контекст смены не выбран",
                    )
                }
            }
        }
    }

    private fun observeBindings() {
        viewModelScope.launch {
            bindingRepository.observeActiveBindings(siteId)
                .collect { bindings ->
                    _state.update {
                        it.copy(
                            activeBindings = bindings,
                            isLoading = false,
                        )
                    }
                }
        }
    }

    private fun selectBinding(binding: DeviceBinding) {
        _state.update {
            it.copy(
                selectedBinding = binding,
                selectedBindingId = binding.id,
                error = null,
            )
        }
    }

    private fun applyScannedDevice(value: String) {
        val normalized = value.trim()
        if (normalized.isBlank()) return
        val matchedBinding = _state.value.activeBindings.firstOrNull { binding ->
            binding.deviceId.equals(normalized, ignoreCase = true)
        }
        if (matchedBinding != null) {
            selectBinding(matchedBinding)
        } else {
            _state.update {
                it.copy(
                    error = "Для этих часов не найдена активная выдача",
                )
            }
        }
    }

    private fun confirmReturn() {
        val binding = _state.value.selectedBinding ?: return
        if (!binding.dataUploaded) {
            _state.update {
                it.copy(showConfirmWithoutUpload = true)
            }
        } else {
            doReturn()
        }
    }

    private fun doReturn() {
        val binding = _state.value.selectedBinding ?: return
        viewModelScope.launch {
            _state.update {
                it.copy(
                    isReturning = true,
                    showConfirmWithoutUpload = false,
                    error = null,
                )
            }
            bindingRepository.returnDevice(
                bindingId = binding.id,
                siteId = siteId,
                shiftDate = shiftDate,
                operatorId = operatorId.ifBlank { null },
            ).fold(
                onSuccess = {
                    _state.update {
                        it.copy(
                            isReturning = false,
                            selectedBinding = null,
                            selectedBindingId = null,
                        )
                    }
                    _effect.emit(
                        ReturnEffect.ShowSuccess(
                            deviceId = binding.deviceId,
                            employeeName =
                                binding.employeeName,
                        ),
                    )
                },
                onFailure = { e ->
                    Timber.e(e, "Return failed")
                    _state.update {
                        it.copy(
                            isReturning = false,
                            error = e.message
                                ?: "Ошибка возврата",
                        )
                    }
                    _effect.emit(
                        ReturnEffect.ShowError(
                            e.message ?: "Ошибка возврата",
                        ),
                    )
                },
            )
        }
    }

    private fun cancelReturn() {
        _state.update {
            it.copy(
                selectedBinding = null,
                selectedBindingId = null,
                error = null,
            )
        }
    }

    private fun openProblemFlow(binding: DeviceBinding) {
        _state.update {
            it.copy(
                selectedBinding = binding,
                selectedBindingId = binding.id,
                showProblemDialog = true,
                selectedProblemReason = ReturnProblemReason.Lost,
                problemComment = "",
                error = null,
            )
        }
    }

    private fun confirmProblemReturn() {
        val binding = _state.value.selectedBinding ?: return
        val reason = _state.value.selectedProblemReason
        val comment = _state.value.problemComment.trim().ifBlank { null }

        viewModelScope.launch {
            _state.update {
                it.copy(
                    isReturning = true,
                    showProblemDialog = false,
                    error = null,
                )
            }
            try {
                bindingRepository.returnDeviceWithProblem(
                    bindingId = binding.id,
                    siteId = siteId,
                    shiftDate = shiftDate,
                    outcome = when (reason) {
                        ReturnProblemReason.Lost -> ReturnDeviceProblemOutcome.Lost
                        ReturnProblemReason.Faulty -> ReturnDeviceProblemOutcome.Faulty
                        ReturnProblemReason.NoConnection -> ReturnDeviceProblemOutcome.NoConnection
                        ReturnProblemReason.Other -> ReturnDeviceProblemOutcome.Other
                    },
                    comment = comment,
                    operatorId = operatorId.ifBlank { null },
                ).getOrThrow()
                _state.update {
                    it.copy(
                        isReturning = false,
                        selectedBinding = null,
                        selectedBindingId = null,
                        selectedProblemReason = ReturnProblemReason.Lost,
                        problemComment = "",
                    )
                }
                _effect.emit(
                    ReturnEffect.ShowSuccess(
                        deviceId = binding.deviceId,
                        employeeName = binding.employeeName,
                    ),
                )
            } catch (e: Exception) {
                Timber.e(e, "Problem return failed")
                _state.update {
                    it.copy(
                        isReturning = false,
                        error = e.message
                            ?: "Ошибка",
                    )
                }
            }
        }
    }
}
