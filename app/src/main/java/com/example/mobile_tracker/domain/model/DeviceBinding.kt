package com.example.mobile_tracker.domain.model

data class DeviceBinding(
    val id: Long = 0,
    val serverId: Long? = null,
    val deviceId: String,
    val employeeId: String,
    val employeeName: String,
    val siteId: String,
    val shiftDate: String,
    val shiftType: String = "day",
    val boundAt: Long,
    val unboundAt: Long? = null,
    val status: String = "active",
    val dataUploaded: Boolean = false,
    val isSynced: Boolean = false,
    val createdAt: Long,
)
