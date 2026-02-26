package com.example.mobile_tracker.data.local.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sites")
data class SiteEntity(
    @PrimaryKey val id: String,
    val name: String,
    val address: String? = null,
    val timezone: String = "Europe/Moscow",
    val status: String = "active",
    @ColumnInfo(name = "synced_at") val syncedAt: Long = 0L,
)
