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
        pageSize: Int = 200,
    ): PaginatedResponse<EmployeeDto> =
        client.get("/api/v1/employees") {
            parameter("site_id", siteId)
            parameter("status", "active")
            parameter("page", page)
            parameter("page_size", pageSize)
        }.body()

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

    suspend fun getSites(): List<SiteDto> =
        client.get("/api/v1/sites").body()

    suspend fun getDowntimeReasons(): List<DowntimeReasonDto> =
        client.get("/api/v1/downtime-reasons").body()
}
