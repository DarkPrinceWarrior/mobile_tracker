package com.example.mobile_tracker.data.local.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "downtime_reasons")
data class DowntimeReasonEntity(
    @PrimaryKey val id: String,
    val name: String,
    @ColumnInfo(name = "synced_at") val syncedAt: Long = 0L,
)
