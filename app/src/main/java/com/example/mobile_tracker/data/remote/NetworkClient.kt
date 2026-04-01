package com.example.mobile_tracker.data.remote

import android.content.Context
import android.os.Build
import android.provider.Settings
import com.example.mobile_tracker.data.local.secure.SecureStorage
import com.example.mobile_tracker.data.remote.dto.RefreshTokenRequest
import com.example.mobile_tracker.data.remote.dto.RefreshTokenResponse
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpResponseValidator
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
    private val context: Context,
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
                url(resolveBaseUrl())
                contentType(ContentType.Application.Json)

                val token = secureStorage.accessToken
                if (!token.isNullOrBlank()) {
                    bearerAuth(token)
                }
            }

            HttpResponseValidator {
                validateResponse { response ->
                    if (response.status.value == 403) {
                        Timber.w(
                            "HTTP 403: forced logout",
                        )
                        secureStorage.clearTokens()
                    }
                }
            }
        }
    }

    private fun resolveBaseUrl(): String {
        val baseUrl = NetworkConfig.BASE_URL
        if (!isProbablyEmulator()) {
            return baseUrl
        }

        return baseUrl
            .replace("://localhost", "://10.0.2.2")
            .replace("://127.0.0.1", "://10.0.2.2")
    }

    private fun isProbablyEmulator(): Boolean {
        return Build.FINGERPRINT.contains("generic", ignoreCase = true) ||
            Build.FINGERPRINT.contains("emulator", ignoreCase = true) ||
            Build.MODEL.contains("Emulator", ignoreCase = true) ||
            Build.MODEL.contains("Android SDK built for", ignoreCase = true) ||
            Build.MANUFACTURER.contains("Genymotion", ignoreCase = true) ||
            Build.PRODUCT.contains("sdk", ignoreCase = true) ||
            Build.PRODUCT.contains("emulator", ignoreCase = true) ||
            Build.HARDWARE.contains("ranchu", ignoreCase = true) ||
            Build.HARDWARE.contains("goldfish", ignoreCase = true)
    }

    suspend fun refreshToken(): Boolean {
        val currentRefresh = secureStorage.refreshToken
            ?: return false

        return try {
            val response: HttpResponse = httpClient.post(
                "/api/v1/auth/device/refresh",
            ) {
                header("Authorization", "")
                val androidId = Settings.Secure.getString(
                    context.contentResolver,
                    Settings.Secure.ANDROID_ID,
                ) ?: "unknown"
                setBody(
                    RefreshTokenRequest(
                        deviceId = androidId,
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
