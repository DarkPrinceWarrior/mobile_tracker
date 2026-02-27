package com.example.mobile_tracker.di

import com.example.mobile_tracker.data.remote.api.AuthApi
import com.example.mobile_tracker.data.remote.api.BindingApi
import com.example.mobile_tracker.data.remote.api.GatewayApi
import com.example.mobile_tracker.data.remote.api.ReferenceApi
import com.example.mobile_tracker.data.remote.demo.FakeHttpEngine
import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import org.koin.dsl.module

val demoNetworkModule = module {
    single {
        HttpClient(FakeHttpEngine.engine) {
            install(ContentNegotiation) {
                json(
                    Json {
                        ignoreUnknownKeys = true
                        encodeDefaults = true
                        isLenient = true
                    },
                )
            }

            defaultRequest {
                contentType(ContentType.Application.Json)
            }
        }
    }

    single { AuthApi(get()) }
    single { ReferenceApi(get()) }
    single { BindingApi(get()) }
    single { GatewayApi(get()) }
}
