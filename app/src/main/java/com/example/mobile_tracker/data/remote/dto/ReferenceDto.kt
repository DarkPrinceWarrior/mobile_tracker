package com.example.mobile_tracker.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class EmployeeDto(
    val id: String? = null,
    val uuid: String? = null,
    @SerialName("full_name") val fullName: String,
    @SerialName("company_id") val companyId: String? = null,
    @SerialName("company_name")
    val companyName: String? = null,
    val position: String? = null,
    @SerialName("pass_number")
    val passNumber: String? = null,
    @SerialName("personnel_number")
    val personnelNumber: String? = null,
    @SerialName("brigade_id")
    val brigadeId: String? = null,
    @SerialName("brigade_name")
    val brigadeName: String? = null,
    @SerialName("site_id") val siteId: String? = null,
    val status: String = "active",
) {
    /** API документация указывает uuid как основной идентификатор */
    val effectiveId: String
        get() = uuid ?: id ?: ""
}

@Serializable
data class DeviceDto(
    @SerialName("device_id") val deviceId: String,
    @SerialName("serial_number")
    val serialNumber: String? = null,
    val model: String? = null,
    val status: String = "active",
    @SerialName("charge_status")
    val chargeStatus: String = "unknown",
    @SerialName("employee_id")
    val employeeId: String? = null,
    @SerialName("employee_name")
    val employeeName: String? = null,
    @SerialName("site_id") val siteId: String? = null,
    @SerialName("last_sync_at")
    val lastSyncAt: String? = null,
    val firmware: String? = null,
    @SerialName("app_version")
    val appVersion: String? = null,
    val timezone: String? = null,
    @SerialName("last_heartbeat_at")
    val lastHeartbeatAt: String? = null,
    @SerialName("created_at")
    val createdAt: String? = null,
    @SerialName("updated_at")
    val updatedAt: String? = null,
    // ── Detail-only fields (GET /devices/{id}) ──
    @SerialName("last_packet_id")
    val lastPacketId: String? = null,
    @SerialName("last_packet_status")
    val lastPacketStatus: String? = null,
    @SerialName("last_packet_received_at")
    val lastPacketReceivedAt: String? = null,
    @SerialName("battery_level")
    val batteryLevel: Double? = null,
)

@Serializable
data class SiteDto(
    val uuid: String? = null,
    @SerialName("site_id") val siteId: String? = null,
    val id: String? = null,
    val name: String,
    val address: String? = null,
    val timezone: String = "Europe/Moscow",
    val status: String = "active",
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null,
) {
    /** Бизнес-ключ площадки — site_id (приоритет) или id или uuid */
    val effectiveId: String
        get() = siteId ?: id ?: uuid ?: ""
}

@Serializable
data class DowntimeReasonDto(
    val id: String? = null,
    val uuid: String? = null,
    val code: String? = null,
    val name: String,
    val category: String? = null,
    @SerialName("is_active") val isActive: Boolean = true,
    @SerialName("sort_order") val sortOrder: Int = 0,
) {
    /** API документация указывает uuid как основной идентификатор */
    val effectiveId: String
        get() = uuid ?: id ?: ""
}

/**
 * Универсальный ответ пагинации совместимый с реальным бэкендом.
 * Бэкенд возвращает: items, total, page, page_size.
 */
@Serializable
data class PaginatedResponse<T>(
    val items: List<T> = emptyList(),
    val data: List<T> = emptyList(),
    val total: Int = 0,
    @SerialName("total_count") val totalCount: Int = 0,
    val page: Int = 1,
    @SerialName("page_size") val pageSize: Int = 20,
    @SerialName("total_pages") val totalPages: Int = 0,
) {
    /** Список элементов — поддерживает оба формата (items и data) */
    val elements: List<T>
        get() = items.ifEmpty { data }

    /** Общее количество элементов — поддерживает оба формата */
    val totalElements: Int
        get() = if (total > 0) total else totalCount
}
