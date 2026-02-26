package com.example.mobile_tracker.data.remote.api

import com.example.mobile_tracker.data.remote.dto.LoginRequest
import com.example.mobile_tracker.data.remote.dto.LoginResponse
import com.example.mobile_tracker.data.remote.dto.UserDto
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse

class AuthApi(private val client: HttpClient) {

    suspend fun login(
        email: String,
        password: String,
    ): HttpResponse = client.post("/api/v1/auth/login") {
        setBody(LoginRequest(email, password))
    }

    suspend fun getMe(): UserDto =
        client.get("/api/v1/auth/me").body()

    suspend fun loginParsed(
        email: String,
        password: String,
    ): LoginResponse = login(email, password).body()
}
