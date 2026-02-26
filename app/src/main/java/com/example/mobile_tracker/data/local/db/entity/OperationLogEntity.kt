package com.example.mobile_tracker.data.local.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "operation_log",
    indices = [
        Index("site_id", "shift_date"),
        Index("type"),
        Index("device_id"),
        Index("employee_id"),
        Index("created_at"),
    ],
)
data class OperationLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: String,
    @ColumnInfo(name = "device_id")
    val deviceId: String? = null,
    @ColumnInfo(name = "employee_id")
    val employeeId: String? = null,
    @ColumnInfo(name = "employee_name")
    val employeeName: String? = null,
    @ColumnInfo(name = "site_id") val siteId: String,
    @ColumnInfo(name = "shift_date") val shiftDate: String,
    val details: String? = null,
    val status: String = "success",
    @ColumnInfo(name = "error_message")
    val errorMessage: String? = null,
    @ColumnInfo(name = "is_synced")
    val isSynced: Boolean = false,
    @ColumnInfo(name = "created_at") val createdAt: Long,
)
