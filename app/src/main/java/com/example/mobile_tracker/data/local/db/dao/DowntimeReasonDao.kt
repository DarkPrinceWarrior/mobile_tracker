package com.example.mobile_tracker.data.local.db.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.example.mobile_tracker.data.local.db.entity.DowntimeReasonEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DowntimeReasonDao {

    @Query("SELECT * FROM downtime_reasons ORDER BY name")
    fun observeAll(): Flow<List<DowntimeReasonEntity>>

    @Query("SELECT * FROM downtime_reasons ORDER BY name")
    suspend fun getAll(): List<DowntimeReasonEntity>

    @Upsert
    suspend fun upsertAll(
        reasons: List<DowntimeReasonEntity>,
    )

    @Query(
        "DELETE FROM downtime_reasons " +
            "WHERE synced_at < :before",
    )
    suspend fun deleteStale(before: Long)
}
