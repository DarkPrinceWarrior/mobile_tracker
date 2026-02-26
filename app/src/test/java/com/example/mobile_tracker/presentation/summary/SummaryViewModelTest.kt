package com.example.mobile_tracker.presentation.summary

import com.example.mobile_tracker.data.local.db.dao.BindingDao
import com.example.mobile_tracker.data.local.db.dao.PacketQueueDao
import com.example.mobile_tracker.data.local.db.dao.ShiftContextDao
import com.example.mobile_tracker.data.local.db.entity.BindingEntity
import com.example.mobile_tracker.data.local.db.entity.ShiftContextEntity
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SummaryViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var bindingDao: BindingDao
    private lateinit var packetQueueDao: PacketQueueDao
    private lateinit var shiftContextDao: ShiftContextDao
    private lateinit var viewModel: SummaryViewModel

    private val shiftContext = ShiftContextEntity(
        siteId = "site_01",
        siteName = "Площадка 1",
        shiftDate = "2026-02-25",
        shiftType = "day",
        operatorId = "op_01",
        operatorName = "Оператор",
    )

    private fun makeBinding(
        id: Long = 1L,
        status: String = "active",
        dataUploaded: Boolean = false,
        isSynced: Boolean = true,
    ) = BindingEntity(
        id = id,
        deviceId = "dev_$id",
        employeeId = "emp_$id",
        employeeName = "Сотрудник $id",
        siteId = "site_01",
        shiftDate = "2026-02-25",
        shiftType = "day",
        boundAt = 1000L,
        status = status,
        dataUploaded = dataUploaded,
        isSynced = isSynced,
        createdAt = 1000L,
    )

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        bindingDao = mockk(relaxed = true)
        packetQueueDao = mockk(relaxed = true)
        shiftContextDao = mockk(relaxed = true)

        coEvery { shiftContextDao.get() } returns shiftContext
        every {
            packetQueueDao.observePendingCount()
        } returns flowOf(0)
        every {
            packetQueueDao.observeErrorCount()
        } returns flowOf(0)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(): SummaryViewModel =
        SummaryViewModel(
            bindingDao = bindingDao,
            packetQueueDao = packetQueueDao,
            shiftContextDao = shiftContextDao,
        )

    @Test
    fun `loads metrics on init`() = runTest {
        val bindings = listOf(
            makeBinding(
                id = 1L,
                status = "active",
                dataUploaded = false,
                isSynced = true,
            ),
            makeBinding(
                id = 2L,
                status = "closed",
                dataUploaded = true,
                isSynced = true,
            ),
            makeBinding(
                id = 3L,
                status = "active",
                dataUploaded = true,
                isSynced = false,
            ),
        )
        every {
            bindingDao.observeByShift(
                "site_01",
                "2026-02-25",
            )
        } returns flowOf(bindings)
        every {
            packetQueueDao.observePendingCount()
        } returns flowOf(5)
        every {
            packetQueueDao.observeErrorCount()
        } returns flowOf(2)

        viewModel = createViewModel()
        advanceUntilIdle()

        val state = viewModel.state.value
        assertEquals(false, state.isLoading)
        assertEquals(3, state.issuedCount)
        assertEquals(1, state.returnedCount)
        assertEquals(2, state.notReturnedCount)
        assertEquals(2, state.dataUploadedCount)
        assertEquals(5, state.pendingPacketsCount)
        assertEquals(2, state.errorPacketsCount)
        assertEquals(1, state.unsyncedBindingsCount)
    }

    @Test
    fun `shows error when context not set`() = runTest {
        coEvery { shiftContextDao.get() } returns null

        viewModel = createViewModel()
        advanceUntilIdle()

        assertNotNull(viewModel.state.value.error)
        assertTrue(
            viewModel.state.value.error!!
                .contains("Контекст"),
        )
    }

    @Test
    fun `loads context info into state`() = runTest {
        every {
            bindingDao.observeByShift(
                "site_01",
                "2026-02-25",
            )
        } returns flowOf(emptyList())

        viewModel = createViewModel()
        advanceUntilIdle()

        val state = viewModel.state.value
        assertEquals("Площадка 1", state.siteName)
        assertEquals("2026-02-25", state.shiftDate)
        assertEquals("day", state.shiftType)
    }

    @Test
    fun `handles zero bindings`() = runTest {
        every {
            bindingDao.observeByShift(
                "site_01",
                "2026-02-25",
            )
        } returns flowOf(emptyList())

        viewModel = createViewModel()
        advanceUntilIdle()

        val state = viewModel.state.value
        assertEquals(0, state.issuedCount)
        assertEquals(0, state.returnedCount)
        assertEquals(0, state.notReturnedCount)
        assertEquals(0, state.dataUploadedCount)
    }
}
