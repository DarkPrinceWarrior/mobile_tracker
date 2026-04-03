package com.example.mobile_tracker.data.local.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.mobile_tracker.data.local.db.entity.OperationLogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface OperationLogDao {

    @Query(
        "SELECT * FROM operation_log " +
            "WHERE site_id = :siteId AND shift_date = :date " +
            "ORDER BY created_at DESC",
    )
    fun observeByShift(
        siteId: String,
        date: String,
    ): Flow<List<OperationLogEntity>>

    @Query(
        "SELECT * FROM operation_log " +
            "WHERE site_id = :siteId " +
            "ORDER BY created_at DESC LIMIT :limit",
    )
    fun observeRecent(
        siteId: String,
        limit: Int = 100,
    ): Flow<List<OperationLogEntity>>

    @Query(
        "SELECT COUNT(*) FROM operation_log " +
            "WHERE type = 'issue' " +
            "AND site_id = :siteId " +
            "AND shift_date = :shiftDate " +
            "AND device_id = :deviceId " +
            "AND employee_id = :employeeId " +
            "AND created_at BETWEEN :fromTs AND :toTs",
    )
    suspend fun countIssueLogsNear(
        siteId: String,
        shiftDate: String,
        deviceId: String,
        employeeId: String,
        fromTs: Long,
        toTs: Long,
    ): Int

    @Insert
    suspend fun insert(log: OperationLogEntity): Long

    @Query(
        "DELETE FROM operation_log WHERE created_at < :before",
    )
    suspend fun deleteOlderThan(before: Long)
}
