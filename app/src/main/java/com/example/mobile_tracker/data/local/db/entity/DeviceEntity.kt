package com.example.mobile_tracker.data.local.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "devices",
    indices = [
        Index("site_id"),
        Index("local_status"),
        Index("employee_id"),
    ],
)
data class DeviceEntity(
    @PrimaryKey
    @ColumnInfo(name = "device_id") val deviceId: String,
    @ColumnInfo(name = "serial_number")
    val serialNumber: String? = null,
    val model: String? = null,
    val status: String = "active",
    @ColumnInfo(name = "charge_status")
    val chargeStatus: String = "unknown",
    @ColumnInfo(name = "employee_id")
    val employeeId: String? = null,
    @ColumnInfo(name = "employee_name")
    val employeeName: String? = null,
    @ColumnInfo(name = "site_id") val siteId: String? = null,
    @ColumnInfo(name = "last_sync_at")
    val lastSyncAt: String? = null,
    @ColumnInfo(name = "local_status")
    val localStatus: String = "available",
    @ColumnInfo(name = "synced_at") val syncedAt: Long = 0L,
)
