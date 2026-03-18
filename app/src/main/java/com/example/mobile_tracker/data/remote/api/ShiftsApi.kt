package com.example.mobile_tracker.data.remote.api

import com.example.mobile_tracker.data.remote.dto.ShiftActivityResponse
import com.example.mobile_tracker.data.remote.dto.ShiftListItem
import com.example.mobile_tracker.data.remote.dto.ShiftListResponse
import com.example.mobile_tracker.data.remote.dto.ShiftMetricsResponse
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter

class ShiftsApi(private val client: HttpClient) {

    /**
     * GET /shifts — список смен с пагинацией и фильтрами.
     */
    suspend fun getShifts(
        deviceId: String? = null,
        employeeId: String? = null,
        dateFrom: String? = null,
        dateTo: String? = null,
        page: Int = 1,
        pageSize: Int = 20,
    ): ShiftListResponse =
        client.get("/api/v1/shifts") {
            if (deviceId != null) parameter("device_id", deviceId)
            if (employeeId != null) parameter("employee_id", employeeId)
            if (dateFrom != null) parameter("date_from", dateFrom)
            if (dateTo != null) parameter("date_to", dateTo)
            parameter("page", page)
            parameter("page_size", pageSize)
        }.body()

    /**
     * GET /shifts/{shift_id} — детали одной смены.
     */
    suspend fun getShift(shiftId: String): ShiftListItem =
        client.get("/api/v1/shifts/$shiftId").body()

    /**
     * GET /shifts/{shift_id}/metrics — агрегированные метрики смены.
     */
    suspend fun getShiftMetrics(shiftId: String): ShiftMetricsResponse =
        client.get("/api/v1/shifts/$shiftId/metrics").body()

    /**
     * GET /shifts/{shift_id}/activity — классификация активности за смену.
     */
    suspend fun getShiftActivity(shiftId: String): ShiftActivityResponse =
        client.get("/api/v1/shifts/$shiftId/activity").body()
}
