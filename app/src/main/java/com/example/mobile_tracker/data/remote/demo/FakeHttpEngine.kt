package com.example.mobile_tracker.data.remote.demo

import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import timber.log.Timber
import java.util.concurrent.atomic.AtomicLong

object FakeHttpEngine {

    private val bindingIdCounter = AtomicLong(100)

    val engine = MockEngine { request ->
        val path = request.url.encodedPath
        val method = request.method

        Timber.d("DEMO ➜ $method $path")

        val json = route(method, path)

        respond(
            content = json,
            status = HttpStatusCode.OK,
            headers = headersOf(
                HttpHeaders.ContentType,
                ContentType.Application.Json.toString(),
            ),
        )
    }

    private fun route(
        method: HttpMethod,
        path: String,
    ): String = when {
        // ── Auth ─────────────────────────
        path.endsWith("/auth/login") ->
            loginResponse()

        path.endsWith("/auth/me") ->
            meResponse()

        path.endsWith("/auth/refresh") ->
            refreshResponse()

        // ── Reference: employees ─────────
        path.contains("/employees") ->
            employeesResponse()

        // ── Reference: devices ───────────
        path.contains("/devices") && method == HttpMethod.Get ->
            devicesResponse()

        // ── Reference: sites ─────────────
        path.contains("/sites") ->
            sitesResponse()

        // ── Reference: downtime-reasons ──
        path.contains("/downtime-reasons") ->
            downtimeReasonsResponse()

        // ── Bindings ─────────────────────
        path.contains("/bindings") &&
            path.contains("/close") ->
            closeBindingResponse()

        path.endsWith("/bindings") &&
            method == HttpMethod.Post ->
            createBindingResponse()

        path.contains("/bindings") &&
            method == HttpMethod.Get ->
            bindingsListResponse()

        // ── Gateway ──────────────────────
        path.contains("/gateway/packets") ->
            uploadPacketResponse()

        else -> {
            Timber.w("DEMO ➜ unhandled: $method $path")
            """{"status":"ok"}"""
        }
    }

    // ── Auth responses ───────────────────

    private fun loginResponse() = """
    {
      "access_token": "demo-access-token-123",
      "refresh_token": "demo-refresh-token-456",
      "token_type": "Bearer",
      "expires_in": 86400,
      "user": {
        "id": "user-demo-1",
        "email": "demo@tracker.local",
        "full_name": "Демо Оператор",
        "role": "operator",
        "scope_type": "site",
        "scope_ids": ["site-demo-1", "site-demo-2"]
      }
    }
    """.trimIndent()

    private fun meResponse() = """
    {
      "id": "user-demo-1",
      "email": "demo@tracker.local",
      "full_name": "Демо Оператор",
      "role": "operator",
      "scope_type": "site",
      "scope_ids": ["site-demo-1", "site-demo-2"]
    }
    """.trimIndent()

    private fun refreshResponse() = """
    {
      "access_token": "demo-access-refreshed-789",
      "refresh_token": "demo-refresh-new-012",
      "expires_in": 86400
    }
    """.trimIndent()

    // ── Reference responses ──────────────

    private fun employeesResponse() = """
    {
      "data": [
        {
          "id": "emp-1",
          "full_name": "Иванов Иван Иванович",
          "company_id": "comp-1",
          "company_name": "ООО СтройМонтаж",
          "position": "Монтажник",
          "pass_number": "A-1001",
          "personnel_number": "TN-2001",
          "brigade_id": "br-1",
          "brigade_name": "Бригада №1",
          "site_id": "site-demo-1",
          "status": "active"
        },
        {
          "id": "emp-2",
          "full_name": "Петров Пётр Сергеевич",
          "company_id": "comp-1",
          "company_name": "ООО СтройМонтаж",
          "position": "Сварщик",
          "pass_number": "A-1002",
          "personnel_number": "TN-2002",
          "brigade_id": "br-1",
          "brigade_name": "Бригада №1",
          "site_id": "site-demo-1",
          "status": "active"
        },
        {
          "id": "emp-3",
          "full_name": "Сидоров Алексей Николаевич",
          "company_id": "comp-2",
          "company_name": "ООО ЭнергоСтрой",
          "position": "Электрик",
          "pass_number": "B-2001",
          "personnel_number": "TN-3001",
          "brigade_id": "br-2",
          "brigade_name": "Бригада №2",
          "site_id": "site-demo-1",
          "status": "active"
        },
        {
          "id": "emp-4",
          "full_name": "Козлова Мария Андреевна",
          "company_id": "comp-2",
          "company_name": "ООО ЭнергоСтрой",
          "position": "Инженер",
          "pass_number": "B-2002",
          "personnel_number": "TN-3002",
          "brigade_id": "br-2",
          "brigade_name": "Бригада №2",
          "site_id": "site-demo-1",
          "status": "active"
        },
        {
          "id": "emp-5",
          "full_name": "Николаев Дмитрий Владимирович",
          "company_id": "comp-1",
          "company_name": "ООО СтройМонтаж",
          "position": "Прораб",
          "pass_number": "A-1003",
          "personnel_number": "TN-2003",
          "brigade_id": "br-1",
          "brigade_name": "Бригада №1",
          "site_id": "site-demo-1",
          "status": "active"
        }
      ],
      "page": 1,
      "page_size": 200,
      "total_count": 5,
      "total_pages": 1
    }
    """.trimIndent()

