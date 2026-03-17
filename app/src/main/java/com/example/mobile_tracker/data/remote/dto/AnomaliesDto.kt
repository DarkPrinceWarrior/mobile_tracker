package com.example.mobile_tracker.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

// ─── GET /anomalies, GET /shifts/{shift_id}/anomalies ───

@Serializable
data class AnomalyListResponse(
    val items: List<AnomalyItem> = emptyList(),
    val total: Int = 0,
    val page: Int = 1,
    @SerialName("page_size") val pageSize: Int = 20,
)

@Serializable
data class ShiftAnomaliesResponse(
    @SerialName("shift_id")
    val shiftId: String,
    val anomalies: List<AnomalyItem> = emptyList(),
    val total: Int = 0,
)

@Serializable
data class AnomalyItem(
    val id: String,
    @SerialName("shift_id")
    val shiftId: String? = null,
    @SerialName("device_id")
    val deviceId: String? = null,
    @SerialName("employee_id")
    val employeeId: String? = null,
    @SerialName("anomaly_type")
    val anomalyType: String,
    val severity: String = "low",
    @SerialName("start_ts_ms")
    val startTsMs: Long = 0,
    @SerialName("end_ts_ms")
    val endTsMs: Long? = null,
    val description: String? = null,
    val status: String = "open",
    @SerialName("details_json")
    val detailsJson: JsonObject? = null,
    val comment: String? = null,
    @SerialName("resolved_by")
    val resolvedBy: String? = null,
    @SerialName("resolved_at")
    val resolvedAt: String? = null,
    @SerialName("created_at")
    val createdAt: String? = null,
    @SerialName("updated_at")
    val updatedAt: String? = null,
)

// ─── PATCH /anomalies/{id} ───

@Serializable
data class UpdateAnomalyRequest(
    val status: String? = null,
    val comment: String? = null,
    @SerialName("resolved_by")
    val resolvedBy: String? = null,
)
