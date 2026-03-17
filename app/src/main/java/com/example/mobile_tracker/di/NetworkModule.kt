package com.example.mobile_tracker.di

import com.example.mobile_tracker.data.remote.NetworkClient
import com.example.mobile_tracker.data.remote.api.AnomaliesApi
import com.example.mobile_tracker.data.remote.api.AuthApi
import com.example.mobile_tracker.data.remote.api.BindingApi
import com.example.mobile_tracker.data.remote.api.GatewayApi
import com.example.mobile_tracker.data.remote.api.HeartbeatApi
import com.example.mobile_tracker.data.remote.api.ReferenceApi
import com.example.mobile_tracker.data.remote.api.ShiftsApi
import com.example.mobile_tracker.data.remote.api.ZonesApi
import org.koin.dsl.module

val networkModule = module {
    single { NetworkClient(get()) }
    single { get<NetworkClient>().httpClient }
    single { AuthApi(get()) }
    single { ReferenceApi(get()) }
    single { BindingApi(get()) }
    single { GatewayApi(get()) }
    single { ShiftsApi(get()) }
    single { ZonesApi(get()) }
    single { AnomaliesApi(get()) }
    single { HeartbeatApi(get()) }
}
