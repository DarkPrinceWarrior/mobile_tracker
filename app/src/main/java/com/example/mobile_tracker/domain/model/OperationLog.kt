package com.example.mobile_tracker.domain.model

data class OperationLog(
    val id: Long = 0,
    val type: String,
    val deviceId: String? = null,
    val employeeId: String? = null,
    val employeeName: String? = null,
    val siteId: String,
    val shiftDate: String,
    val details: String? = null,
    val status: String = "success",
    val errorMessage: String? = null,
    val isSynced: Boolean = false,
    val createdAt: Long,
)
