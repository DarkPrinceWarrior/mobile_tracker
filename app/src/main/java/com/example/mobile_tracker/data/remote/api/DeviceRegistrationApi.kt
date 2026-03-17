package com.example.mobile_tracker.data.remote.api

import com.example.mobile_tracker.data.remote.dto.MobileRegisterRequest
import io.ktor.client.HttpClient
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse

class DeviceRegistrationApi(private val client: HttpClient) {

    suspend fun registerWatchViaMobile(
        request: MobileRegisterRequest,
    ): HttpResponse =
        client.post("/api/v1/auth/device/register-via-mobile") {
            setBody(request)
        }
}
