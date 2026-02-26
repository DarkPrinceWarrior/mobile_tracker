package com.example.mobile_tracker.data.remote

import com.example.mobile_tracker.data.local.secure.SecureStorage
import com.example.mobile_tracker.data.remote.dto.RefreshTokenRequest
import com.example.mobile_tracker.data.remote.dto.RefreshTokenResponse
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import timber.log.Timber

class NetworkClient(
    private val secureStorage: SecureStorage,
) {
    private val jsonConfig = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        isLenient = true
    }

    val httpClient: HttpClient by lazy {
        HttpClient(OkHttp) {
            install(ContentNegotiation) {
                json(jsonConfig)
            }

            install(HttpTimeout) {
                connectTimeoutMillis =
                    NetworkConfig.CONNECT_TIMEOUT_SEC * 1000
                requestTimeoutMillis =
                    NetworkConfig.READ_TIMEOUT_SEC * 1000
                socketTimeoutMillis =
                    NetworkConfig.WRITE_TIMEOUT_SEC * 1000
            }

            install(Logging) {
                level = LogLevel.BODY
                logger = object :
                    io.ktor.client.plugins.logging.Logger {
                    override fun log(message: String) {
                        Timber.tag("HTTP").d(message)
                    }
                }
            }

            defaultRequest {
                url(NetworkConfig.BASE_URL)
                contentType(ContentType.Application.Json)

                val token = secureStorage.accessToken
                if (!token.isNullOrBlank()) {
                    bearerAuth(token)
                }
            }
        }
    }

    suspend fun refreshToken(): Boolean {
        val currentRefresh = secureStorage.refreshToken
            ?: return false

        return try {
            val response: HttpResponse = httpClient.post(
                "/api/v1/auth/refresh",
            ) {
                header("Authorization", "")
                setBody(
                    RefreshTokenRequest(
                        refreshToken = currentRefresh,
                    ),
                )
            }

            if (response.status.value == 200) {
                val body = response.body<RefreshTokenResponse>()
                secureStorage.accessToken = body.accessToken
                secureStorage.refreshToken = body.refreshToken
                true
            } else {
                false
            }
        } catch (e: Exception) {
            Timber.e(e, "Token refresh failed")
            false
        }
    }
}
