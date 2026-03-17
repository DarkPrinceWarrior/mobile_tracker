package com.example.mobile_tracker.data.remote.api

import com.example.mobile_tracker.data.remote.dto.BindingResponse
import com.example.mobile_tracker.data.remote.dto.CloseBindingRequest
import com.example.mobile_tracker.data.remote.dto.CloseBindingResponse
import com.example.mobile_tracker.data.remote.dto.CreateBindingRequest
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse

class BindingApi(private val client: HttpClient) {

    suspend fun createBinding(
        request: CreateBindingRequest,
    ): HttpResponse =
        client.post("/api/v1/bindings/") {
            setBody(request)
        }

    suspend fun closeBinding(
        bindingId: Long,
        request: CloseBindingRequest = CloseBindingRequest(),
    ): HttpResponse =
        client.put("/api/v1/bindings/$bindingId/close") {
            setBody(request)
        }

    /**
     * GET /bindings/ — список привязок с фильтрами.
     *
     * Можно фильтровать по:
     * - employee_id: история привязок сотрудника
     * - device_id: кто носил эти часы
     * - site_id: привязки на площадке
     * - shift_date: привязки за дату
     */
    suspend fun getBindings(
        siteId: String? = null,
        employeeId: String? = null,
        deviceId: String? = null,
        shiftDate: String? = null,
        page: Int = 1,
        pageSize: Int = 20,
    ): List<BindingResponse> =
        client.get("/api/v1/bindings/") {
            if (siteId != null) parameter("site_id", siteId)
            if (employeeId != null) parameter("employee_id", employeeId)
            if (deviceId != null) parameter("device_id", deviceId)
            if (shiftDate != null) parameter("shift_date", shiftDate)
            parameter("page", page)
            parameter("page_size", pageSize)
        }.body()
}
