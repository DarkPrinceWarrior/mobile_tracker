package com.example.mobile_tracker.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class LoginRequest(
    val email: String,
    val password: String,
)

/**
 * Реальный бэкенд POST /auth/login возвращает только access_token + token_type.
 * Нет refresh_token, нет user, нет expires_in.
 */
@Serializable
data class LoginResponse(
    @SerialName("access_token") val accessToken: String,
    @SerialName("token_type") val tokenType: String,
)

/**
 * GET /auth/me — информация о текущем пользователе.
 * Реальный бэкенд возвращает: id, email, full_name, role, status.
 */
@Serializable
data class UserDto(
    val id: String,
    val email: String,
    @SerialName("full_name") val fullName: String,
    val role: String,
    val status: String = "active",
)

@Serializable
data class RefreshTokenRequest(
    @SerialName("device_id") val deviceId: String,
    @SerialName("refresh_token") val refreshToken: String,
)

@Serializable
data class RefreshTokenResponse(
    @SerialName("access_token") val accessToken: String,
    @SerialName("refresh_token") val refreshToken: String,
    @SerialName("expires_in") val expiresIn: Long,
)
