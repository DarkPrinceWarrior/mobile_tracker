package com.example.mobile_tracker.presentation.journal

import com.example.mobile_tracker.data.local.db.dao.OperationLogDao
import com.example.mobile_tracker.data.local.db.dao.ShiftContextDao
import com.example.mobile_tracker.data.local.db.entity.OperationLogEntity
import com.example.mobile_tracker.data.local.db.entity.ShiftContextEntity
import io.mockk.coEvery
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
class JournalViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var operationLogDao: OperationLogDao
    private lateinit var shiftContextDao: ShiftContextDao
    private lateinit var viewModel: JournalViewModel

    private val shiftContext = ShiftContextEntity(
        siteId = "site_01",
        siteName = "Площадка 1",
        shiftDate = "2026-02-25",
        shiftType = "day",
        operatorId = "op_01",
        operatorName = "Оператор",
    )

    private fun makeLog(
        id: Long = 1L,
        type: String = "issue",
        status: String = "success",
        employeeName: String? = "Иванов И.И.",
        deviceId: String? = "dev_001",
    ) = OperationLogEntity(
        id = id,
        type = type,
        deviceId = deviceId,
        employeeName = employeeName,
        siteId = "site_01",
        shiftDate = "2026-02-25",
        status = status,
        createdAt = System.currentTimeMillis(),
    )

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        operationLogDao = mockk(relaxed = true)
        shiftContextDao = mockk(relaxed = true)
        coEvery { shiftContextDao.get() } returns shiftContext
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(): JournalViewModel =
        JournalViewModel(
            operationLogDao = operationLogDao,
            shiftContextDao = shiftContextDao,
        )

    @Test
    fun `loads logs on init`() = runTest {
        val logs = listOf(
            makeLog(id = 1L, type = "issue"),
            makeLog(id = 2L, type = "return"),
        )
        coEvery {
            operationLogDao.observeByShift(
                "site_01",
                "2026-02-25",
            )
        } returns flowOf(logs)

        viewModel = createViewModel()
        advanceUntilIdle()

        val state = viewModel.state.value
        assertEquals(2, state.logs.size)
        assertEquals(2, state.filteredLogs.size)
        assertEquals(false, state.isLoading)
        assertTrue(state.availableTypes.contains("issue"))
        assertTrue(state.availableTypes.contains("return"))
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
    fun `filters by type`() = runTest {
        val logs = listOf(
            makeLog(id = 1L, type = "issue"),
            makeLog(id = 2L, type = "return"),
            makeLog(id = 3L, type = "issue"),
        )
        coEvery {
            operationLogDao.observeByShift(
                "site_01",
                "2026-02-25",
            )
        } returns flowOf(logs)

        viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onIntent(
            JournalIntent.SetTypeFilter("issue"),
        )

        assertEquals(2, viewModel.state.value.filteredLogs.size)
        assertTrue(
            viewModel.state.value.filteredLogs.all {
                it.type == "issue"
            },
        )
    }

    @Test
    fun `filters by status`() = runTest {
        val logs = listOf(
            makeLog(id = 1L, status = "success"),
            makeLog(id = 2L, status = "error"),
            makeLog(id = 3L, status = "success"),
        )
        coEvery {
            operationLogDao.observeByShift(
                "site_01",
                "2026-02-25",
            )
        } returns flowOf(logs)

        viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onIntent(
            JournalIntent.SetStatusFilter("error"),
        )

        assertEquals(1, viewModel.state.value.filteredLogs.size)
        assertEquals(
            "error",
            viewModel.state.value.filteredLogs[0].status,
        )
    }

    @Test
    fun `searches by employee name`() = runTest {
        val logs = listOf(
            makeLog(
                id = 1L,
                employeeName = "Иванов И.И.",
            ),
            makeLog(
                id = 2L,
                employeeName = "Петров П.П.",
            ),
        )
        coEvery {
            operationLogDao.observeByShift(
                "site_01",
                "2026-02-25",
            )
        } returns flowOf(logs)

        viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onIntent(
            JournalIntent.SetSearchQuery("Петров"),
        )

        assertEquals(1, viewModel.state.value.filteredLogs.size)
        assertEquals(
            "Петров П.П.",
            viewModel.state.value.filteredLogs[0].employeeName,
        )
    }

    @Test
    fun `searches by device id`() = runTest {
        val logs = listOf(
            makeLog(id = 1L, deviceId = "dev_001"),
            makeLog(id = 2L, deviceId = "dev_002"),
        )
        coEvery {
            operationLogDao.observeByShift(
                "site_01",
                "2026-02-25",
            )
        } returns flowOf(logs)

        viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onIntent(
            JournalIntent.SetSearchQuery("dev_002"),
        )

        assertEquals(1, viewModel.state.value.filteredLogs.size)
        assertEquals(
            "dev_002",
            viewModel.state.value.filteredLogs[0].deviceId,
        )
    }

    @Test
    fun `clear type filter shows all`() = runTest {
        val logs = listOf(
            makeLog(id = 1L, type = "issue"),
            makeLog(id = 2L, type = "return"),
        )
        coEvery {
            operationLogDao.observeByShift(
                "site_01",
                "2026-02-25",
            )
        } returns flowOf(logs)

        viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onIntent(
            JournalIntent.SetTypeFilter("issue"),
        )
        assertEquals(1, viewModel.state.value.filteredLogs.size)

        viewModel.onIntent(
            JournalIntent.SetTypeFilter(null),
        )
        assertEquals(2, viewModel.state.value.filteredLogs.size)
    }
}
