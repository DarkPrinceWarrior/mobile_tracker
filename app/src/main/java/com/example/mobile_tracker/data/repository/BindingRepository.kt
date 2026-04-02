package com.example.mobile_tracker.data.repository

import com.example.mobile_tracker.data.local.db.dao.BindingDao
import com.example.mobile_tracker.data.local.db.dao.DeviceDao
import com.example.mobile_tracker.data.local.db.dao.OperationLogDao
import com.example.mobile_tracker.data.local.db.entity.BindingEntity
import com.example.mobile_tracker.data.local.db.entity.OperationLogEntity
import com.example.mobile_tracker.data.remote.api.BindingApi
import com.example.mobile_tracker.data.remote.dto.BindingResponse
import com.example.mobile_tracker.data.remote.dto.CloseBindingRequest
import com.example.mobile_tracker.data.remote.dto.CreateBindingRequest
import com.example.mobile_tracker.data.remote.dto.toDomain
import com.example.mobile_tracker.domain.model.DeviceBinding
import com.example.mobile_tracker.util.OperatorNotificationManager
import io.ktor.client.call.body
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import timber.log.Timber

enum class ReturnDeviceProblemOutcome(
    val deviceStatus: String,
) {
    Lost(deviceStatus = "lost"),
    Faulty(deviceStatus = "faulty"),
    NoConnection(deviceStatus = "inspection"),
    Other(deviceStatus = "inspection"),
}

