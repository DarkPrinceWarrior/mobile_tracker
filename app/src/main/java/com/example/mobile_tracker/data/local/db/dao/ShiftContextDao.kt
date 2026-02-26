package com.example.mobile_tracker.data.local.db.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.example.mobile_tracker.data.local.db.entity.ShiftContextEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ShiftContextDao {

    @Query("SELECT * FROM shift_context WHERE id = 1 LIMIT 1")
    fun observe(): Flow<ShiftContextEntity?>

    @Query("SELECT * FROM shift_context WHERE id = 1 LIMIT 1")
    suspend fun get(): ShiftContextEntity?

    @Upsert
    suspend fun save(context: ShiftContextEntity)

    @Query("DELETE FROM shift_context")
    suspend fun clear()
}
