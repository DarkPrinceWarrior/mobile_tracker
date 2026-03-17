package com.example.mobile_tracker.data.remote.api

import com.example.mobile_tracker.data.remote.dto.ShiftRouteResponse
import com.example.mobile_tracker.data.remote.dto.ShiftZonesResponse
import com.example.mobile_tracker.data.remote.dto.ZoneListResponse
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter

class ZonesApi(private val client: HttpClient) {

    /**
     * GET /sites/{site_id}/zones — список зон объекта.
     *
     * site_id берётся из текущего контекста оператора
     * (ShiftContext / UserPreferences).
     */
    suspend fun getSiteZones(
        siteId: String,
        page: Int = 1,
        pageSize: Int = 200,
    ): ZoneListResponse =
        client.get("/api/v1/sites/$siteId/zones") {
            parameter("page", page)
            parameter("page_size", pageSize)
        }.body()

    /**
     * GET /shifts/{shift_id}/zones — зоны, посещённые за смену.
     *
     * Возвращает список посещений и сводку по зонам.
     */
    suspend fun getShiftZones(
        shiftId: String,
    ): ShiftZonesResponse =
        client.get("/api/v1/shifts/$shiftId/zones").body()

    /**
     * GET /shifts/{shift_id}/route — маршрут перемещения за смену.
     *
     * Хронологический порядок: зона → зона → зона.
     */
    suspend fun getShiftRoute(
        shiftId: String,
    ): ShiftRouteResponse =
        client.get("/api/v1/shifts/$shiftId/route").body()
}
