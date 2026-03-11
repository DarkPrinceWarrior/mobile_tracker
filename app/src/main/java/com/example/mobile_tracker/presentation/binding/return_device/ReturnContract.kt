package com.example.mobile_tracker.presentation.binding.return_device

import com.example.mobile_tracker.domain.model.DeviceBinding

enum class ReturnProblemReason {
    Lost,
    Faulty,
    NoConnection,
    Other,
}

data class ReturnState(
    val activeBindings: List<DeviceBinding> = emptyList(),
    val selectedBinding: DeviceBinding? = null,
    val selectedBindingId: Long? = null,
    val isLoading: Boolean = false,
    val isReturning: Boolean = false,
    val error: String? = null,
    val showConfirmWithoutUpload: Boolean = false,
    val showProblemDialog: Boolean = false,
    val selectedProblemReason: ReturnProblemReason = ReturnProblemReason.Lost,
    val problemComment: String = "",
)

sealed interface ReturnIntent {
    data class SelectBinding(
        val binding: DeviceBinding,
    ) : ReturnIntent

    data class ApplyScannedDevice(
        val value: String,
    ) : ReturnIntent

    data object ConfirmReturn : ReturnIntent
    data object CancelReturn : ReturnIntent

    data object ConfirmReturnWithoutUpload : ReturnIntent
    data object DismissConfirmDialog : ReturnIntent

    data class OpenProblemFlow(
        val binding: DeviceBinding,
    ) : ReturnIntent

    data class SelectProblemReason(
        val reason: ReturnProblemReason,
    ) : ReturnIntent

    data class UpdateProblemComment(
        val value: String,
    ) : ReturnIntent

    data object ConfirmProblemReturn : ReturnIntent
    data object DismissProblemDialog : ReturnIntent

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
