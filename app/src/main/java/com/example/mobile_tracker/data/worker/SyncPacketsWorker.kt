package com.example.mobile_tracker.data.worker

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.mobile_tracker.data.local.db.dao.PacketQueueDao
import com.example.mobile_tracker.data.remote.api.GatewayApi
import com.example.mobile_tracker.data.remote.dto.GatewayDeviceInfo
import com.example.mobile_tracker.data.remote.dto.UploadPacketRequest
import com.example.mobile_tracker.data.remote.dto.UploadPacketResponse
import io.ktor.client.call.body
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import timber.log.Timber
import java.util.concurrent.TimeUnit

class SyncPacketsWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params), KoinComponent {

    private val packetQueueDao: PacketQueueDao by inject()
    private val gatewayApi: GatewayApi by inject()

    override suspend fun doWork(): Result {
        Timber.d("SyncPacketsWorker started")
        val pending = packetQueueDao.getPending()
        if (pending.isEmpty()) {
            Timber.d("No pending packets")
            return Result.success()
        }

        for (packet in pending) {
            try {
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
                    operatorId = "",
                    siteId = packet.siteId,
                    employeeId = packet.employeeId,
                    bindingId = packet.bindingId,
                    gatewayDeviceInfo = GatewayDeviceInfo(
                        model = android.os.Build.MODEL,
                        osVersion = "Android " +
                            android.os.Build.VERSION.RELEASE,
                        appVersion = "1.0.0",
                    ),
                )

                val response =
                    gatewayApi.uploadPacket(request)
                val code = response.status.value

                when {
                    code in 200..202 -> {
                        val body = response
                            .body<UploadPacketResponse>()
                        packetQueueDao.markUploaded(
                            packet.packetId,
                            body.status,
                            System.currentTimeMillis(),
                        )
                        Timber.d(
                            "Packet ${packet.packetId}" +
                                " uploaded",
                        )
                    }
                    code == 409 -> {
                        packetQueueDao.markUploaded(
                            packet.packetId,
                            "accepted",
                            System.currentTimeMillis(),
                        )
                    }
                    code in 400..499 -> {
                        packetQueueDao.updateStatus(
                            packet.packetId,
                            "error",
                            packet.attempt + 1,
                            "HTTP $code",
                        )
                    }
                    else -> {
                        return if (
                            runAttemptCount < MAX_RETRIES
                        ) {
                            Result.retry()
                        } else {
                            Result.failure()
                        }
                    }
                }
            } catch (e: Exception) {
                Timber.e(
                    e,
                    "Packet ${packet.packetId} " +
                        "upload failed",
                )
                packetQueueDao.updateStatus(
                    packet.packetId,
                    "error",
                    packet.attempt + 1,
                    e.message,
                )
            }
        }
        return Result.success()
    }

    companion object {
        const val WORK_NAME = "sync_packets"
        private const val MAX_RETRIES = 5
        private const val REPEAT_INTERVAL_MINUTES = 15L

        fun enqueuePeriodicSync(
            workManager: WorkManager,
        ) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(
                    NetworkType.CONNECTED,
                )
                .build()

            val request =
                PeriodicWorkRequestBuilder<
                    SyncPacketsWorker>(
                    REPEAT_INTERVAL_MINUTES,
                    TimeUnit.MINUTES,
                )
                    .setConstraints(constraints)
                    .setBackoffCriteria(
                        BackoffPolicy.EXPONENTIAL,
                        30L,
                        TimeUnit.SECONDS,
                    )
                    .build()

            workManager.enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request,
            )

            Timber.d(
                "Periodic packet sync scheduled " +
                    "every $REPEAT_INTERVAL_MINUTES min",
            )
        }
    }
}
