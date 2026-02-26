package com.example.mobile_tracker.di

import com.example.mobile_tracker.data.ble.BleProtocol
import com.example.mobile_tracker.data.ble.BleScanner
import com.example.mobile_tracker.data.ble.GattClient
import com.example.mobile_tracker.data.local.datastore.UserPreferencesManager
import com.example.mobile_tracker.data.local.secure.SecureStorage
import com.example.mobile_tracker.data.repository.BindingRepository
import com.example.mobile_tracker.data.repository.ReferenceRepository
import com.example.mobile_tracker.data.repository.UploadRepository
import com.example.mobile_tracker.presentation.binding.issue.IssueViewModel
import com.example.mobile_tracker.presentation.binding.return_device.ReturnViewModel
import com.example.mobile_tracker.presentation.context_selection.ContextSelectionViewModel
import com.example.mobile_tracker.presentation.devices.DeviceListViewModel
import com.example.mobile_tracker.presentation.employees.EmployeeSearchViewModel
import com.example.mobile_tracker.presentation.home.HomeViewModel
import com.example.mobile_tracker.presentation.login.LoginViewModel
import com.example.mobile_tracker.presentation.upload.UploadViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val appModule = module {
    single { SecureStorage(androidContext()) }
    single { UserPreferencesManager(androidContext()) }
    single {
        ReferenceRepository(
            get(), get(), get(), get(), get(),
        )
    }
    single {
        BindingRepository(
            get(), get(), get(), get(),
        )
    }

    // BLE
    factory { BleScanner(androidContext()) }
    factory { GattClient(androidContext()) }
    factory { BleProtocol(get(), get()) }

    // Upload
    single {
        UploadRepository(get(), get(), get(), get())
    }

    viewModel { LoginViewModel(get(), get(), get()) }
    viewModel {
        ContextSelectionViewModel(get(), get(), get())
    }
    viewModel { HomeViewModel(get()) }
    viewModel {
        DeviceListViewModel(get(), get(), get())
    }
    viewModel {
        EmployeeSearchViewModel(get(), get(), get())
    }
    viewModel {
        IssueViewModel(get(), get(), get(), get())
    }
    viewModel {
        ReturnViewModel(get(), get(), get(), get())
    }
    viewModel {
        UploadViewModel(get(), get(), get(), get())
    }
}
