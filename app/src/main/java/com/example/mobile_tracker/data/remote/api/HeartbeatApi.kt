package com.example.mobile_tracker.data.remote.api

import com.example.mobile_tracker.data.remote.dto.HeartbeatRequest
import com.example.mobile_tracker.data.remote.dto.HeartbeatResponse
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody

class HeartbeatApi(private val client: HttpClient) {

    /**
     * POST /watch/heartbeat — отправка heartbeat от часов через gateway.
     *
     * Рекомендуется вызывать каждые 1-5 минут во время активной работы.
     *
     * Что делает сервер:
     * - Обновляет last_heartbeat_at и last_sync_at устройства
     * - Обновляет battery_level и app_version
     * - Возвращает серверное время для синхронизации
     */
    suspend fun sendHeartbeat(
        request: HeartbeatRequest,
    ): HeartbeatResponse =
        client.post("/api/v1/watch/heartbeat") {
            setBody(request)
        }.body()
}
