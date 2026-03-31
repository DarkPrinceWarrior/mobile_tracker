package com.example.mobile_tracker.data.remote.api

import com.example.mobile_tracker.data.remote.dto.SensorSampleResponse
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter

/**
 * GET /api/v1/sensor-samples/{stream}
 *
 * Сенсорные данные фильтруются по shift_id (НЕ device_id!).
 * При перепривязке часов другому сотруднику device_id отдаёт данные прошлого
 * владельца — shift_id гарантирует данные только текущей смены.
 *
 * Streams: heart-rate | wear | battery | accel | gyro | baro | mag | ble | downtime
 * sort=-ts_ms — последняя запись первой.
 */
class SensorSamplesApi(private val client: HttpClient) {

    /**
     * GET /api/v1/sensor-samples/heart-rate?shift_id={id}&sort=-ts_ms&limit=1
     * payload: { "bpm": 75, "confidence": 0.95 }
     * ⚠️ heart-rate через дефис, не hr и не heart_rate
     */
    suspend fun getHeartRate(shiftId: String, limit: Int = 1): SensorSampleResponse? =
        client.get("/api/v1/sensor-samples/heart-rate") {
            parameter("shift_id", shiftId)
            parameter("sort", "-ts_ms")
            parameter("limit", limit)
        }.body<List<SensorSampleResponse>>().firstOrNull()

    /**
     * GET /api/v1/sensor-samples/wear?shift_id={id}&sort=-ts_ms&limit=1
     * payload: { "state": "on" | "off" }
     */
    suspend fun getWear(shiftId: String, limit: Int = 1): SensorSampleResponse? =
        client.get("/api/v1/sensor-samples/wear") {
            parameter("shift_id", shiftId)
            parameter("sort", "-ts_ms")
            parameter("limit", limit)
        }.body<List<SensorSampleResponse>>().firstOrNull()

    /**
     * GET /api/v1/sensor-samples/battery?shift_id={id}&sort=-ts_ms&limit=1
     * payload: { "level": 0.35 } → × 100 = %
     */
    suspend fun getBattery(shiftId: String, limit: Int = 1): SensorSampleResponse? =
        client.get("/api/v1/sensor-samples/battery") {
            parameter("shift_id", shiftId)
            parameter("sort", "-ts_ms")
            parameter("limit", limit)
        }.body<List<SensorSampleResponse>>().firstOrNull()
}
