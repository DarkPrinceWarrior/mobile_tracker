package com.example.mobile_tracker.data.ble

import android.annotation.SuppressLint
import android.bluetooth.le.ScanResult
import android.util.Base64
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import timber.log.Timber
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Высокоуровневый BLE-протокол обмена с часами.
 *
 * Объединяет [BleScanner] и [GattClient] для реализации
 * полного цикла: подключение → передача контекста смены →
 * считывание пакета → ACK.
 */
class BleProtocol(
    private val scanner: BleScanner,
    private val gattClient: GattClient,
    private val json: Json = Json {
        ignoreUnknownKeys = true
    },
) {

    /**
     * Контекст смены, передаваемый на часы при начале.
     */
    @kotlinx.serialization.Serializable
    data class ShiftContextPayload(
        val command: String = "start_shift",
        @kotlinx.serialization.SerialName("shift_id")
        val shiftId: String,
        @kotlinx.serialization.SerialName("employee_id")
        val employeeId: String,
        @kotlinx.serialization.SerialName("site_id")
        val siteId: String,
        @kotlinx.serialization.SerialName("start_ts_ms")
        val startTsMs: Long,
        @kotlinx.serialization.SerialName(
            "planned_end_ts_ms",
        )
        val plannedEndTsMs: Long,
        val mode: String = "GATEWAY",
        @kotlinx.serialization.SerialName(
            "downtime_reasons",
        )
        val downtimeReasons: List<DowntimeReasonItem> =
            emptyList(),
    )

    @kotlinx.serialization.Serializable
    data class DowntimeReasonItem(
        val id: String,
        val name: String,
    )

    @kotlinx.serialization.Serializable
    data class AckPayload(
        @kotlinx.serialization.SerialName("packet_id")
        val packetId: String,
        val status: String = "received",
        @kotlinx.serialization.SerialName(
            "chunks_received",
        )
        val chunksReceived: Int,
    )

    /**
     * Сканирует BLE-устройства и находит часы по
     * deviceId (из справочника).
     *
     * @return [ScanResult] найденного устройства.
     * @throws BleException.ScanTimeout если не найдено.
     */
    @SuppressLint("MissingPermission")
    suspend fun findDevice(
        targetDeviceId: String?,
    ): ScanResult {
        try {
            return withTimeout(
                BleConstants.SCAN_TIMEOUT_MS,
            ) {
                scanner.scan().first { result ->
                    val name = result.device.name
                    val address = result.device.address
                    Timber.d(
                        "BLE found: $name ($address)",
                    )
                    targetDeviceId == null ||
                        name?.contains(
                            targetDeviceId,
                        ) == true ||
                        address == targetDeviceId
                }
            }
        } catch (e: TimeoutCancellationException) {
            throw BleException.ScanTimeout(
                "Scan timeout: device not found in " +
                    "${BleConstants.SCAN_TIMEOUT_MS}ms",
            )
        }
    }

    /**
     * Подключается к устройству, обнаруживает сервисы,
     * запрашивает MTU.
     */
    suspend fun connectAndSetup(
        scanResult: ScanResult,
    ) {
        gattClient.connect(scanResult.device)
        gattClient.discoverServices()
        gattClient.requestMtu()
    }

    /**
     * Передаёт контекст смены на часы через
     * характеристику Shift Context.
     */
    suspend fun sendShiftContext(
        payload: ShiftContextPayload,
    ) {
        val jsonStr = json.encodeToString(payload)
        val data = jsonStr.toByteArray(Charsets.UTF_8)
        Timber.d(
            "Sending shift context: ${data.size} bytes",
        )
        gattClient.writeCharacteristic(
            BleConstants.CHAR_SHIFT_CONTEXT, data,
        )
    }

    /**
     * Запрашивает метаданные пакета с часов.
     * 1. Записывает команду `get_packet`
     * 2. Читает Packet Meta
     *
     * @return [PacketMeta] — метаданные пакета.
     */
    suspend fun requestPacketMeta(): PacketMeta {
        val cmd = """{"command":"get_packet"}"""
        gattClient.writeCharacteristic(
            BleConstants.CHAR_PACKET_REQUEST,
            cmd.toByteArray(Charsets.UTF_8),
        )

        val metaBytes = gattClient.readCharacteristic(
            BleConstants.CHAR_PACKET_META,
        )
        val metaJson = metaBytes.toString(Charsets.UTF_8)
        Timber.d("Packet meta: $metaJson")
        return json.decodeFromString(metaJson)
    }

    /**
     * Считывает зашифрованный пакет чанками через
     * Notify на Packet Data.
     *
     * @param meta — метаданные с количеством чанков.
     * @param onProgress — callback (received, total).
     * @return собранный payload в Base64.
     */
    suspend fun readPacketChunks(
        meta: PacketMeta,
        onProgress: (received: Int, total: Int) -> Unit =
            { _, _ -> },
    ): String {
        gattClient.enableNotify(
            BleConstants.CHAR_PACKET_DATA,
        )

        val chunks = Array<ByteArray?>(meta.totalChunks) {
            null
        }
        var received = 0

        try {
            withTimeout(
                meta.totalChunks * 500L + 10_000L,
            ) {
                for (data in gattClient.notifyChannel) {
                    if (data.size <
                        BleConstants.CHUNK_HEADER_SIZE
                    ) {
                        Timber.w(
                            "Chunk too small: " +
                                "${data.size}",
                        )
                        continue
                    }

                    val index = ByteBuffer.wrap(
                        data, 0,
                        BleConstants.CHUNK_HEADER_SIZE,
                    )
                        .order(ByteOrder.BIG_ENDIAN)
                        .short.toInt() and 0xFFFF

                    if (index >= meta.totalChunks) {
                        Timber.w(
                            "Chunk index out of range:" +
                                " $index",
                        )
                        continue
                    }

                    val payload = data.copyOfRange(
                        BleConstants.CHUNK_HEADER_SIZE,
                        data.size,
                    )
                    chunks[index] = payload
                    received++

                    onProgress(received, meta.totalChunks)

                    if (received >= meta.totalChunks) {
                        break
                    }
                }
            }
        } catch (e: TimeoutCancellationException) {
            Timber.e(
                "Chunk read timeout: $received/" +
                    "${meta.totalChunks}",
            )
        }

        if (received < meta.totalChunks) {
            throw BleException.IncompletePacket(
                "Received $received/" +
                    "${meta.totalChunks} chunks",
                received,
                meta.totalChunks,
            )
        }

        val assembled = ByteBuffer.allocate(
            meta.totalSizeBytes,
        )
        for (chunk in chunks) {
            assembled.put(chunk!!)
        }

        return Base64.encodeToString(
            assembled.array(), Base64.NO_WRAP,
        )
    }

    /**
     * Отправляет ACK на часы — подтверждение
     * успешного приёма пакета.
     */
    suspend fun sendAck(
        packetId: String,
        chunksReceived: Int,
    ) {
        val ack = AckPayload(
            packetId = packetId,
            chunksReceived = chunksReceived,
        )
        val data = json.encodeToString(ack)
            .toByteArray(Charsets.UTF_8)
        gattClient.writeCharacteristic(
            BleConstants.CHAR_ACK, data,
        )
        Timber.d("ACK sent for $packetId")
    }

    /**
     * Отправляет NACK — запрос повторной передачи.
     */
    suspend fun sendNack(packetId: String) {
        val nack =
            """{"packet_id":"$packetId","status":"nack"}"""
        gattClient.writeCharacteristic(
            BleConstants.CHAR_ACK,
            nack.toByteArray(Charsets.UTF_8),
        )
        Timber.d("NACK sent for $packetId")
    }

    /** Отключение от часов. */
    fun disconnect() {
        gattClient.disconnect()
    }

}
