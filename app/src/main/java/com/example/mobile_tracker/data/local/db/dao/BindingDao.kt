package com.example.mobile_tracker.data.local.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.example.mobile_tracker.data.local.db.entity.BindingEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BindingDao {

    @Query(
        "SELECT * FROM bindings " +
            "WHERE site_id = :siteId AND shift_date = :date " +
            "ORDER BY bound_at DESC",
    )
    fun observeByShift(
        siteId: String,
        date: String,
    ): Flow<List<BindingEntity>>

    @Query(
        "SELECT * FROM bindings " +
            "WHERE status = 'active' AND site_id = :siteId",
    )
    fun observeActive(
        siteId: String,
    ): Flow<List<BindingEntity>>

    @Query(
        "SELECT * FROM bindings " +
            "WHERE device_id = :deviceId " +
            "AND status = 'active' LIMIT 1",
    )
    suspend fun findActiveByDevice(
        deviceId: String,
    ): BindingEntity?

    @Query(
        "SELECT * FROM bindings " +
            "WHERE employee_id = :empId " +
            "AND status = 'active' LIMIT 1",
    )
    suspend fun findActiveByEmployee(
        empId: String,
    ): BindingEntity?

    @Query(
        "SELECT * FROM bindings WHERE id = :id LIMIT 1",
    )
    suspend fun findActiveByIdSync(id: Long): BindingEntity?

    @Insert
    suspend fun insert(binding: BindingEntity): Long

    @Update
    suspend fun update(binding: BindingEntity)

    @Query(
        "UPDATE bindings SET status = 'closed', " +
            "unbound_at = :unboundAt, is_synced = 0 " +
            "WHERE id = :id",
    )
    suspend fun closeBinding(id: Long, unboundAt: Long)

    @Query(
        "UPDATE bindings SET data_uploaded = 1 " +
            "WHERE id = :id",
    )
    suspend fun markDataUploaded(id: Long)

    @Query("SELECT * FROM bindings WHERE is_synced = 0")
    suspend fun getUnsynced(): List<BindingEntity>
}
