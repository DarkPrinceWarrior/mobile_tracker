package com.example.mobile_tracker.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.mobile_tracker.data.local.db.dao.BindingDao
import com.example.mobile_tracker.data.local.db.dao.DeviceDao
import com.example.mobile_tracker.data.local.db.dao.DowntimeReasonDao
import com.example.mobile_tracker.data.local.db.dao.EmployeeDao
import com.example.mobile_tracker.data.local.db.dao.OperationLogDao
import com.example.mobile_tracker.data.local.db.dao.PacketQueueDao
import com.example.mobile_tracker.data.local.db.dao.ShiftContextDao
import com.example.mobile_tracker.data.local.db.dao.SiteDao
import com.example.mobile_tracker.data.local.db.entity.BindingEntity
import com.example.mobile_tracker.data.local.db.entity.DeviceEntity
import com.example.mobile_tracker.data.local.db.entity.DowntimeReasonEntity
import com.example.mobile_tracker.data.local.db.entity.EmployeeEntity
import com.example.mobile_tracker.data.local.db.entity.OperationLogEntity
import com.example.mobile_tracker.data.local.db.entity.PacketQueueEntity
import com.example.mobile_tracker.data.local.db.entity.ShiftContextEntity
import com.example.mobile_tracker.data.local.db.entity.SiteEntity

@Database(
    entities = [
        EmployeeEntity::class,
        DeviceEntity::class,
        BindingEntity::class,
        PacketQueueEntity::class,
        OperationLogEntity::class,
        SiteEntity::class,
        ShiftContextEntity::class,
        DowntimeReasonEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun employeeDao(): EmployeeDao
    abstract fun deviceDao(): DeviceDao
    abstract fun bindingDao(): BindingDao
    abstract fun packetQueueDao(): PacketQueueDao
    abstract fun operationLogDao(): OperationLogDao
    abstract fun siteDao(): SiteDao
    abstract fun shiftContextDao(): ShiftContextDao
    abstract fun downtimeReasonDao(): DowntimeReasonDao
}
