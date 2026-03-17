package com.example.mobile_tracker.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// ─── GET /shifts ───

@Serializable
data class ShiftListResponse(
    val items: List<ShiftListItem> = emptyList(),
    val total: Int = 0,
    val page: Int = 1,
    @SerialName("page_size") val pageSize: Int = 20,
)

@Serializable
data class ShiftSamplesCount(
    val accel: Int = 0,
    val gyro: Int = 0,
    val baro: Int = 0,
    val mag: Int = 0,
    val hr: Int = 0,
    val ble: Int = 0,
    val wear: Int = 0,
    val battery: Int = 0,
    val downtime: Int = 0,
)

@Serializable
data class ShiftListItem(
    val id: String,
    @SerialName("employee_name")
    val employeeName: String? = null,
    @SerialName("packet_id")
    val packetId: String? = null,
    @SerialName("device_id")
    val deviceId: String? = null,
    @SerialName("employee_id")
    val employeeId: String? = null,
    @SerialName("site_id")
    val siteId: String? = null,
    @SerialName("start_ts_ms")
    val startTsMs: Long = 0,
    @SerialName("end_ts_ms")
    val endTsMs: Long = 0,
    @SerialName("duration_minutes")
    val durationMinutes: Int = 0,
    @SerialName("schema_version")
    val schemaVersion: Int = 1,
    @SerialName("device_model")
    val deviceModel: String? = null,
    @SerialName("device_fw")
    val deviceFw: String? = null,
    @SerialName("app_version")
    val appVersion: String? = null,
    val timezone: String? = null,
    @SerialName("server_time_offset_ms")
    val serverTimeOffsetMs: Long? = null,
    val status: String? = null,
    @SerialName("samples_count")
    val samplesCount: ShiftSamplesCount? = null,
    @SerialName("created_at")
    val createdAt: String? = null,
    @SerialName("updated_at")
    val updatedAt: String? = null,
)

// ─── GET /shifts/{id}/metrics ───

@Serializable
data class ShiftActivityBreakdown(
    @SerialName("A1_sec") val a1Sec: Int = 0,
    @SerialName("A1_percent") val a1Percent: Double = 0.0,
    @SerialName("A2_sec") val a2Sec: Int = 0,
    @SerialName("A2_percent") val a2Percent: Double = 0.0,
    @SerialName("B1_sec") val b1Sec: Int = 0,
    @SerialName("B1_percent") val b1Percent: Double = 0.0,
    @SerialName("B2_sec") val b2Sec: Int = 0,
    @SerialName("B2_percent") val b2Percent: Double = 0.0,
    @SerialName("V1_sec") val v1Sec: Int = 0,
    @SerialName("V1_percent") val v1Percent: Double = 0.0,
    @SerialName("V2_sec") val v2Sec: Int = 0,
    @SerialName("V2_percent") val v2Percent: Double = 0.0,
    @SerialName("V3_sec") val v3Sec: Int = 0,
    @SerialName("V3_percent") val v3Percent: Double = 0.0,
    @SerialName("V4_sec") val v4Sec: Int = 0,
    @SerialName("V4_percent") val v4Percent: Double = 0.0,
)

@Serializable
data class ShiftMetricsResponse(
    @SerialName("shift_id")
    val shiftId: String,
    @SerialName("employee_name")
    val employeeName: String? = null,
    @SerialName("site_name")
    val siteName: String? = null,
    @SerialName("shift_duration_sec")
    val shiftDurationSec: Int = 0,
    @SerialName("on_site_duration_sec")
    val onSiteDurationSec: Int = 0,
    @SerialName("productivity_percent")
    val productivityPercent: Double = 0.0,
    @SerialName("v1_percent")
    val v1Percent: Double = 0.0,
    @SerialName("avg_reaction_time_sec")
    val avgReactionTimeSec: Double = 0.0,
    @SerialName("median_reaction_time_sec")
    val medianReactionTimeSec: Double = 0.0,
    @SerialName("activity_breakdown")
    val activityBreakdown: ShiftActivityBreakdown? = null,
    @SerialName("wear_compliance_percent")
    val wearCompliancePercent: Double = 0.0,
    @SerialName("zones_visited")
    val zonesVisited: Int = 0,
    @SerialName("avg_hr_bpm")
    val avgHrBpm: Int = 0,
    @SerialName("anomalies_count")
    val anomaliesCount: Int = 0,
    @SerialName("data_quality_score")
    val dataQualityScore: Double = 0.0,
)

// ─── GET /shifts/{id}/activity ───

@Serializable
data class ShiftActivityResponse(
    @SerialName("shift_id")
    val shiftId: String,
    @SerialName("total_intervals")
    val totalIntervals: Int = 0,
    val intervals: List<ActivityInterval> = emptyList(),
    val summary: ActivitySummary? = null,
)

@Serializable
data class ActivityInterval(
    @SerialName("interval_id")
    val intervalId: String? = null,
    @SerialName("activity_class")
    val activityClass: String,
    @SerialName("start_ts_ms")
    val startTsMs: Long = 0,
    @SerialName("end_ts_ms")
    val endTsMs: Long = 0,
    @SerialName("duration_sec")
    val durationSec: Int = 0,
    @SerialName("zone_id")
    val zoneId: String? = null,
    @SerialName("zone_name")
    val zoneName: String? = null,
    val confidence: Double = 0.0,
)

@Serializable
data class ActivitySummary(
    @SerialName("A1_sec") val a1Sec: Int = 0,
    @SerialName("A2_sec") val a2Sec: Int = 0,
    @SerialName("B1_sec") val b1Sec: Int = 0,
    @SerialName("B2_sec") val b2Sec: Int = 0,
    @SerialName("V1_sec") val v1Sec: Int = 0,
    @SerialName("V2_sec") val v2Sec: Int = 0,
    @SerialName("V3_sec") val v3Sec: Int = 0,
    @SerialName("V4_sec") val v4Sec: Int = 0,
    @SerialName("total_sec") val totalSec: Int = 0,
)
