package com.example.mobile_tracker.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// ─── GET /sites/{site_id}/zones ───

@Serializable
data class ZoneListResponse(
    val items: List<ZoneDto> = emptyList(),
    val total: Int = 0,
    val page: Int = 1,
    @SerialName("page_size") val pageSize: Int = 20,
)

@Serializable
data class ZoneDto(
    val uuid: String,
    @SerialName("site_id")
    val siteId: String? = null,
    val name: String,
    @SerialName("zone_type")
    val zoneType: String? = null,
    @SerialName("productivity_percent")
    val productivityPercent: Int = 100,
    val lat: Double? = null,
    val lon: Double? = null,
    val floor: Int? = null,
    val status: String = "active",
    @SerialName("created_at")
    val createdAt: String? = null,
    @SerialName("updated_at")
    val updatedAt: String? = null,
)

// ─── GET /shifts/{shift_id}/zones ───

@Serializable
data class ShiftZonesResponse(
    @SerialName("shift_id")
    val shiftId: String,
    @SerialName("total_visits")
    val totalVisits: Int = 0,
    @SerialName("total_zones")
    val totalZones: Int = 0,
    val visits: List<ZoneVisit> = emptyList(),
    @SerialName("summary_by_zone")
    val summaryByZone: List<ZoneSummary> = emptyList(),
)

@Serializable
data class ZoneVisit(
    @SerialName("zone_id")
    val zoneId: String,
    @SerialName("zone_name")
    val zoneName: String? = null,
    @SerialName("zone_type")
    val zoneType: String? = null,
    @SerialName("enter_ts_ms")
    val enterTsMs: Long = 0,
    @SerialName("exit_ts_ms")
    val exitTsMs: Long? = null,
    @SerialName("duration_sec")
    val durationSec: Int = 0,
    @SerialName("avg_rssi")
    val avgRssi: Int? = null,
)

@Serializable
data class ZoneSummary(
    @SerialName("zone_id")
    val zoneId: String,
    @SerialName("zone_name")
    val zoneName: String? = null,
    @SerialName("zone_type")
    val zoneType: String? = null,
    @SerialName("total_duration_sec")
    val totalDurationSec: Int = 0,
    @SerialName("visit_count")
    val visitCount: Int = 0,
)

// ─── GET /shifts/{shift_id}/route ───

@Serializable
data class ShiftRouteResponse(
    @SerialName("shift_id")
    val shiftId: String,
    val route: List<RoutePoint> = emptyList(),
)

@Serializable
data class RoutePoint(
    @SerialName("zone_id")
    val zoneId: String,
    @SerialName("zone_name")
    val zoneName: String? = null,
    @SerialName("enter_ts_ms")
    val enterTsMs: Long = 0,
    @SerialName("exit_ts_ms")
    val exitTsMs: Long? = null,
)
