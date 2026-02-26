package com.example.mobile_tracker.di

import com.example.mobile_tracker.data.local.datastore.UserPreferencesManager
import com.example.mobile_tracker.data.local.secure.SecureStorage
import com.example.mobile_tracker.presentation.context_selection.ContextSelectionViewModel
import com.example.mobile_tracker.presentation.home.HomeViewModel
import com.example.mobile_tracker.presentation.login.LoginViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val appModule = module {
    single { SecureStorage(androidContext()) }
    single { UserPreferencesManager(androidContext()) }

    viewModel { LoginViewModel(get(), get(), get()) }
    viewModel { ContextSelectionViewModel(get(), get()) }
    viewModel { HomeViewModel(get()) }
}
