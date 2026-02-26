package com.example.mobile_tracker.presentation.settings

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import com.example.mobile_tracker.data.local.datastore.UserPreferences
import com.example.mobile_tracker.data.local.datastore.UserPreferencesManager
import com.example.mobile_tracker.data.local.db.dao.ShiftContextDao
import com.example.mobile_tracker.data.local.db.entity.ShiftContextEntity
import com.example.mobile_tracker.data.local.secure.SecureStorage
import com.example.mobile_tracker.data.repository.ReferenceRepository
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    private val secureStorage = mockk<SecureStorage>(relaxed = true)
    private val preferencesManager =
        mockk<UserPreferencesManager>()
    private val shiftContextDao = mockk<ShiftContextDao>()
    private val referenceRepository =
        mockk<ReferenceRepository>()
    private val appContext = mockk<Context>()
    private val packageManager = mockk<PackageManager>()

    private lateinit var viewModel: SettingsViewModel

    private val testPrefs = UserPreferences(
        userId = "user-1",
        userEmail = "operator@test.com",
        userName = "Иванов И.И.",
        userRole = "operator",
        isLoggedIn = true,
    )

    private val testContext = ShiftContextEntity(
        id = 1,
        siteId = "site-1",
        siteName = "Площадка А",
        shiftDate = "2025-02-26",
        shiftType = "day",
        operatorId = "user-1",
        operatorName = "Иванов И.И.",
    )

    @Before
    fun setup() {
        Dispatchers.setMain(dispatcher)

        every {
            preferencesManager.userPreferences
        } returns flowOf(testPrefs)

        coEvery { shiftContextDao.get() } returns testContext

        val packageInfo = PackageInfo().apply {
            versionName = "1.2.3"
        }
        every { appContext.packageManager } returns packageManager
        every { appContext.packageName } returns "com.example.test"
        every {
            packageManager.getPackageInfo(
                "com.example.test", 0,
            )
        } returns packageInfo

        viewModel = SettingsViewModel(
            secureStorage,
            preferencesManager,
            shiftContextDao,
            referenceRepository,
            appContext,
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `loads operator and context on init`() = runTest {
        advanceUntilIdle()

        val state = viewModel.state.value
        assertEquals("Иванов И.И.", state.operatorName)
        assertEquals("operator@test.com", state.operatorEmail)
        assertEquals("Площадка А", state.siteName)
        assertEquals("2025-02-26", state.shiftDate)
        assertEquals("day", state.shiftType)
        assertEquals("1.2.3", state.appVersion)
    }

    @Test
    fun `logout clears tokens and preferences`() = runTest {
        advanceUntilIdle()

        coEvery { preferencesManager.clearAll() } just Runs
        coEvery { shiftContextDao.clear() } just Runs

        viewModel.onIntent(SettingsIntent.LogoutClicked)
        assertTrue(viewModel.state.value.showLogoutDialog)

        viewModel.onIntent(SettingsIntent.LogoutConfirmed)
        advanceUntilIdle()

        coVerify { secureStorage.clearTokens() }
        coVerify { preferencesManager.clearAll() }
        coVerify { shiftContextDao.clear() }
    }

    @Test
    fun `logout dismiss hides dialog`() = runTest {
        advanceUntilIdle()

        viewModel.onIntent(SettingsIntent.LogoutClicked)
        assertTrue(viewModel.state.value.showLogoutDialog)

        viewModel.onIntent(SettingsIntent.LogoutDismissed)
        assertFalse(viewModel.state.value.showLogoutDialog)
    }

    @Test
    fun `change context clears shift context`() = runTest {
        advanceUntilIdle()

        coEvery { shiftContextDao.clear() } just Runs

        viewModel.onIntent(
            SettingsIntent.ChangeContextClicked,
        )
        advanceUntilIdle()

        coVerify { shiftContextDao.clear() }
    }

    @Test
    fun `clear cache calls repository clearAll`() = runTest {
        advanceUntilIdle()

        coEvery { referenceRepository.clearAll() } just Runs

        viewModel.onIntent(SettingsIntent.ClearCacheClicked)
        assertTrue(
            viewModel.state.value.showClearCacheDialog,
        )

        viewModel.onIntent(
            SettingsIntent.ClearCacheConfirmed,
        )
        advanceUntilIdle()

        coVerify { referenceRepository.clearAll() }
        assertFalse(viewModel.state.value.isLoading)
    }

    @Test
    fun `clear cache dismiss hides dialog`() = runTest {
        advanceUntilIdle()

        viewModel.onIntent(SettingsIntent.ClearCacheClicked)
        assertTrue(
            viewModel.state.value.showClearCacheDialog,
        )

        viewModel.onIntent(
            SettingsIntent.ClearCacheDismissed,
        )
        assertFalse(
            viewModel.state.value.showClearCacheDialog,
        )
    }

    @Test
    fun `clear cache error updates state`() = runTest {
        advanceUntilIdle()

        coEvery {
            referenceRepository.clearAll()
        } throws RuntimeException("DB error")

        viewModel.onIntent(SettingsIntent.ClearCacheClicked)
        viewModel.onIntent(
            SettingsIntent.ClearCacheConfirmed,
        )
        advanceUntilIdle()

        val state = viewModel.state.value
        assertFalse(state.isLoading)
        assertTrue(state.error?.contains("DB error") == true)
    }

    @Test
    fun `dismiss error clears error`() = runTest {
        advanceUntilIdle()

        coEvery {
            referenceRepository.clearAll()
        } throws RuntimeException("fail")

        viewModel.onIntent(SettingsIntent.ClearCacheClicked)
        viewModel.onIntent(
            SettingsIntent.ClearCacheConfirmed,
        )
        advanceUntilIdle()
        assertTrue(viewModel.state.value.error != null)

        viewModel.onIntent(SettingsIntent.DismissError)
        assertEquals(null, viewModel.state.value.error)
    }
}
