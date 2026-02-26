package com.example.mobile_tracker.domain.model

data class ShiftPacket(
    val packetId: String,
    val deviceId: String,
    val employeeId: String? = null,
    val bindingId: Long? = null,
    val siteId: String,
    val shiftStartTs: Long,
    val shiftEndTs: Long,
    val schemaVersion: Int = 1,
    val payloadEnc: String,
    val payloadKeyEnc: String,
    val iv: String,
    val payloadHash: String,
    val payloadSizeBytes: Int? = null,
    val status: String = "pending",
    val attempt: Int = 0,
    val lastError: String? = null,
    val serverStatus: String? = null,
    val createdAt: Long,
    val uploadedAt: Long? = null,
)
