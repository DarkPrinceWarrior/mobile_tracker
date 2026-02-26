package com.example.mobile_tracker.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class EmployeeDto(
    val id: String,
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
)

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
)

@Serializable
data class SiteDto(
    val id: String,
    val name: String,
    val address: String? = null,
    val timezone: String = "Europe/Moscow",
    val status: String = "active",
)

@Serializable
data class DowntimeReasonDto(
    val id: String,
    val name: String,
)

@Serializable
data class PaginatedResponse<T>(
    val data: List<T>,
    val page: Int,
    @SerialName("page_size") val pageSize: Int,
    @SerialName("total_count") val totalCount: Int,
    @SerialName("total_pages") val totalPages: Int,
)
