package com.example.mobile_tracker.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CreateBindingRequest(
    @SerialName("device_id") val deviceId: String,
    @SerialName("employee_id") val employeeId: String,
    @SerialName("site_id") val siteId: String,
    @SerialName("shift_date") val shiftDate: String,
    @SerialName("shift_type") val shiftType: String = "day",
)

@Serializable
data class BindingResponse(
    val id: Long,
    @SerialName("device_id") val deviceId: String,
    @SerialName("employee_id") val employeeId: String,
    @SerialName("employee_name")
    val employeeName: String? = null,
    @SerialName("site_id") val siteId: String,
    @SerialName("shift_date") val shiftDate: String,
    @SerialName("shift_type")
    val shiftType: String = "day",
    @SerialName("bound_at") val boundAt: String? = null,
    @SerialName("unbound_at")
    val unboundAt: String? = null,
    val status: String = "active",
    @SerialName("data_uploaded")
    val dataUploaded: Boolean = false,
    @SerialName("created_at")
    val createdAt: String? = null,
)

@Serializable
data class CloseBindingRequest(
    @SerialName("unbound_at") val unboundAt: String? = null,
)

@Serializable
data class CloseBindingResponse(
    val id: Long,
    val status: String,
)
