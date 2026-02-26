package com.example.mobile_tracker.data.local.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "employees",
    indices = [
        Index("personnel_number", unique = true),
        Index("pass_number"),
        Index("site_id"),
        Index("full_name"),
    ],
)
data class EmployeeEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "full_name") val fullName: String,
    @ColumnInfo(name = "company_id") val companyId: String? = null,
    @ColumnInfo(name = "company_name") val companyName: String? = null,
    val position: String? = null,
    @ColumnInfo(name = "pass_number") val passNumber: String? = null,
    @ColumnInfo(name = "personnel_number")
    val personnelNumber: String? = null,
    @ColumnInfo(name = "brigade_id") val brigadeId: String? = null,
    @ColumnInfo(name = "brigade_name") val brigadeName: String? = null,
    @ColumnInfo(name = "site_id") val siteId: String? = null,
    val status: String = "active",
    @ColumnInfo(name = "synced_at") val syncedAt: Long = 0L,
)
