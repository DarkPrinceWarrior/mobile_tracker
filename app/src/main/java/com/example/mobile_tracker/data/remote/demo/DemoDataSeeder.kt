package com.example.mobile_tracker.data.remote.demo

import com.example.mobile_tracker.data.local.db.dao.BindingDao
import com.example.mobile_tracker.data.local.db.entity.BindingEntity
import timber.log.Timber

object DemoDataSeeder {

    suspend fun seedBindings(
        bindingDao: BindingDao,
        siteId: String,
        shiftDate: String,
    ) {
        val active = bindingDao.countActive(siteId)
        if (active > 0) {
            Timber.d("Demo bindings already exist")
            return
        }

        val now = System.currentTimeMillis()

        val bindings = listOf(
            BindingEntity(
                deviceId = "dev-watch-04",
                employeeId = "emp-1",
                employeeName = "Иванов Иван Иванович",
                siteId = siteId,
                shiftDate = shiftDate,
                shiftType = "day",
                boundAt = now - 3_600_000,
                status = "active",
                isSynced = true,
                serverId = 1L,
                createdAt = now - 3_600_000,
            ),
            BindingEntity(
                deviceId = "dev-watch-05",
                employeeId = "emp-2",
                employeeName = "Петров Пётр Сергеевич",
                siteId = siteId,
                shiftDate = shiftDate,
                shiftType = "day",
                boundAt = now - 3_000_000,
                status = "active",
                isSynced = true,
                serverId = 2L,
                createdAt = now - 3_000_000,
            ),
        )

        for (b in bindings) {
            bindingDao.insert(b)
        }

        Timber.d("Seeded ${bindings.size} demo bindings")
    }
}
