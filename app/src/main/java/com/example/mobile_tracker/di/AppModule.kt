package com.example.mobile_tracker.di

import com.example.mobile_tracker.data.ble.BleProtocol
import com.example.mobile_tracker.data.ble.BleScanner
import com.example.mobile_tracker.data.ble.GattClient
import com.example.mobile_tracker.data.local.datastore.UserPreferencesManager
import com.example.mobile_tracker.data.local.secure.SecureStorage
import com.example.mobile_tracker.data.repository.BindingRepository
import com.example.mobile_tracker.data.repository.ReferenceRepository
import com.example.mobile_tracker.data.repository.UploadRepository
import com.example.mobile_tracker.data.remote.api.DeviceRegistrationApi
import com.example.mobile_tracker.presentation.alerts.AlertsViewModel
import com.example.mobile_tracker.presentation.binding.issue.IssueViewModel
import com.example.mobile_tracker.presentation.binding.return_device.ReturnViewModel
import com.example.mobile_tracker.presentation.context_selection.ContextSelectionViewModel
import com.example.mobile_tracker.presentation.devices.DeviceListViewModel
import com.example.mobile_tracker.presentation.employees.EmployeeSearchViewModel
import com.example.mobile_tracker.presentation.home.HomeViewModel
import com.example.mobile_tracker.presentation.login.LoginViewModel
import com.example.mobile_tracker.presentation.journal.JournalViewModel
import com.example.mobile_tracker.presentation.maps.MapsViewModel
import com.example.mobile_tracker.presentation.monitoring.MonitoringViewModel
import com.example.mobile_tracker.presentation.register_watch.RegisterWatchViewModel
import com.example.mobile_tracker.presentation.settings.SettingsViewModel
import com.example.mobile_tracker.presentation.summary.SummaryViewModel
import com.example.mobile_tracker.presentation.upload.UploadViewModel
import com.example.mobile_tracker.presentation.workers.WorkersViewModel
import com.example.mobile_tracker.presentation.worker_detail.WorkerDetailViewModel
import com.example.mobile_tracker.util.NetworkMonitor
import com.example.mobile_tracker.util.OperatorNotificationManager
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val appModule = module {
    single { SecureStorage(androidContext()) }
    single { UserPreferencesManager(androidContext()) }
    single { OperatorNotificationManager(androidContext(), get()) }
    single {
        ReferenceRepository(
            get(), get(), get(), get(), get(), get(),
        )
    }
    single {
        BindingRepository(
            get(), get(), get(), get(), get(),
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

    // Device Registration
    single { DeviceRegistrationApi(get<com.example.mobile_tracker.data.remote.NetworkClient>().httpClient) }

    viewModel { LoginViewModel(get(), get(), get()) }
    viewModel {
        ContextSelectionViewModel(
            get(), get(), get(), get(), get(),
        )
    }
    single { NetworkMonitor(androidContext()) }

    viewModel { HomeViewModel(get(), get(), get(), get(), get(), get()) }
    viewModel {
        DeviceListViewModel(get(), get(), get())
    }
    viewModel {
        EmployeeSearchViewModel(get(), get(), get(), get(), get())
    }
    viewModel { MonitoringViewModel(get(), get(), get(), get(), get(), get(), get(), get(), get()) }
    viewModel { WorkersViewModel(get(), get(), get(), get(), get(), get(), get(), get()) }
    viewModel { MapsViewModel(get(), get(), get(), get(), get(), get(), get(), get(), get()) }
    viewModel { WorkerDetailViewModel(get(), get(), get(), get(), get(), get(), get(), get(), get()) }
    viewModel {
        IssueViewModel(get(), get(), get(), get())
    }
    viewModel {
        ReturnViewModel(get(), get(), get(), get())
    }
    viewModel {
        UploadViewModel(get(), get(), get(), get(), get(), get())
    }
    viewModel { JournalViewModel(get(), get()) }
    viewModel { AlertsViewModel(get(), get(), get(), get(), get()) }
    viewModel { SummaryViewModel(get(), get(), get(), get()) }
    viewModel {
        SettingsViewModel(
            get(), get(), get(), get(),
            androidContext(),
        )
    }
    viewModel { RegisterWatchViewModel(get(), get(), get()) }
}
