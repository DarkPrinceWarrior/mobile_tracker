package com.example.mobile_tracker.data.local.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "packet_queue",
    indices = [
        Index("status"),
        Index("device_id"),
        Index("site_id"),
        Index("created_at"),
    ],
)
data class PacketQueueEntity(
    @PrimaryKey
    @ColumnInfo(name = "packet_id") val packetId: String,
    @ColumnInfo(name = "device_id") val deviceId: String,
    @ColumnInfo(name = "employee_id")
    val employeeId: String? = null,
    @ColumnInfo(name = "binding_id")
    val bindingId: Long? = null,
    @ColumnInfo(name = "site_id") val siteId: String,
    @ColumnInfo(name = "shift_start_ts")
    val shiftStartTs: Long,
    @ColumnInfo(name = "shift_end_ts") val shiftEndTs: Long,
    @ColumnInfo(name = "schema_version")
    val schemaVersion: Int = 1,
    @ColumnInfo(name = "payload_enc") val payloadEnc: String,
    @ColumnInfo(name = "payload_key_enc")
    val payloadKeyEnc: String,
    val iv: String,
    @ColumnInfo(name = "payload_hash") val payloadHash: String,
    @ColumnInfo(name = "payload_size_bytes")
    val payloadSizeBytes: Int? = null,
    val status: String = "pending",
    val attempt: Int = 0,
    @ColumnInfo(name = "last_error")
    val lastError: String? = null,
    @ColumnInfo(name = "server_status")
    val serverStatus: String? = null,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "uploaded_at")
    val uploadedAt: Long? = null,
)
