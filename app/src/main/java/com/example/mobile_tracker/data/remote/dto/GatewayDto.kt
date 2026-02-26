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
    @SerialName("operator_id") val operatorId: String,
    @SerialName("site_id") val siteId: String,
    @SerialName("employee_id")
    val employeeId: String? = null,
    @SerialName("binding_id")
    val bindingId: Long? = null,
    @SerialName("uploaded_from")
    val uploadedFrom: String = "gateway",
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
    @SerialName("server_time")
    val serverTime: String? = null,
)
