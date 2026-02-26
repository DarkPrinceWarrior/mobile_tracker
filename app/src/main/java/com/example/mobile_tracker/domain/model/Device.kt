package com.example.mobile_tracker.domain.model

data class Device(
    val deviceId: String,
    val serialNumber: String? = null,
    val model: String? = null,
    val status: String = "active",
    val chargeStatus: String = "unknown",
    val employeeId: String? = null,
    val employeeName: String? = null,
    val siteId: String? = null,
    val lastSyncAt: String? = null,
    val localStatus: String = "available",
)