class BindingRepository(
    private val bindingApi: BindingApi,
    private val bindingDao: BindingDao,
    private val deviceDao: DeviceDao,
    private val operationLogDao: OperationLogDao,
    private val notificationManager: OperatorNotificationManager,
) {

    /**
     * Pull-sync: загружаем привязки за выбранную смену и все активные выдачи
     * на площадке. Второй запрос нужен для кейса, когда выдача создана
     * до полуночи, а возврат оператор делает уже на следующий день.
     */
    suspend fun refreshBindings(
        siteId: String,
        shiftDate: String,
    ): Result<Int> = runCatching {
        val shiftResponse = bindingApi.getBindings(
            siteId = siteId,
            shiftDate = shiftDate,
            pageSize = 100,
        )
        val activeResponse = bindingApi.getBindings(
            siteId = siteId,
            status = "active",
            pageSize = 100,
        )
        val merged = linkedMapOf<String, BindingResponse>()
        (activeResponse.items + shiftResponse.items).forEach { binding ->
            merged[binding.id] = binding
        }
        var count = 0
        for (binding in merged.values) {
            count += upsertBindingFromBackend(binding)
        }
        Timber.d(
            "Refreshed $count bindings from backend for $siteId / $shiftDate " +
                "(active=${activeResponse.items.size}, shift=${shiftResponse.items.size})",
        )
        count
    }

    private fun parseIsoTimestamp(iso: String): Long {
        return try {
            java.time.Instant.parse(iso).toEpochMilli()
        } catch (e: Exception) {
            System.currentTimeMillis()
        }
    }

    private suspend fun upsertBindingFromBackend(
        binding: BindingResponse,
    ): Int {
        val existing = bindingDao.findByServerId(binding.id)
        val boundAt = binding.boundAt?.let { parseIsoTimestamp(it) }
            ?: System.currentTimeMillis()
        val createdAt = binding.createdAt?.let { parseIsoTimestamp(it) }
            ?: boundAt
        val unboundAt = binding.unboundAt?.let { parseIsoTimestamp(it) }
        val employeeName = binding.employeeName ?: existing?.employeeName.orEmpty()

        return if (existing != null) {
            val updated = existing.copy(
                serverId = binding.id,
                deviceId = binding.deviceId,
                employeeId = binding.employeeId,
                employeeName = employeeName,
                siteId = binding.siteId,
                shiftDate = binding.shiftDate,
                shiftType = binding.shiftType,
                boundAt = boundAt,
                unboundAt = unboundAt,
                status = binding.status,
                operatorId = binding.boundBy ?: existing.operatorId,
                isSynced = true,
                createdAt = createdAt,
            )
            if (updated != existing) {
                bindingDao.update(updated)
                1
            } else {
                0
            }
        } else {
            bindingDao.insert(
                BindingEntity(
                    serverId = binding.id,
                    deviceId = binding.deviceId,
                    employeeId = binding.employeeId,
                    employeeName = employeeName,
                    siteId = binding.siteId,
                    shiftDate = binding.shiftDate,
                    shiftType = binding.shiftType,
                    boundAt = boundAt,
                    unboundAt = unboundAt,
                    status = binding.status,
                    operatorId = binding.boundBy,
                    isSynced = true,
                    createdAt = createdAt,
                ),
            )
            1
        }
    }

    fun observeActiveBindings(
        siteId: String,
    ): Flow<List<DeviceBinding>> =
        bindingDao.observeActive(siteId).map { list ->
            list.map { it.toDomain() }
        }

    fun observeShiftBindings(
        siteId: String,
        date: String,
    ): Flow<List<DeviceBinding>> =
        bindingDao.observeByShift(siteId, date).map { list ->
            list.map { it.toDomain() }
        }

    suspend fun issueDevice(
        deviceId: String,
        employeeId: String,
        employeeName: String,
        siteId: String,
        shiftDate: String,
        shiftType: String,
        operatorId: String,
    ): Result<DeviceBinding> = runCatching {
        val existingByDevice =
            bindingDao.findActiveByDevice(deviceId)
        require(existingByDevice == null) {
            "Часы $deviceId уже выданы: " +
                "${existingByDevice?.employeeName}"
        }

        val existingByEmployee =
            bindingDao.findActiveByEmployee(employeeId)
        require(existingByEmployee == null) {
            "Сотрудник уже имеет часы: " +
                "${existingByEmployee?.deviceId}"
        }

        val device = deviceDao.findById(deviceId)
        requireNotNull(device) {
            "Часы $deviceId не найдены"
        }
        require(
            device.localStatus == "available" &&
                device.status == "active",
        ) {
            "Часы недоступны (статус: " +
                "${device.localStatus}, ${device.status})"
        }

        val now = System.currentTimeMillis()
        val entity = BindingEntity(
            deviceId = deviceId,
            employeeId = employeeId,
            employeeName = employeeName,
            siteId = siteId,
            shiftDate = shiftDate,
            shiftType = shiftType,
            boundAt = now,
            status = "active",
            operatorId = operatorId,
            isSynced = false,
            createdAt = now,
        )
        val localId = bindingDao.insert(entity)

        deviceDao.updateLocalStatus(
            deviceId = deviceId,
            status = "issued",
            empId = employeeId,
            empName = employeeName,
        )

        operationLogDao.insert(
            OperationLogEntity(
                type = "issue",
                deviceId = deviceId,
                employeeId = employeeId,
                employeeName = employeeName,
                siteId = siteId,
                shiftDate = shiftDate,
                status = "success",
                createdAt = now,
            ),
        )

        val binding = entity.copy(id = localId)

        try {
            val response = bindingApi.createBinding(
                CreateBindingRequest(
                    deviceId = deviceId,
                    employeeId = employeeId,
                    siteId = siteId,
                    shiftDate = shiftDate,
                    shiftType = shiftType,
                    boundBy = operatorId,
                ),
            )
            val code = response.status.value
            when {
                code in 200..201 -> {
                    val body =
                        response.body<BindingResponse>()
                    bindingDao.update(
                        binding.copy(
                            serverId = body.id,
                            isSynced = true,
                        ),
                    )
                    Timber.d(
                        "Binding synced: server_id=${body.id}",
                    )
                }
                code == 409 -> {
                    bindingDao.update(
                        binding.copy(isSynced = true),
                    )
                    notificationManager.notifyBindingConflict(deviceId)
                    Timber.d(
                        "Binding conflict (409), " +
                            "marked as synced",
                    )
                }
                else -> {
                    Timber.w(
                        "Binding sync failed: HTTP $code",
                    )
                }
            }
        } catch (e: Exception) {
            Timber.w(
                e,
                "Binding sync failed, will retry later",
            )
        }

        binding.toDomain()
    }

    suspend fun returnDevice(
        bindingId: Long,
        siteId: String,
        shiftDate: String,
        operatorId: String? = null,
    ): Result<DeviceBinding> = runCatching {
        val binding = bindingDao.findActiveByIdSync(bindingId)
        requireNotNull(binding) {
            "Привязка #$bindingId не найдена"
        }
        require(binding.status == "active") {
            "Привязка уже закрыта"
        }

        val now = System.currentTimeMillis()
        bindingDao.closeBinding(bindingId, now)

        deviceDao.updateLocalStatus(
            deviceId = binding.deviceId,
            status = "available",
            empId = null,
            empName = null,
        )

        operationLogDao.insert(
            OperationLogEntity(
                type = "return",
                deviceId = binding.deviceId,
                employeeId = binding.employeeId,
                employeeName = binding.employeeName,
                siteId = siteId,
                shiftDate = shiftDate,
                status = "success",
                createdAt = now,
            ),
        )

        if (binding.serverId != null) {
            try {
                val response = bindingApi.closeBinding(
                    binding.serverId,
                    CloseBindingRequest(
                        unboundBy = operatorId,
                    ),
                )
                val code = response.status.value
                if (code in 200..299) {
                    bindingDao.update(
                        binding.copy(
                            status = "closed",
                            unboundAt = now,
                            isSynced = true,
                        ),
                    )
                    Timber.d("Return synced: ${binding.serverId}")
                } else {
                    Timber.w("Return sync failed: HTTP $code")
                }
            } catch (e: Exception) {
                Timber.w(
                    e,
                    "Return sync failed, will retry later",
                )
            }
        }

        binding.copy(
            status = "closed",
            unboundAt = now,
        ).toDomain()
    }

    suspend fun returnDeviceWithProblem(
        bindingId: Long,
        siteId: String,
        shiftDate: String,
        outcome: ReturnDeviceProblemOutcome,
        comment: String?,
        operatorId: String? = null,
    ): Result<DeviceBinding> = runCatching {
        val binding = bindingDao.findActiveByIdSync(bindingId)
        requireNotNull(binding) {
            "Привязка #$bindingId не найдена"
        }
        require(binding.status == "active") {
            "Привязка уже закрыта"
        }

        val now = System.currentTimeMillis()
        bindingDao.closeBinding(bindingId, now)

        deviceDao.updateLocalStatus(
            deviceId = binding.deviceId,
            status = outcome.deviceStatus,
            empId = null,
            empName = null,
        )

        val details = buildString {
            append("Возврат с проблемой: ")
            append(
                when (outcome) {
                    ReturnDeviceProblemOutcome.Lost -> "потеря"
                    ReturnDeviceProblemOutcome.Faulty -> "поломка"
                    ReturnDeviceProblemOutcome.NoConnection -> "нет связи"
                    ReturnDeviceProblemOutcome.Other -> "другое"
                },
            )
            if (!comment.isNullOrBlank()) {
                append(". ")
                append(comment.trim())
            }
        }

        operationLogDao.insert(
            OperationLogEntity(
                type = "status_change",
                deviceId = binding.deviceId,
                employeeId = binding.employeeId,
                employeeName = binding.employeeName,
                siteId = siteId,
                shiftDate = shiftDate,
                status = "success",
                details = details,
                createdAt = now,
            ),
        )

        if (binding.serverId != null) {
            try {
                val response = bindingApi.closeBinding(
                    binding.serverId,
                    CloseBindingRequest(
                        unboundBy = operatorId,
                    ),
                )
                val code = response.status.value
                if (code in 200..299) {
                    bindingDao.update(
                        binding.copy(
                            status = "closed",
                            unboundAt = now,
                            isSynced = true,
                        ),
                    )
                    Timber.d("Problem return synced: ${binding.serverId}")
                } else {
                    Timber.w("Problem return sync failed: HTTP $code")
                }
            } catch (e: Exception) {
                Timber.w(
                    e,
                    "Problem return sync failed, will retry later",
                )
            }
        }

        binding.copy(
            status = "closed",
            unboundAt = now,
        ).toDomain()
    }

    suspend fun syncUnsynced(): Result<Int> = runCatching {
        val unsynced = bindingDao.getUnsynced()
        var count = 0
        for (b in unsynced) {
            try {
                if (b.status == "active" && b.serverId == null) {
                    val response = bindingApi.createBinding(
                        CreateBindingRequest(
                            deviceId = b.deviceId,
                            employeeId = b.employeeId,
                            siteId = b.siteId,
                            shiftDate = b.shiftDate,
                            shiftType = b.shiftType,
                            boundBy = b.operatorId,
                        ),
                    )
                    val code = response.status.value
                    if (code in 200..201) {
                        val body =
                            response.body<BindingResponse>()
                        bindingDao.update(
                            b.copy(
                                serverId = body.id,
                                isSynced = true,
                            ),
                        )
                        count++
                    } else if (code == 409) {
                        bindingDao.update(
                            b.copy(isSynced = true),
                        )
                        notificationManager.notifyBindingConflict(b.deviceId)
                        count++
                    }
                } else if (
                    b.status == "closed" &&
                    b.serverId != null
                ) {
                    val response = bindingApi.closeBinding(
                        b.serverId,
                        CloseBindingRequest(
                            unboundBy = b.operatorId,
                        ),
                    )
                    val code = response.status.value
                    if (code in 200..299) {
                        bindingDao.update(
                            b.copy(isSynced = true),
                        )
                        count++
                    }
                }
            } catch (e: Exception) {
                Timber.w(
                    e,
                    "Sync binding #${b.id} failed",
                )
            }
        }
        Timber.d("Synced $count bindings")
        count
    }
}
