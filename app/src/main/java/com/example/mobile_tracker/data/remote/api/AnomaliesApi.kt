package com.example.mobile_tracker.data.remote.api

import com.example.mobile_tracker.data.remote.dto.AnomalyItem
import com.example.mobile_tracker.data.remote.dto.AnomalyListResponse
import com.example.mobile_tracker.data.remote.dto.ShiftAnomaliesResponse
import com.example.mobile_tracker.data.remote.dto.UpdateAnomalyRequest
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.patch
import io.ktor.client.request.setBody

class AnomaliesApi(private val client: HttpClient) {

    /**
     * GET /anomalies — список всех аномалий с фильтрами.
     */
    suspend fun getAnomalies(
        siteId: String? = null,
        employeeId: String? = null,
        severity: String? = null,
        status: String? = null,
        anomalyType: String? = null,
        dateFrom: String? = null,
        dateTo: String? = null,
        page: Int = 1,
        pageSize: Int = 20,
    ): AnomalyListResponse =
        client.get("/api/v1/anomalies") {
            if (siteId != null) parameter("site_id", siteId)
            if (employeeId != null) parameter("employee_id", employeeId)
            if (severity != null) parameter("severity", severity)
            if (status != null) parameter("status", status)
            if (anomalyType != null) parameter("anomaly_type", anomalyType)
            if (dateFrom != null) parameter("date_from", dateFrom)
            if (dateTo != null) parameter("date_to", dateTo)
            parameter("page", page)
            parameter("page_size", pageSize)
        }.body()

    /**
     * GET /shifts/{shift_id}/anomalies — аномалии конкретной смены.
     */
    suspend fun getShiftAnomalies(
        shiftId: String,
    ): ShiftAnomaliesResponse =
        client.get("/api/v1/shifts/$shiftId/anomalies").body()

    /**
     * PATCH /anomalies/{anomaly_id} — обновить статус аномалии.
     *
     * Статусы: open → acknowledged → resolved / false_positive
     */
    suspend fun updateAnomaly(
        anomalyId: String,
        request: UpdateAnomalyRequest,
    ): AnomalyItem =
        client.patch("/api/v1/anomalies/$anomalyId") {
            setBody(request)
        }.body()
}
