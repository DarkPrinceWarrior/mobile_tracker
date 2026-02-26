package com.example.mobile_tracker.presentation.binding.return_device

import com.example.mobile_tracker.data.local.db.dao.BindingDao
import com.example.mobile_tracker.data.local.db.dao.DeviceDao
import com.example.mobile_tracker.data.local.db.dao.ShiftContextDao
import com.example.mobile_tracker.data.local.db.entity.ShiftContextEntity
import com.example.mobile_tracker.data.repository.BindingRepository
import com.example.mobile_tracker.domain.model.DeviceBinding
import io.mockk.coEvery
import io.mockk.coVerify
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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ReturnViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var bindingDao: BindingDao
    private lateinit var deviceDao: DeviceDao
    private lateinit var shiftContextDao: ShiftContextDao
    private lateinit var bindingRepository: BindingRepository
    private lateinit var viewModel: ReturnViewModel

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
        deviceId: String = "dev_001",
        employeeName: String = "Иванов И.И.",
        dataUploaded: Boolean = false,
        isSynced: Boolean = true,
    ) = DeviceBinding(
        id = id,
        deviceId = deviceId,
        employeeId = "emp_001",
        employeeName = employeeName,
        siteId = "site_01",
        shiftDate = "2026-02-25",
        boundAt = 1000L,
        status = "active",
        dataUploaded = dataUploaded,
        isSynced = isSynced,
        createdAt = 1000L,
    )

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        bindingDao = mockk(relaxed = true)
        deviceDao = mockk(relaxed = true)
        shiftContextDao = mockk(relaxed = true)
        bindingRepository = mockk(relaxed = true)

        coEvery { shiftContextDao.get() } returns shiftContext
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(): ReturnViewModel =
        ReturnViewModel(
            bindingDao = bindingDao,
            deviceDao = deviceDao,
            shiftContextDao = shiftContextDao,
            bindingRepository = bindingRepository,
        )

    @Test
    fun `loads active bindings on init`() = runTest {
        val bindings = listOf(
            makeBinding(id = 1L, deviceId = "dev_001"),
            makeBinding(id = 2L, deviceId = "dev_002"),
        )
        coEvery {
            bindingRepository.observeActiveBindings("site_01")
        } returns flowOf(bindings)

        viewModel = createViewModel()
        advanceUntilIdle()

        val state = viewModel.state.value
        assertEquals(2, state.activeBindings.size)
        assertFalse(state.isLoading)
    }

    @Test
    fun `shows error when shift context not set`() =
        runTest {
            coEvery {
                shiftContextDao.get()
            } returns null

            viewModel = createViewModel()
            advanceUntilIdle()

            assertNotNull(viewModel.state.value.error)
            assertTrue(
                viewModel.state.value.error!!
                    .contains("Контекст"),
            )
        }

    @Test
    fun `selectBinding updates selected binding`() =
        runTest {
            val binding = makeBinding()
            coEvery {
                bindingRepository.observeActiveBindings(
                    "site_01",
                )
            } returns flowOf(listOf(binding))

            viewModel = createViewModel()
            advanceUntilIdle()

            viewModel.onIntent(
                ReturnIntent.SelectBinding(binding),
            )

            assertEquals(
                binding,
                viewModel.state.value.selectedBinding,
            )
        }

    @Test
    fun `confirmReturn shows dialog when data not uploaded`() =
        runTest {
            val binding = makeBinding(dataUploaded = false)
            coEvery {
                bindingRepository.observeActiveBindings(
                    "site_01",
                )
            } returns flowOf(listOf(binding))

            viewModel = createViewModel()
            advanceUntilIdle()

            viewModel.onIntent(
                ReturnIntent.SelectBinding(binding),
            )
            viewModel.onIntent(ReturnIntent.ConfirmReturn)

            assertTrue(
                viewModel.state.value
                    .showConfirmWithoutUpload,
            )
        }

    @Test
    fun `confirmReturn proceeds when data uploaded`() =
        runTest {
            val binding = makeBinding(dataUploaded = true)
            coEvery {
                bindingRepository.observeActiveBindings(
                    "site_01",
                )
            } returns flowOf(listOf(binding))
            coEvery {
                bindingRepository.returnDevice(
                    any(), any(), any(),
                )
            } returns Result.success(
                binding.copy(status = "closed"),
            )

            viewModel = createViewModel()
            advanceUntilIdle()

            viewModel.onIntent(
                ReturnIntent.SelectBinding(binding),
            )
            viewModel.onIntent(ReturnIntent.ConfirmReturn)
            advanceUntilIdle()

            assertFalse(
                viewModel.state.value
                    .showConfirmWithoutUpload,
            )
            coVerify {
                bindingRepository.returnDevice(
                    bindingId = 1L,
                    siteId = "site_01",
                    shiftDate = "2026-02-25",
                )
            }
        }

    @Test
    fun `confirmReturnWithoutUpload calls returnDevice`() =
        runTest {
            val binding = makeBinding(dataUploaded = false)
            coEvery {
                bindingRepository.observeActiveBindings(
                    "site_01",
                )
            } returns flowOf(listOf(binding))
            coEvery {
                bindingRepository.returnDevice(
                    any(), any(), any(),
                )
            } returns Result.success(
                binding.copy(status = "closed"),
            )

            viewModel = createViewModel()
            advanceUntilIdle()

            viewModel.onIntent(
                ReturnIntent.SelectBinding(binding),
            )
            viewModel.onIntent(ReturnIntent.ConfirmReturn)
            viewModel.onIntent(
                ReturnIntent.ConfirmReturnWithoutUpload,
            )
            advanceUntilIdle()

            coVerify {
                bindingRepository.returnDevice(
                    bindingId = 1L,
                    siteId = "site_01",
                    shiftDate = "2026-02-25",
                )
            }
            assertNull(
                viewModel.state.value.selectedBinding,
            )
        }

    @Test
    fun `returnDevice failure shows error`() = runTest {
        val binding = makeBinding(dataUploaded = true)
        coEvery {
            bindingRepository.observeActiveBindings(
                "site_01",
            )
        } returns flowOf(listOf(binding))
        coEvery {
            bindingRepository.returnDevice(
                any(), any(), any(),
            )
        } returns Result.failure(
            RuntimeException("Ошибка сервера"),
        )

        viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onIntent(
            ReturnIntent.SelectBinding(binding),
        )
        viewModel.onIntent(ReturnIntent.ConfirmReturn)
        advanceUntilIdle()

        assertNotNull(viewModel.state.value.error)
        assertFalse(viewModel.state.value.isReturning)
    }

    @Test
    fun `dismissConfirmDialog hides dialog`() = runTest {
        val binding = makeBinding(dataUploaded = false)
        coEvery {
            bindingRepository.observeActiveBindings(
                "site_01",
            )
        } returns flowOf(listOf(binding))

        viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onIntent(
            ReturnIntent.SelectBinding(binding),
        )
        viewModel.onIntent(ReturnIntent.ConfirmReturn)
        assertTrue(
            viewModel.state.value
                .showConfirmWithoutUpload,
        )

        viewModel.onIntent(
            ReturnIntent.DismissConfirmDialog,
        )
        assertFalse(
            viewModel.state.value
                .showConfirmWithoutUpload,
        )
    }

    @Test
    fun `markLost closes binding with lost status`() =
        runTest {
            val binding = makeBinding()
            coEvery {
                bindingRepository.observeActiveBindings(
                    "site_01",
                )
            } returns flowOf(listOf(binding))

            viewModel = createViewModel()
            advanceUntilIdle()

            viewModel.onIntent(
                ReturnIntent.MarkLost(binding),
            )
            advanceUntilIdle()

            coVerify {
                bindingDao.closeBinding(1L, any())
            }
            coVerify {
                deviceDao.updateLocalStatus(
                    "dev_001",
                    "lost",
                    null,
                    null,
                )
            }
            assertNull(
                viewModel.state.value.selectedBinding,
            )
        }
}
