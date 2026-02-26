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
import io.ktor.client.call.body
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import timber.log.Timber

class BindingRepository(
    private val bindingApi: BindingApi,
    private val bindingDao: BindingDao,
    private val deviceDao: DeviceDao,
    private val operationLogDao: OperationLogDao,
) {

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
                    CloseBindingRequest(),
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
                        count++
                    }
                } else if (
                    b.status == "closed" &&
                    b.serverId != null
                ) {
                    val response = bindingApi.closeBinding(
                        b.serverId,
                        CloseBindingRequest(),
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
