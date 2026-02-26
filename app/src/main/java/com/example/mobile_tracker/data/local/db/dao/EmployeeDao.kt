package com.example.mobile_tracker.data.local.db.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.example.mobile_tracker.data.local.db.entity.EmployeeEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface EmployeeDao {

    @Query(
        "SELECT * FROM employees " +
            "WHERE site_id = :siteId AND status = 'active' " +
            "ORDER BY full_name",
    )
    fun observeBySite(siteId: String): Flow<List<EmployeeEntity>>

    @Query(
        "SELECT * FROM employees " +
            "WHERE personnel_number = :number LIMIT 1",
    )
    suspend fun findByPersonnelNumber(
        number: String,
    ): EmployeeEntity?

    @Query(
        "SELECT * FROM employees " +
            "WHERE pass_number = :pass LIMIT 1",
    )
    suspend fun findByPassNumber(pass: String): EmployeeEntity?

    @Query(
        "SELECT * FROM employees " +
            "WHERE full_name LIKE '%' || :query || '%' " +
            "AND site_id = :siteId AND status = 'active'",
    )
    suspend fun search(
        query: String,
        siteId: String,
    ): List<EmployeeEntity>

    @Upsert
    suspend fun upsertAll(employees: List<EmployeeEntity>)

    @Query("DELETE FROM employees WHERE synced_at < :before")
    suspend fun deleteStale(before: Long)
}
