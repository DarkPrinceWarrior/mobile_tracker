package com.example.mobile_tracker.di

import com.example.mobile_tracker.data.remote.NetworkClient
import com.example.mobile_tracker.data.remote.api.AuthApi
import com.example.mobile_tracker.data.remote.api.BindingApi
import com.example.mobile_tracker.data.remote.api.ReferenceApi
import org.koin.dsl.module

val networkModule = module {
    single { NetworkClient(get()) }
    single { get<NetworkClient>().httpClient }
    single { AuthApi(get()) }
    single { ReferenceApi(get()) }
    single { BindingApi(get()) }
}
