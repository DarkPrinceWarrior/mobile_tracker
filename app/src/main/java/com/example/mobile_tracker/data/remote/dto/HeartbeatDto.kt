package com.example.mobile_tracker.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

// ─── POST /watch/heartbeat ───

@Serializable
data class HeartbeatRequest(
    @SerialName("device_id")
    val deviceId: String,
    @SerialName("device_time_ms")
    val deviceTimeMs: Long,
    @SerialName("battery_level")
    val batteryLevel: Double,
    @SerialName("is_collecting")
    val isCollecting: Boolean,
    @SerialName("pending_packets")
    val pendingPackets: Int,
    @SerialName("app_version")
    val appVersion: String,
)

@Serializable
data class HeartbeatResponse(
    @SerialName("server_time")
    val serverTime: String,
    @SerialName("server_time_ms")
    val serverTimeMs: Long,
    @SerialName("time_offset_ms")
    val timeOffsetMs: Long = 0,
    val commands: List<JsonObject> = emptyList(),
)
