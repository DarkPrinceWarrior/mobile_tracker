package com.example.mobile_tracker.data.remote.api

import com.example.mobile_tracker.data.remote.dto.UploadPacketRequest
import io.ktor.client.HttpClient
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse

class GatewayApi(private val client: HttpClient) {

    suspend fun uploadPacket(
        request: UploadPacketRequest,
    ): HttpResponse =
        client.post("/api/v1/gateway/packets") {
            header(
                "Idempotency-Key", request.packetId,
            )
            setBody(request)
        }
}
