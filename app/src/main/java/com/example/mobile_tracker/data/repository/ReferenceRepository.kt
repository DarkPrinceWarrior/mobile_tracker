package com.example.mobile_tracker.data.repository

import com.example.mobile_tracker.data.local.db.dao.DeviceDao
import com.example.mobile_tracker.data.local.db.dao.DowntimeReasonDao
import com.example.mobile_tracker.data.local.db.dao.EmployeeDao
import com.example.mobile_tracker.data.local.db.dao.SiteDao
import com.example.mobile_tracker.data.remote.api.ReferenceApi
import com.example.mobile_tracker.data.remote.dto.toEntity
import timber.log.Timber

class ReferenceRepository(
    private val referenceApi: ReferenceApi,
    private val employeeDao: EmployeeDao,
    private val deviceDao: DeviceDao,
    private val siteDao: SiteDao,
    private val downtimeReasonDao: DowntimeReasonDao,
) {
    suspend fun syncEmployees(siteId: String): Result<Int> =
        runCatching {
            val now = System.currentTimeMillis()
            var page = 1
            var total = 0
            do {
                val response = referenceApi.getEmployees(
                    siteId = siteId,
                    page = page,
                )
                val entities = response.data.map {
                    it.toEntity(now)
                }
                employeeDao.upsertAll(entities)
                total += entities.size
                page++
            } while (page <= response.totalPages)

            val staleBefore =
                now - STALE_THRESHOLD_MS
            employeeDao.deleteStale(staleBefore)

            Timber.d(
                "Synced $total employees for site $siteId",
            )
            total
        }

    suspend fun syncDevices(siteId: String): Result<Int> =
        runCatching {
            val now = System.currentTimeMillis()
            var page = 1
            var total = 0
            do {
                val response = referenceApi.getDevices(
                    siteId = siteId,
                    page = page,
                )
                val entities = response.data.map {
                    it.toEntity(now)
                }
                deviceDao.upsertAll(entities)
                total += entities.size
                page++
            } while (page <= response.totalPages)

            Timber.d(
                "Synced $total devices for site $siteId",
            )
            total
        }

    suspend fun syncSites(): Result<Int> = runCatching {
        val now = System.currentTimeMillis()
        val sites = referenceApi.getSites()
        val entities = sites.map { it.toEntity(now) }
        siteDao.upsertAll(entities)

        Timber.d("Synced ${entities.size} sites")
        entities.size
    }

    suspend fun syncDowntimeReasons(): Result<Int> =
        runCatching {
            val now = System.currentTimeMillis()
            val reasons = referenceApi.getDowntimeReasons()
            val entities = reasons.map {
                it.toEntity(now)
            }
            downtimeReasonDao.upsertAll(entities)

            val staleBefore =
                now - STALE_THRESHOLD_MS
            downtimeReasonDao.deleteStale(staleBefore)

            Timber.d(
                "Synced ${entities.size} downtime reasons",
            )
            entities.size
        }

    suspend fun syncAll(siteId: String): Result<Unit> =
        runCatching {
            syncSites().getOrThrow()
            syncEmployees(siteId).getOrThrow()
            syncDevices(siteId).getOrThrow()
            syncDowntimeReasons().getOrThrow()
            Timber.d("Full reference sync complete")
        }

    companion object {
        private const val STALE_THRESHOLD_MS =
            7L * 24 * 60 * 60 * 1000
    }
}
