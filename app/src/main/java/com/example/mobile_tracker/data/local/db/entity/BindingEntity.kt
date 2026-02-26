package com.example.mobile_tracker.data.local.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "bindings",
    indices = [
        Index("device_id", "status"),
        Index("employee_id", "shift_date"),
        Index("site_id", "shift_date"),
        Index("is_synced"),
    ],
)
data class BindingEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "server_id") val serverId: Long? = null,
    @ColumnInfo(name = "device_id") val deviceId: String,
    @ColumnInfo(name = "employee_id") val employeeId: String,
    @ColumnInfo(name = "employee_name")
    val employeeName: String,
    @ColumnInfo(name = "site_id") val siteId: String,
    @ColumnInfo(name = "shift_date") val shiftDate: String,
    @ColumnInfo(name = "shift_type")
    val shiftType: String = "day",
    @ColumnInfo(name = "bound_at") val boundAt: Long,
    @ColumnInfo(name = "unbound_at") val unboundAt: Long? = null,
    val status: String = "active",
    @ColumnInfo(name = "data_uploaded")
    val dataUploaded: Boolean = false,
    @ColumnInfo(name = "is_synced")
    val isSynced: Boolean = false,
    @ColumnInfo(name = "created_at") val createdAt: Long,
)
