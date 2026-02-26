package com.example.mobile_tracker.presentation.upload

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mobile_tracker.data.ble.BleException
import com.example.mobile_tracker.data.ble.BleProtocol
import com.example.mobile_tracker.data.local.datastore.UserPreferencesManager
import com.example.mobile_tracker.data.local.db.dao.ShiftContextDao
import com.example.mobile_tracker.data.repository.UploadRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber

class UploadViewModel(
    private val bleProtocol: BleProtocol,
    private val uploadRepository: UploadRepository,
    private val shiftContextDao: ShiftContextDao,
    private val userPreferencesManager:
        UserPreferencesManager,
) : ViewModel() {

    private val _state = MutableStateFlow(UploadState())
    val state: StateFlow<UploadState> =
        _state.asStateFlow()

    private val _effect = MutableSharedFlow<UploadEffect>()
    val effect: SharedFlow<UploadEffect> =
        _effect.asSharedFlow()

    private var lastIntent: UploadIntent.StartUpload? =
        null

    fun onIntent(intent: UploadIntent) {
        when (intent) {
            is UploadIntent.StartUpload -> {
                lastIntent = intent
                startUpload(intent)
            }
            is UploadIntent.Retry -> {
                lastIntent?.let { startUpload(it) }
            }
            is UploadIntent.Cancel -> {
                cancelUpload()
            }
            is UploadIntent.DismissError -> {
                _state.update {
                    it.copy(
                        error = null,
                        step = UploadStep.Idle,
                    )
                }
            }
        }
    }

    private fun startUpload(
        intent: UploadIntent.StartUpload,
    ) {
        viewModelScope.launch {
            _state.update {
                UploadState(
                    step = UploadStep.Scanning,
                    deviceId = intent.deviceId,
                    employeeId = intent.employeeId,
                    employeeName = intent.employeeName,
                    bindingId = intent.bindingId,
                )
            }

            try {
                // 1. Сканирование
                val scanResult =
                    bleProtocol.findDevice(intent.deviceId)

                // 2. Подключение
                _state.update {
                    it.copy(step = UploadStep.Connecting)
                }
                bleProtocol.connectAndSetup(scanResult)

                // 3. Чтение метаданных
                _state.update {
                    it.copy(
                        step = UploadStep.ReadingMeta,
                    )
                }
                val meta =
                    bleProtocol.requestPacketMeta()
                _state.update {
                    it.copy(
                        totalChunks = meta.totalChunks,
                        packetId = meta.packetId,
                    )
                }

                // 4. Чтение чанков
                _state.update {
                    it.copy(
                        step = UploadStep.ReadingChunks,
                    )
                }
                val payloadEnc =
                    bleProtocol.readPacketChunks(
                        meta,
                    ) { received, total ->
                        _state.update {
                            it.copy(
                                chunksReceived = received,
                                totalChunks = total,
                            )
                        }
                    }

                // 5. Верификация
                _state.update {
                    it.copy(step = UploadStep.Verifying)
                }

                // 6. ACK
                _state.update {
                    it.copy(step = UploadStep.SendingAck)
                }
                bleProtocol.sendAck(
                    meta.packetId,
                    meta.totalChunks,
                )

                // 7. Сохранение в очередь
                _state.update {
                    it.copy(
                        step = UploadStep.SavingLocally,
                    )
                }
                val ctx =
                    shiftContextDao.get()
                uploadRepository.enqueuePacket(
                    meta = meta,
                    payloadEnc = payloadEnc,
                    employeeId = intent.employeeId,
                    bindingId = intent.bindingId,
                    siteId = ctx?.siteId ?: "",
                )

                // 8. Попытка отправки на сервер
                _state.update {
                    it.copy(
                        step =
                            UploadStep.UploadingToServer,
                    )
                }
                val prefs = userPreferencesManager
                    .userPreferences.first()
                val uploaded =
                    uploadRepository.tryUploadPacket(
                        packetId = meta.packetId,
                        operatorId = prefs.userId,
                        siteId = ctx?.siteId ?: "",
                    )

                // 9. Логирование
                uploadRepository.logUploadOperation(
                    deviceId = intent.deviceId,
                    employeeId = intent.employeeId,
                    siteId = ctx?.siteId ?: "",
                    shiftDate = ctx?.shiftDate ?: "",
                    status = if (uploaded) "success"
                        else "pending",
                )

                // Отключаем BLE
                bleProtocol.disconnect()

                _state.update {
                    it.copy(
                        step = UploadStep.Done,
                        isServerUploaded = uploaded,
                    )
                }
                _effect.emit(UploadEffect.UploadComplete)
            } catch (e: BleException) {
                Timber.e(e, "BLE upload error")
                handleError(e.message ?: "BLE error")
            } catch (e: Exception) {
                Timber.e(e, "Upload error")
                handleError(
                    e.message ?: "Unknown error",
                )
            }
        }
    }

    private fun handleError(message: String) {
        bleProtocol.disconnect()
        _state.update {
            it.copy(
                step = UploadStep.Error,
                error = message,
            )
        }
        viewModelScope.launch {
            _effect.emit(
                UploadEffect.ShowError(message),
            )
        }
    }

    private fun cancelUpload() {
        bleProtocol.disconnect()
        _state.update { UploadState() }
    }

    override fun onCleared() {
        super.onCleared()
        bleProtocol.disconnect()
    }
}
