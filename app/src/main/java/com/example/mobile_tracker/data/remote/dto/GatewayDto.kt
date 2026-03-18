package com.example.mobile_tracker.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UploadPacketRequest(
    @SerialName("packet_id") val packetId: String,
    @SerialName("device_id") val deviceId: String,
    @SerialName("shift_start_ts")
    val shiftStartTs: Long,
    @SerialName("shift_end_ts") val shiftEndTs: Long,
    @SerialName("schema_version")
    val schemaVersion: Int = 1,
    @SerialName("payload_enc") val payloadEnc: String,
    @SerialName("payload_key_enc")
    val payloadKeyEnc: String,
    val iv: String,
    @SerialName("payload_hash")
    val payloadHash: String,
    @SerialName("payload_size_bytes")
    val payloadSizeBytes: Int? = null,
    @SerialName("operator_id") val operatorId: String? = null,
    @SerialName("site_id") val siteId: String? = null,
    @SerialName("binding_id")
    val bindingId: String? = null,
    @SerialName("gateway_device_info")
    val gatewayDeviceInfo: GatewayDeviceInfo? = null,
)

@Serializable
data class GatewayDeviceInfo(
    val model: String,
    @SerialName("os_version") val osVersion: String,
    @SerialName("app_version") val appVersion: String,
)

@Serializable
data class UploadPacketResponse(
    @SerialName("packet_id") val packetId: String,
    val status: String,
    @SerialName("received_at")
    val receivedAt: String? = null,
    @SerialName("server_time")
    val serverTime: String? = null,
)
