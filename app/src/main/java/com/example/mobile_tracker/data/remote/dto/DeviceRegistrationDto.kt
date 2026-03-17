package com.example.mobile_tracker.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class MobileRegisterRequest(
    @SerialName("device_id") val deviceId: String,
    @SerialName("employee_id") val employeeId: String,
    @SerialName("site_id") val siteId: String,
    val model: String? = null,
    val firmware: String? = null,
    @SerialName("app_version") val appVersion: String? = null,
)

@Serializable
data class MobileRegisterResponse(
    @SerialName("device_id") val deviceId: String,
    val status: String,
    @SerialName("binding_id") val bindingId: Long? = null,
)
