package com.example.mobile_tracker.data.local.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "shift_context")
data class ShiftContextEntity(
    @PrimaryKey val id: Int = 1,
    @ColumnInfo(name = "site_id") val siteId: String,
    @ColumnInfo(name = "site_name") val siteName: String,
    @ColumnInfo(name = "shift_date") val shiftDate: String,
    @ColumnInfo(name = "shift_type")
    val shiftType: String = "day",
    @ColumnInfo(name = "operator_id") val operatorId: String,
    @ColumnInfo(name = "operator_name")
    val operatorName: String,
    @ColumnInfo(name = "updated_at") val updatedAt: Long = 0L,
)
