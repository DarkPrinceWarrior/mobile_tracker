package com.example.mobile_tracker.data.repository

import android.os.Build
import com.example.mobile_tracker.BuildConfig
import com.example.mobile_tracker.data.ble.BleProtocol
import com.example.mobile_tracker.data.ble.PacketMeta
import com.example.mobile_tracker.data.local.db.dao.BindingDao
import com.example.mobile_tracker.data.local.db.dao.OperationLogDao
import com.example.mobile_tracker.data.local.db.dao.PacketQueueDao
import com.example.mobile_tracker.data.local.db.entity.OperationLogEntity
import com.example.mobile_tracker.data.local.db.entity.PacketQueueEntity
import com.example.mobile_tracker.data.remote.api.GatewayApi
import com.example.mobile_tracker.data.remote.dto.GatewayDeviceInfo
import com.example.mobile_tracker.data.remote.dto.UploadPacketRequest
import io.ktor.client.call.body
import com.example.mobile_tracker.data.remote.dto.UploadPacketResponse
import kotlinx.coroutines.flow.Flow
import timber.log.Timber

/**
 * Репозиторий выгрузки данных с часов.
 *
 * Полный цикл: BLE-считывание → сохранение в очередь →
 * отправка на сервер.
 */
class UploadRepository(
    private val packetQueueDao: PacketQueueDao,
    private val bindingDao: BindingDao,
    private val operationLogDao: OperationLogDao,
    private val gatewayApi: GatewayApi,
) {

    /**
     * Сохраняет считанный с часов пакет в очередь.
     */
    suspend fun enqueuePacket(
        meta: PacketMeta,
        payloadEnc: String,
        employeeId: String?,
        bindingId: Long?,
        siteId: String,
    ) {
        val entity = PacketQueueEntity(
            packetId = meta.packetId,
            deviceId = meta.deviceId,
            employeeId = employeeId,
            bindingId = bindingId,
            siteId = siteId,
            shiftStartTs = meta.shiftStartTs,
            shiftEndTs = meta.shiftEndTs,
            schemaVersion = meta.schemaVersion,
            payloadEnc = payloadEnc,
            payloadKeyEnc = meta.payloadKeyEnc,
            iv = meta.iv,
            payloadHash = meta.payloadHash,
            payloadSizeBytes = meta.totalSizeBytes,
            status = "pending",
            createdAt = System.currentTimeMillis(),
        )
        packetQueueDao.enqueue(entity)
        Timber.d(
            "Packet ${meta.packetId} enqueued",
        )
    }

    /**
     * Пытается немедленно отправить пакет на сервер.
     *
     * @return true если успешно отправлен (202 / 409).
     */
    suspend fun tryUploadPacket(
        packetId: String,
        operatorId: String,
        siteId: String,
    ): Boolean {
        val packets = packetQueueDao.getPending()
        val packet = packets.find {
            it.packetId == packetId
        } ?: return false

        return try {
            val request = UploadPacketRequest(
                packetId = packet.packetId,
                deviceId = packet.deviceId,
                shiftStartTs = packet.shiftStartTs,
                shiftEndTs = packet.shiftEndTs,
                schemaVersion = packet.schemaVersion,
                payloadEnc = packet.payloadEnc,
                payloadKeyEnc = packet.payloadKeyEnc,
                iv = packet.iv,
                payloadHash = packet.payloadHash,
                operatorId = operatorId,
                siteId = siteId,
                employeeId = packet.employeeId,
                bindingId = packet.bindingId,
                gatewayDeviceInfo = GatewayDeviceInfo(
                    model = Build.MODEL,
                    osVersion = "Android ${Build.VERSION.RELEASE}",
                    appVersion = BuildConfig.VERSION_NAME,
                ),
            )

            val response = gatewayApi.uploadPacket(request)
            val code = response.status.value

            when {
                code in 200..202 -> {
                    val body =
                        response.body<UploadPacketResponse>()
                    packetQueueDao.markUploaded(
                        packet.packetId,
                        body.status,
                        System.currentTimeMillis(),
                    )
                    markBindingUploaded(packet.bindingId)
                    Timber.d(
                        "Packet ${packet.packetId} " +
                            "uploaded: ${body.status}",
                    )
                    true
                }
                code == 409 -> {
                    packetQueueDao.markUploaded(
                        packet.packetId,
                        "accepted",
                        System.currentTimeMillis(),
                    )
                    markBindingUploaded(packet.bindingId)
                    Timber.d(
                        "Packet ${packet.packetId} " +
                            "already accepted (409)",
                    )
                    true
                }
                else -> {
                    packetQueueDao.updateStatus(
                        packet.packetId,
                        "error",
                        packet.attempt + 1,
                        "HTTP $code",
                    )
                    Timber.w(
                        "Packet upload failed: HTTP $code",
                    )
                    false
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Packet upload exception")
            packetQueueDao.updateStatus(
                packet.packetId,
                "error",
                packet.attempt + 1,
                e.message,
            )
            false
        }
    }

    /**
     * Логирует операцию выгрузки в журнал.
     */
    suspend fun logUploadOperation(
        deviceId: String,
        employeeId: String?,
        siteId: String,
        shiftDate: String,
        status: String,
        errorMessage: String? = null,
    ) {
        operationLogDao.insert(
            OperationLogEntity(
                type = "upload",
                deviceId = deviceId,
                employeeId = employeeId,
                siteId = siteId,
                shiftDate = shiftDate,
                status = status,
                errorMessage = errorMessage,
                createdAt = System.currentTimeMillis(),
            ),
        )
    }

    fun observeUnsent(
        siteId: String,
    ): Flow<List<PacketQueueEntity>> =
        packetQueueDao.observeUnsent(siteId)

    fun observePendingCount(): Flow<Int> =
        packetQueueDao.observePendingCount()

    fun observeErrorCount(): Flow<Int> =
        packetQueueDao.observeErrorCount()

    private suspend fun markBindingUploaded(
        bindingId: Long?,
    ) {
        if (bindingId != null) {
            bindingDao.markDataUploaded(bindingId)
        }
    }
}
