package com.example.mobile_tracker.data.repository

import com.example.mobile_tracker.data.local.db.dao.BindingDao
import com.example.mobile_tracker.data.local.db.dao.DeviceDao
import com.example.mobile_tracker.data.local.db.dao.OperationLogDao
import com.example.mobile_tracker.data.local.db.entity.BindingEntity
import com.example.mobile_tracker.data.local.db.entity.DeviceEntity
import com.example.mobile_tracker.data.local.db.entity.OperationLogEntity
import com.example.mobile_tracker.data.remote.api.BindingApi
import com.example.mobile_tracker.data.remote.dto.BindingResponse
import com.example.mobile_tracker.data.remote.dto.CreateBindingRequest
import io.ktor.client.statement.HttpResponse
import io.ktor.http.HttpStatusCode
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class BindingRepositoryTest {

    private lateinit var bindingApi: BindingApi
    private lateinit var bindingDao: BindingDao
    private lateinit var deviceDao: DeviceDao
    private lateinit var operationLogDao: OperationLogDao
    private lateinit var repository: BindingRepository

    @Before
    fun setup() {
        bindingApi = mockk(relaxed = true)
        bindingDao = mockk(relaxed = true)
        deviceDao = mockk(relaxed = true)
        operationLogDao = mockk(relaxed = true)
        repository = BindingRepository(
            bindingApi = bindingApi,
            bindingDao = bindingDao,
            deviceDao = deviceDao,
            operationLogDao = operationLogDao,
        )
    }

    private fun makeDevice(
        deviceId: String = "dev_001",
        localStatus: String = "available",
        status: String = "active",
    ) = DeviceEntity(
        deviceId = deviceId,
        localStatus = localStatus,
        status = status,
        syncedAt = 0L,
    )

    private fun makeBinding(
        id: Long = 1L,
        deviceId: String = "dev_001",
        employeeId: String = "emp_001",
        status: String = "active",
        isSynced: Boolean = false,
        serverId: Long? = null,
    ) = BindingEntity(
        id = id,
        serverId = serverId,
        deviceId = deviceId,
        employeeId = employeeId,
        employeeName = "Иванов И.И.",
        siteId = "site_01",
        shiftDate = "2026-02-25",
        shiftType = "day",
        boundAt = 1000L,
        status = status,
        isSynced = isSynced,
        createdAt = 1000L,
    )

    // --- issueDevice ---

    @Test
    fun `issueDevice success when device available and no active binding`() =
        runTest {
            coEvery {
                bindingDao.findActiveByDevice("dev_001")
            } returns null
            coEvery {
                bindingDao.findActiveByEmployee("emp_001")
            } returns null
            coEvery {
                deviceDao.findById("dev_001")
            } returns makeDevice()
            coEvery {
                bindingDao.insert(any())
            } returns 1L

            val httpResponse = mockk<HttpResponse>()
            every {
                httpResponse.status
            } returns HttpStatusCode.Created
            coEvery {
                bindingApi.createBinding(any())
            } returns httpResponse
            coEvery {
                httpResponse.call
            } returns mockk(relaxed = true)

            val result = repository.issueDevice(
                deviceId = "dev_001",
                employeeId = "emp_001",
                employeeName = "Иванов И.И.",
                siteId = "site_01",
                shiftDate = "2026-02-25",
                shiftType = "day",
                operatorId = "op_01",
            )

            assertTrue(result.isSuccess)
            val binding = result.getOrNull()
            assertNotNull(binding)
            assertEquals("dev_001", binding!!.deviceId)
            assertEquals("emp_001", binding.employeeId)

            coVerify {
                bindingDao.insert(any())
            }
            coVerify {
                deviceDao.updateLocalStatus(
                    "dev_001",
                    "issued",
                    "emp_001",
                    "Иванов И.И.",
                )
            }
            coVerify {
                operationLogDao.insert(
                    match { it.type == "issue" },
                )
            }
        }

    @Test
    fun `issueDevice fails when device already issued`() =
        runTest {
            coEvery {
                bindingDao.findActiveByDevice("dev_001")
            } returns makeBinding()

            val result = repository.issueDevice(
                deviceId = "dev_001",
                employeeId = "emp_002",
                employeeName = "Петров П.П.",
                siteId = "site_01",
                shiftDate = "2026-02-25",
                shiftType = "day",
                operatorId = "op_01",
            )

            assertTrue(result.isFailure)
            assertTrue(
                result.exceptionOrNull()?.message
                    ?.contains("уже выданы") == true,
            )
            coVerify(exactly = 0) {
                bindingDao.insert(any())
            }
        }

    @Test
    fun `issueDevice fails when employee already has active binding`() =
        runTest {
            coEvery {
                bindingDao.findActiveByDevice("dev_002")
            } returns null
            coEvery {
                bindingDao.findActiveByEmployee("emp_001")
            } returns makeBinding(deviceId = "dev_001")

            val result = repository.issueDevice(
                deviceId = "dev_002",
                employeeId = "emp_001",
                employeeName = "Иванов И.И.",
                siteId = "site_01",
                shiftDate = "2026-02-25",
                shiftType = "day",
                operatorId = "op_01",
            )

            assertTrue(result.isFailure)
            assertTrue(
                result.exceptionOrNull()?.message
                    ?.contains("уже имеет часы") == true,
            )
        }

    @Test
    fun `issueDevice fails when device not available`() =
        runTest {
            coEvery {
                bindingDao.findActiveByDevice("dev_001")
            } returns null
            coEvery {
                bindingDao.findActiveByEmployee("emp_001")
            } returns null
            coEvery {
                deviceDao.findById("dev_001")
            } returns makeDevice(localStatus = "issued")

            val result = repository.issueDevice(
                deviceId = "dev_001",
                employeeId = "emp_001",
                employeeName = "Иванов И.И.",
                siteId = "site_01",
                shiftDate = "2026-02-25",
                shiftType = "day",
                operatorId = "op_01",
            )

            assertTrue(result.isFailure)
            assertTrue(
                result.exceptionOrNull()?.message
                    ?.contains("недоступны") == true,
            )
        }

    @Test
    fun `issueDevice saves locally when network fails (offline)`() =
        runTest {
            coEvery {
                bindingDao.findActiveByDevice("dev_001")
            } returns null
            coEvery {
                bindingDao.findActiveByEmployee("emp_001")
            } returns null
            coEvery {
                deviceDao.findById("dev_001")
            } returns makeDevice()
            coEvery {
                bindingDao.insert(any())
            } returns 1L
            coEvery {
                bindingApi.createBinding(any())
            } throws java.io.IOException("No network")

            val result = repository.issueDevice(
                deviceId = "dev_001",
                employeeId = "emp_001",
                employeeName = "Иванов И.И.",
                siteId = "site_01",
                shiftDate = "2026-02-25",
                shiftType = "day",
                operatorId = "op_01",
            )

            assertTrue(result.isSuccess)
            coVerify {
                bindingDao.insert(any())
            }
            coVerify {
                deviceDao.updateLocalStatus(
                    "dev_001",
                    "issued",
                    "emp_001",
                    "Иванов И.И.",
                )
            }
        }

    // --- returnDevice ---

    @Test
    fun `returnDevice success closes binding and frees device`() =
        runTest {
            val binding = makeBinding(
                id = 1L,
                serverId = 100L,
                status = "active",
            )
            coEvery {
                bindingDao.findActiveByIdSync(1L)
            } returns binding

            val httpResponse = mockk<HttpResponse>()
            every {
                httpResponse.status
            } returns HttpStatusCode.OK
            coEvery {
                bindingApi.closeBinding(100L, any())
            } returns httpResponse

            val result = repository.returnDevice(
                bindingId = 1L,
                siteId = "site_01",
                shiftDate = "2026-02-25",
            )

            assertTrue(result.isSuccess)
            assertEquals(
                "closed",
                result.getOrNull()?.status,
            )
            coVerify {
                bindingDao.closeBinding(1L, any())
            }
            coVerify {
                deviceDao.updateLocalStatus(
                    "dev_001",
                    "available",
                    null,
                    null,
                )
            }
            coVerify {
                operationLogDao.insert(
                    match { it.type == "return" },
                )
            }
        }

    @Test
    fun `returnDevice fails when binding not found`() =
        runTest {
            coEvery {
                bindingDao.findActiveByIdSync(99L)
            } returns null

            val result = repository.returnDevice(
                bindingId = 99L,
                siteId = "site_01",
                shiftDate = "2026-02-25",
            )

            assertTrue(result.isFailure)
            assertTrue(
                result.exceptionOrNull()?.message
                    ?.contains("не найдена") == true,
            )
        }

    @Test
    fun `returnDevice fails when binding already closed`() =
        runTest {
            coEvery {
                bindingDao.findActiveByIdSync(1L)
            } returns makeBinding(status = "closed")

            val result = repository.returnDevice(
                bindingId = 1L,
                siteId = "site_01",
                shiftDate = "2026-02-25",
            )

            assertTrue(result.isFailure)
            assertTrue(
                result.exceptionOrNull()?.message
                    ?.contains("уже закрыта") == true,
            )
        }

    // --- syncUnsynced ---

    @Test
    fun `syncUnsynced syncs active unsynced bindings`() =
        runTest {
            val unsynced = makeBinding(
                id = 1L,
                status = "active",
                isSynced = false,
                serverId = null,
            )
            coEvery {
                bindingDao.getUnsynced()
            } returns listOf(unsynced)

            val httpResponse = mockk<HttpResponse>()
            every {
                httpResponse.status
            } returns HttpStatusCode.Created
            coEvery {
                bindingApi.createBinding(any())
            } returns httpResponse
            val call = mockk<io.ktor.client.call.HttpClientCall>(
                relaxed = true,
            )
            coEvery {
                call.bodyNullable(any())
            } returns BindingResponse(
                id = 100L,
                deviceId = unsynced.deviceId,
                employeeId = unsynced.employeeId,
                siteId = unsynced.siteId,
                shiftDate = unsynced.shiftDate,
            )
            every { httpResponse.call } returns call

            val result = repository.syncUnsynced()

            assertTrue(result.isSuccess)
            assertEquals(1, result.getOrNull())
            coVerify {
                bindingDao.update(
                    match { it.isSynced },
                )
            }
        }

    @Test
    fun `syncUnsynced handles 409 conflict as success`() =
        runTest {
            val unsynced = makeBinding(
                status = "active",
                isSynced = false,
                serverId = null,
            )
            coEvery {
                bindingDao.getUnsynced()
            } returns listOf(unsynced)

            val httpResponse = mockk<HttpResponse>()
            every {
                httpResponse.status
            } returns HttpStatusCode.Conflict
            coEvery {
                bindingApi.createBinding(any())
            } returns httpResponse

            val result = repository.syncUnsynced()

            assertTrue(result.isSuccess)
            assertEquals(1, result.getOrNull())
            coVerify {
                bindingDao.update(
                    match { it.isSynced },
                )
            }
        }

    // --- observeActiveBindings ---

    @Test
    fun `observeActiveBindings returns domain models`() =
        runTest {
            val entity = makeBinding()
            coEvery {
                bindingDao.observeActive("site_01")
            } returns flowOf(listOf(entity))

            val flow = repository.observeActiveBindings(
                "site_01",
            )
            flow.collect { bindings ->
                assertEquals(1, bindings.size)
                assertEquals(
                    "dev_001",
                    bindings[0].deviceId,
                )
                assertEquals(
                    "Иванов И.И.",
                    bindings[0].employeeName,
                )
            }
        }
}
