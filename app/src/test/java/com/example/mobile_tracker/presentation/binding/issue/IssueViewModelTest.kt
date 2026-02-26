package com.example.mobile_tracker.presentation.binding.issue

import com.example.mobile_tracker.data.local.db.dao.DeviceDao
import com.example.mobile_tracker.data.local.db.dao.EmployeeDao
import com.example.mobile_tracker.data.local.db.dao.ShiftContextDao
import com.example.mobile_tracker.data.local.db.entity.DeviceEntity
import com.example.mobile_tracker.data.local.db.entity.EmployeeEntity
import com.example.mobile_tracker.data.local.db.entity.ShiftContextEntity
import com.example.mobile_tracker.data.repository.BindingRepository
import com.example.mobile_tracker.domain.model.DeviceBinding
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class IssueViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var employeeDao: EmployeeDao
    private lateinit var deviceDao: DeviceDao
    private lateinit var shiftContextDao: ShiftContextDao
    private lateinit var bindingRepository: BindingRepository
    private lateinit var viewModel: IssueViewModel

    private val shiftContext = ShiftContextEntity(
        siteId = "site_01",
        siteName = "Площадка 1",
        shiftDate = "2026-02-25",
        shiftType = "day",
        operatorId = "op_01",
        operatorName = "Оператор",
    )

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        employeeDao = mockk(relaxed = true)
        deviceDao = mockk(relaxed = true)
        shiftContextDao = mockk(relaxed = true)
        bindingRepository = mockk(relaxed = true)

        coEvery { shiftContextDao.get() } returns shiftContext
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(): IssueViewModel =
        IssueViewModel(
            employeeDao = employeeDao,
            deviceDao = deviceDao,
            shiftContextDao = shiftContextDao,
            bindingRepository = bindingRepository,
        )

    private fun makeEmployee(
        id: String = "emp_001",
        fullName: String = "Иванов И.И.",
        personnelNumber: String? = "12345",
    ) = EmployeeEntity(
        id = id,
        fullName = fullName,
        personnelNumber = personnelNumber,
        status = "active",
        syncedAt = 0L,
    )

    private fun makeDevice(
        deviceId: String = "dev_001",
    ) = DeviceEntity(
        deviceId = deviceId,
        localStatus = "available",
        status = "active",
        syncedAt = 0L,
    )

    @Test
    fun `initial state is IDENTIFY_EMPLOYEE step`() =
        runTest {
            viewModel = createViewModel()
            advanceUntilIdle()
            assertEquals(
                IssueStep.IDENTIFY_EMPLOYEE,
                viewModel.state.value.step,
            )
        }

    @Test
    fun `searchByPersonnel finds employee`() = runTest {
        val employee = makeEmployee()
        coEvery {
            employeeDao.findByPersonnelNumber("12345")
        } returns employee

        viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onIntent(
            IssueIntent.UpdatePersonnelQuery("12345"),
        )
        viewModel.onIntent(IssueIntent.SearchByPersonnel)
        advanceUntilIdle()

        val state = viewModel.state.value
        assertEquals(1, state.searchResults.size)
        assertEquals(
            "Иванов И.И.",
            state.searchResults[0].fullName,
        )
        assertNull(state.error)
    }

    @Test
    fun `searchByPersonnel shows error when not found`() =
        runTest {
            coEvery {
                employeeDao.findByPersonnelNumber("99999")
            } returns null

            viewModel = createViewModel()
            advanceUntilIdle()

            viewModel.onIntent(
                IssueIntent.UpdatePersonnelQuery("99999"),
            )
            viewModel.onIntent(
                IssueIntent.SearchByPersonnel,
            )
            advanceUntilIdle()

            val state = viewModel.state.value
            assertTrue(state.searchResults.isEmpty())
            assertNotNull(state.error)
            assertTrue(state.error!!.contains("не найден"))
        }

    @Test
    fun `selectEmployee moves to SELECT_DEVICE step`() =
        runTest {
            val employee = makeEmployee()
            coEvery {
                employeeDao.findByPersonnelNumber("12345")
            } returns employee
            coEvery {
                deviceDao.getAvailable("site_01")
            } returns listOf(makeDevice())

            viewModel = createViewModel()
            advanceUntilIdle()

            viewModel.onIntent(
                IssueIntent.UpdatePersonnelQuery("12345"),
            )
            viewModel.onIntent(IssueIntent.SearchByPersonnel)
            advanceUntilIdle()

            viewModel.onIntent(
                IssueIntent.SelectEmployee(
                    viewModel.state.value.searchResults[0],
                ),
            )
            advanceUntilIdle()

            val state = viewModel.state.value
            assertEquals(
                IssueStep.SELECT_DEVICE,
                state.step,
            )
            assertNotNull(state.selectedEmployee)
            assertEquals(1, state.availableDevices.size)
        }

    @Test
    fun `selectEmployee shows error when no devices available`() =
        runTest {
            val employee = makeEmployee()
            coEvery {
                employeeDao.findByPersonnelNumber("12345")
            } returns employee
            coEvery {
                deviceDao.getAvailable("site_01")
            } returns emptyList()

            viewModel = createViewModel()
            advanceUntilIdle()

            viewModel.onIntent(
                IssueIntent.UpdatePersonnelQuery("12345"),
            )
            viewModel.onIntent(IssueIntent.SearchByPersonnel)
            advanceUntilIdle()

            viewModel.onIntent(
                IssueIntent.SelectEmployee(
                    viewModel.state.value.searchResults[0],
                ),
            )
            advanceUntilIdle()

            val state = viewModel.state.value
            assertNotNull(state.error)
            assertTrue(
                state.error!!.contains("свободных часов"),
            )
        }

    @Test
    fun `confirmIssue calls repository and resets state`() =
        runTest {
            val employee = makeEmployee()
            val device = makeDevice()
            coEvery {
                employeeDao.findByPersonnelNumber("12345")
            } returns employee
            coEvery {
                deviceDao.getAvailable("site_01")
            } returns listOf(device)
            coEvery {
                bindingRepository.issueDevice(
                    any(), any(), any(),
                    any(), any(), any(), any(),
                )
            } returns Result.success(
                DeviceBinding(
                    id = 1L,
                    deviceId = "dev_001",
                    employeeId = "emp_001",
                    employeeName = "Иванов И.И.",
                    siteId = "site_01",
                    shiftDate = "2026-02-25",
                    boundAt = 1000L,
                    createdAt = 1000L,
                ),
            )

            viewModel = createViewModel()
            advanceUntilIdle()

            viewModel.onIntent(
                IssueIntent.UpdatePersonnelQuery("12345"),
            )
            viewModel.onIntent(IssueIntent.SearchByPersonnel)
            advanceUntilIdle()

            viewModel.onIntent(
                IssueIntent.SelectEmployee(
                    viewModel.state.value.searchResults[0],
                ),
            )
            advanceUntilIdle()

            viewModel.onIntent(IssueIntent.AutoAssignDevice)
            viewModel.onIntent(IssueIntent.ConfirmIssue)
            advanceUntilIdle()

            coVerify {
                bindingRepository.issueDevice(
                    deviceId = "dev_001",
                    employeeId = "emp_001",
                    employeeName = "Иванов И.И.",
                    siteId = "site_01",
                    shiftDate = "2026-02-25",
                    shiftType = "day",
                    operatorId = "op_01",
                )
            }

            assertEquals(
                IssueStep.IDENTIFY_EMPLOYEE,
                viewModel.state.value.step,
            )
        }

    @Test
    fun `goBack navigates between steps`() = runTest {
        val employee = makeEmployee()
        coEvery {
            employeeDao.findByPersonnelNumber("12345")
        } returns employee
        coEvery {
            deviceDao.getAvailable("site_01")
        } returns listOf(makeDevice())

        viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onIntent(
            IssueIntent.UpdatePersonnelQuery("12345"),
        )
        viewModel.onIntent(IssueIntent.SearchByPersonnel)
        advanceUntilIdle()

        viewModel.onIntent(
            IssueIntent.SelectEmployee(
                viewModel.state.value.searchResults[0],
            ),
        )
        advanceUntilIdle()
        assertEquals(
            IssueStep.SELECT_DEVICE,
            viewModel.state.value.step,
        )

        viewModel.onIntent(IssueIntent.GoBack)
        assertEquals(
            IssueStep.IDENTIFY_EMPLOYEE,
            viewModel.state.value.step,
        )
    }
}
