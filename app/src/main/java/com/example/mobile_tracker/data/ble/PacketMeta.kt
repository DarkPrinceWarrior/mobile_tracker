package com.example.mobile_tracker.data.ble

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Метаданные пакета, считываемые из характеристики
 * Packet Meta (0000ff05-...).
 */
@Serializable
data class PacketMeta(
    @SerialName("packet_id")
    val packetId: String,
    @SerialName("device_id")
    val deviceId: String,
    @SerialName("shift_start_ts")
    val shiftStartTs: Long,
    @SerialName("shift_end_ts")
    val shiftEndTs: Long,
    @SerialName("schema_version")
    val schemaVersion: Int = 1,
    @SerialName("payload_hash")
    val payloadHash: String,
    @SerialName("total_chunks")
    val totalChunks: Int,
    @SerialName("chunk_size")
    val chunkSize: Int,
    @SerialName("total_size_bytes")
    val totalSizeBytes: Int,
    @SerialName("payload_key_enc")
    val payloadKeyEnc: String,
    val iv: String,
)
