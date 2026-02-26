package com.example.mobile_tracker.presentation.upload

data class UploadState(
    val step: UploadStep = UploadStep.Idle,
    val deviceId: String = "",
    val employeeId: String? = null,
    val employeeName: String? = null,
    val bindingId: Long? = null,
    val chunksReceived: Int = 0,
    val totalChunks: Int = 0,
    val packetId: String? = null,
    val isServerUploaded: Boolean = false,
    val error: String? = null,
)

enum class UploadStep {
    Idle,
    Scanning,
    Connecting,
    ReadingMeta,
    ReadingChunks,
    Verifying,
    SendingAck,
    SavingLocally,
    UploadingToServer,
    Done,
    Error,
}

sealed interface UploadIntent {
    data class StartUpload(
        val deviceId: String,
        val employeeId: String? = null,
        val employeeName: String? = null,
        val bindingId: Long? = null,
    ) : UploadIntent

    data object Retry : UploadIntent
    data object Cancel : UploadIntent
    data object DismissError : UploadIntent
}

sealed interface UploadEffect {
    data object UploadComplete : UploadEffect
    data class ShowError(val message: String) :
        UploadEffect
}
