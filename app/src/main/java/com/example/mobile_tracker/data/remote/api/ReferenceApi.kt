package com.example.mobile_tracker.data.remote.api

import com.example.mobile_tracker.data.remote.dto.DeviceDto
import com.example.mobile_tracker.data.remote.dto.DowntimeReasonDto
import com.example.mobile_tracker.data.remote.dto.EmployeeDto
import com.example.mobile_tracker.data.remote.dto.PaginatedResponse
import com.example.mobile_tracker.data.remote.dto.SiteDto
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter

class ReferenceApi(private val client: HttpClient) {

    suspend fun getEmployees(
        siteId: String,
        page: Int = 1,
        pageSize: Int = 100,
    ): List<EmployeeDto> =
        client.get("/api/v1/employees/") {
            parameter("site_id", siteId)
            parameter("status", "active")
            parameter("limit", pageSize)
            parameter("offset", (page - 1) * pageSize)
        }.body()

    /**
     * GET /employees/{uuid} — детали одного сотрудника.
     */
    suspend fun getEmployee(
        employeeUuid: String,
    ): EmployeeDto =
        client.get("/api/v1/employees/$employeeUuid").body()

    suspend fun getDevices(
        siteId: String,
        page: Int = 1,
        pageSize: Int = 100,
    ): PaginatedResponse<DeviceDto> =
        client.get("/api/v1/devices") {
            parameter("site_id", siteId)
            parameter("status", "active")
            parameter("page", page)
            parameter("page_size", pageSize)
        }.body()

    /**
     * GET /devices/{device_id} — детальная информация об устройстве.
     *
     * Возвращает все поля из списка + last_packet_id,
     * last_packet_status, last_packet_received_at, battery_level.
     */
    suspend fun getDevice(
        deviceId: String,
    ): DeviceDto =
        client.get("/api/v1/devices/$deviceId").body()

    suspend fun getSites(): List<SiteDto> =
        client.get("/api/v1/sites/").body()

    suspend fun getDowntimeReasons(): List<DowntimeReasonDto> =
        client.get("/api/v1/downtime-reasons/").body()
}
