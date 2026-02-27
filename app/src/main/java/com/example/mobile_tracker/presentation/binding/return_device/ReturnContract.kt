package com.example.mobile_tracker.presentation.binding.return_device

import com.example.mobile_tracker.domain.model.DeviceBinding

data class ReturnState(
    val activeBindings: List<DeviceBinding> = emptyList(),
    val selectedBinding: DeviceBinding? = null,
    val selectedBindingId: Long? = null,
    val isLoading: Boolean = false,
    val isReturning: Boolean = false,
    val error: String? = null,
    val showConfirmWithoutUpload: Boolean = false,
)

sealed interface ReturnIntent {
    data class SelectBinding(
        val binding: DeviceBinding,
    ) : ReturnIntent

    data object ConfirmReturn : ReturnIntent
    data object CancelReturn : ReturnIntent

    data object ConfirmReturnWithoutUpload : ReturnIntent
    data object DismissConfirmDialog : ReturnIntent

    data class MarkLost(
        val binding: DeviceBinding,
    ) : ReturnIntent

    data object DismissError : ReturnIntent
}

sealed interface ReturnEffect {
    data class ShowSuccess(
        val deviceId: String,
        val employeeName: String,
    ) : ReturnEffect

    data class ShowError(
        val message: String,
    ) : ReturnEffect
}
