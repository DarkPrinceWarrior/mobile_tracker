package com.example.mobile_tracker.data.local.db.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.example.mobile_tracker.data.local.db.entity.DeviceEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DeviceDao {

    @Query(
        "SELECT * FROM devices " +
            "WHERE site_id = :siteId ORDER BY device_id",
    )
    fun observeBySite(siteId: String): Flow<List<DeviceEntity>>

    @Query(
        "SELECT * FROM devices " +
            "WHERE site_id = :siteId " +
            "AND local_status = 'available' " +
            "AND status = 'active'",
    )
    suspend fun getAvailable(siteId: String): List<DeviceEntity>

    @Query(
        "SELECT * FROM devices " +
            "WHERE site_id = :siteId AND local_status = 'issued'",
    )
    fun observeIssued(
        siteId: String,
    ): Flow<List<DeviceEntity>>

    @Query(
        "SELECT * FROM devices " +
            "WHERE device_id = :deviceId LIMIT 1",
    )
    suspend fun findById(deviceId: String): DeviceEntity?

    @Upsert
    suspend fun upsertAll(devices: List<DeviceEntity>)

    @Query(
        "UPDATE devices SET local_status = :status, " +
            "employee_id = :empId, employee_name = :empName " +
            "WHERE device_id = :deviceId",
    )
    suspend fun updateLocalStatus(
        deviceId: String,
        status: String,
        empId: String?,
        empName: String?,
    )

    @Query(
        "SELECT COUNT(*) FROM devices " +
            "WHERE site_id = :siteId AND local_status = :status",
    )
    suspend fun countByStatus(siteId: String, status: String): Int

    @Query("DELETE FROM devices")
    suspend fun deleteAll()
}