    private fun devicesResponse() = """
    {
      "data": [
        {
          "device_id": "dev-watch-01",
          "serial_number": "GW8-SN-00101",
          "model": "Galaxy Watch 8",
          "status": "active",
          "charge_status": "charged",
          "employee_id": null,
          "employee_name": null,
          "site_id": "site-demo-1",
          "last_sync_at": null
        },
        {
          "device_id": "dev-watch-02",
          "serial_number": "GW8-SN-00102",
          "model": "Galaxy Watch 8",
          "status": "active",
          "charge_status": "charged",
          "employee_id": null,
          "employee_name": null,
          "site_id": "site-demo-1",
          "last_sync_at": null
        },
        {
          "device_id": "dev-watch-03",
          "serial_number": "GW8-SN-00103",
          "model": "Galaxy Watch 8",
          "status": "active",
          "charge_status": "low",
          "employee_id": null,
          "employee_name": null,
          "site_id": "site-demo-1",
          "last_sync_at": null
        },
        {
          "device_id": "dev-watch-04",
          "serial_number": "GW8-SN-00104",
          "model": "Galaxy Watch 8",
          "status": "active",
          "charge_status": "charged",
          "employee_id": "emp-1",
          "employee_name": "Иванов И.И.",
          "site_id": "site-demo-1",
          "last_sync_at": "2026-02-27T08:00:00Z"
        },
        {
          "device_id": "dev-watch-05",
          "serial_number": "GW8-SN-00105",
          "model": "Galaxy Watch 8",
          "status": "active",
          "charge_status": "charged",
          "employee_id": "emp-2",
          "employee_name": "Петров П.С.",
          "site_id": "site-demo-1",
          "last_sync_at": "2026-02-27T08:15:00Z"
        }
      ],
      "page": 1,
      "page_size": 100,
      "total_count": 5,
      "total_pages": 1
    }
    """.trimIndent()

    private fun sitesResponse() = """
    [
      {
        "id": "site-demo-1",
        "name": "Площадка Альфа",
        "address": "г. Москва, ул. Строителей, д. 15",
        "timezone": "Europe/Moscow",
        "status": "active"
      },
      {
        "id": "site-demo-2",
        "name": "Площадка Бета",
        "address": "г. Казань, пр. Победы, д. 42",
        "timezone": "Europe/Moscow",
        "status": "active"
      }
    ]
    """.trimIndent()

    private fun downtimeReasonsResponse() = """
    [
      {"id": "dr-1", "name": "Ожидание материалов"},
      {"id": "dr-2", "name": "Поломка оборудования"},
      {"id": "dr-3", "name": "Погодные условия"},
      {"id": "dr-4", "name": "Перерыв"},
      {"id": "dr-5", "name": "Инструктаж по ТБ"}
    ]
    """.trimIndent()

    // ── Binding responses ────────────────

    private fun createBindingResponse(): String {
        val id = bindingIdCounter.getAndIncrement()
        return """
        {
          "id": $id,
          "status": "active",
          "created_at": "2026-02-27T09:00:00Z"
        }
        """.trimIndent()
    }

    private fun closeBindingResponse() = """
    {
      "id": 100,
      "status": "closed"
    }
    """.trimIndent()

    private fun bindingsListResponse() = """
    [
      {
        "id": 1,
        "device_id": "dev-watch-04",
        "employee_id": "emp-1",
        "employee_name": "Иванов Иван Иванович",
        "site_id": "site-demo-1",
        "shift_date": "2026-02-27",
        "shift_type": "day",
        "bound_at": "2026-02-27T08:00:00Z",
        "unbound_at": null,
        "status": "active",
        "data_uploaded": false,
        "created_at": "2026-02-27T08:00:00Z"
      },
      {
        "id": 2,
        "device_id": "dev-watch-05",
        "employee_id": "emp-2",
        "employee_name": "Петров Пётр Сергеевич",
        "site_id": "site-demo-1",
        "shift_date": "2026-02-27",
        "shift_type": "day",
        "bound_at": "2026-02-27T08:15:00Z",
        "unbound_at": null,
        "status": "active",
        "data_uploaded": false,
        "created_at": "2026-02-27T08:15:00Z"
      }
    ]
    """.trimIndent()

    // ── Gateway response ─────────────────

    private fun uploadPacketResponse() = """
    {
      "packet_id": "demo-packet-id",
      "status": "accepted",
      "server_time": "2026-02-27T12:00:00Z"
    }
    """.trimIndent()
}
