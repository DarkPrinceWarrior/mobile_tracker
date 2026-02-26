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
        client.post("/api/v1/bindings") {
            setBody(request)
        }

    suspend fun closeBinding(
        bindingId: Long,
        request: CloseBindingRequest = CloseBindingRequest(),
    ): HttpResponse =
        client.put("/api/v1/bindings/$bindingId/close") {
            setBody(request)
        }

    suspend fun getBindings(
        siteId: String,
        date: String? = null,
    ): List<BindingResponse> =
        client.get("/api/v1/bindings") {
            parameter("site_id", siteId)
            if (date != null) parameter("date", date)
        }.body()
}
