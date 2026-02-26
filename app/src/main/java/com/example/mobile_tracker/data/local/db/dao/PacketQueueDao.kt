package com.example.mobile_tracker.data.local.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.mobile_tracker.data.local.db.entity.PacketQueueEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PacketQueueDao {

    @Query(
        "SELECT * FROM packet_queue ORDER BY created_at DESC",
    )
    fun observeAll(): Flow<List<PacketQueueEntity>>

    @Query(
        "SELECT * FROM packet_queue " +
            "WHERE status = 'pending' ORDER BY created_at ASC",
    )
    suspend fun getPending(): List<PacketQueueEntity>

    @Query(
        "SELECT COUNT(*) FROM packet_queue " +
            "WHERE status = 'pending'",
    )
    fun observePendingCount(): Flow<Int>

    @Query(
        "SELECT COUNT(*) FROM packet_queue " +
            "WHERE status = 'error'",
    )
    fun observeErrorCount(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun enqueue(packet: PacketQueueEntity)

    @Query(
        "UPDATE packet_queue SET status = :status, " +
            "attempt = :attempt, last_error = :error " +
            "WHERE packet_id = :packetId",
    )
    suspend fun updateStatus(
        packetId: String,
        status: String,
        attempt: Int,
        error: String?,
    )

    @Query(
        "UPDATE packet_queue SET status = 'uploaded', " +
            "server_status = :serverStatus, " +
            "uploaded_at = :uploadedAt " +
            "WHERE packet_id = :packetId",
    )
    suspend fun markUploaded(
        packetId: String,
        serverStatus: String,
        uploadedAt: Long,
    )

    @Query(
        "DELETE FROM packet_queue " +
            "WHERE status = 'uploaded' AND uploaded_at < :before",
    )
    suspend fun cleanupUploaded(before: Long)

    @Query(
        "SELECT * FROM packet_queue " +
            "WHERE status IN ('pending', 'error') " +
            "AND site_id = :siteId",
    )
    fun observeUnsent(
        siteId: String,
    ): Flow<List<PacketQueueEntity>>
}
