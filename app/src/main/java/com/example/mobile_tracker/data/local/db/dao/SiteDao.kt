package com.example.mobile_tracker.data.local.db.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.example.mobile_tracker.data.local.db.entity.SiteEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SiteDao {

    @Query(
        "SELECT * FROM sites " +
            "WHERE status = 'active' ORDER BY name",
    )
    fun observeAll(): Flow<List<SiteEntity>>

    @Query("SELECT * FROM sites WHERE id = :id LIMIT 1")
    suspend fun findById(id: String): SiteEntity?

    @Upsert
    suspend fun upsertAll(sites: List<SiteEntity>)

    @Query("DELETE FROM sites")
    suspend fun deleteAll()
}
