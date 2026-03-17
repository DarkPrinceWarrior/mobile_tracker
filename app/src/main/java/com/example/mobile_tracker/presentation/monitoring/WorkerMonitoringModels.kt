package com.example.mobile_tracker.presentation.monitoring

import com.example.mobile_tracker.data.local.db.entity.BindingEntity
import com.example.mobile_tracker.data.local.db.entity.DeviceEntity
import com.example.mobile_tracker.data.local.db.entity.EmployeeEntity
import com.example.mobile_tracker.data.local.db.entity.OperationLogEntity
import com.example.mobile_tracker.data.local.db.entity.PacketQueueEntity
import com.example.mobile_tracker.util.formatTimestamp
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import kotlin.math.absoluteValue

data class MonitoringZoneDefinition(
    val id: String,
    val xRatio: Float,
    val yRatio: Float,
    val widthRatio: Float,
    val heightRatio: Float,
)

data class MonitoringZoneSummary(
    val zone: MonitoringZoneDefinition,
    val totalWorkers: Int,
    val activeWorkers: Int,
    val idleWorkers: Int,
)

enum class WorkerMonitoringStatus {
    Active,
    Idle,
    Offline,
}

enum class WorkerIncidentSeverity {
    Critical,
    Warning,
    Info,
}

enum class WorkerIncidentKind {
    PacketError,
    PacketPending,
    BindingUnsynced,
    UploadRequired,
    LowBattery,
    InactiveTooLong,
    WatchDisconnected,
    OperationError,
    OperationPending,
}

data class WorkerRouteVisit(
    val zoneId: String,
    val startAt: Long,
    val endAt: Long?,
    val current: Boolean,
)

data class WorkerIncident(
    val id: String,
    val kind: WorkerIncidentKind,
    val severity: WorkerIncidentSeverity,
    val timestamp: Long,
    val note: String? = null,
)

data class WorkerMonitoringSnapshot(
    val employeeId: String,
    val fullName: String,
    val roleLabel: String,
    val companyName: String?,
    val brigadeName: String?,
    val personnelNumber: String?,
    val passNumber: String?,
    val status: WorkerMonitoringStatus,
    val zoneId: String?,
    val mapXRatio: Float?,
    val mapYRatio: Float?,
    val shiftStartAt: Long,
    val activeDurationMinutes: Long,
    val heartRate: Int?,
    val temperatureCelsius: Float?,
    val steps: Int,
    val batteryPercent: Int,
    val watchOn: Boolean,
    val watchModel: String?,
    val deviceId: String?,
    val watchIssuedAt: Long?,
    val lastSeenAt: Long?,
    val smrPercent: Int,
    val efficiencyPercent: Int,
    val activeBinding: BindingEntity?,
    val device: DeviceEntity?,
    val route: List<WorkerRouteVisit>,
    val incidents: List<WorkerIncident>,
    val recentLogs: List<OperationLogEntity>,
)

val monitoringZones = listOf(
    MonitoringZoneDefinition("А1", 0.04f, 0.06f, 0.22f, 0.30f),
    MonitoringZoneDefinition("А2", 0.30f, 0.06f, 0.26f, 0.30f),
    MonitoringZoneDefinition("Б1", 0.04f, 0.42f, 0.22f, 0.28f),
    MonitoringZoneDefinition("Б2", 0.30f, 0.42f, 0.26f, 0.28f),
    MonitoringZoneDefinition("В1", 0.60f, 0.06f, 0.32f, 0.64f),
)

fun buildWorkerMonitoringSnapshots(
    employees: List<EmployeeEntity>,
    devices: List<DeviceEntity>,
    bindings: List<BindingEntity>,
    logs: List<OperationLogEntity>,
    packets: List<PacketQueueEntity>,
    shiftDate: String,
    shiftType: String,
    nowMillis: Long = System.currentTimeMillis(),
): List<WorkerMonitoringSnapshot> {
    val activeBindingsByEmployee = bindings
        .filter { it.status == "active" }
        .associateBy { it.employeeId }
    val devicesById = devices.associateBy { it.deviceId }
    val devicesByEmployee = devices
        .filter { !it.employeeId.isNullOrBlank() }
        .groupBy { it.employeeId.orEmpty() }
    val logsByEmployee = logs
        .filter { !it.employeeId.isNullOrBlank() }
        .groupBy { it.employeeId.orEmpty() }
    val logsByDevice = logs
        .filter { !it.deviceId.isNullOrBlank() }
        .groupBy { it.deviceId.orEmpty() }
    val packetsByEmployee = packets
        .filter { !it.employeeId.isNullOrBlank() }
        .groupBy { it.employeeId.orEmpty() }
    val packetsByDevice = packets.groupBy { it.deviceId }

    return employees
        .sortedBy { it.fullName }
        .map { employee ->
            val binding = activeBindingsByEmployee[employee.id]
            val device = binding?.let { devicesById[it.deviceId] }
                ?: devicesByEmployee[employee.id].orEmpty().firstOrNull()
            val employeeLogs = buildList {
                addAll(logsByEmployee[employee.id].orEmpty())
                if (device != null) {
                    addAll(logsByDevice[device.deviceId].orEmpty())
                }
            }
                .distinctBy { it.id }
                .sortedByDescending { it.createdAt }
            val employeePackets = buildList {
                addAll(packetsByEmployee[employee.id].orEmpty())
                if (device != null) {
                    addAll(packetsByDevice[device.deviceId].orEmpty())
                }
            }
                .distinctBy { it.packetId }
                .sortedByDescending { it.createdAt }

            buildWorkerMonitoringSnapshot(
                employee = employee,
                binding = binding,
                device = device,
                logs = employeeLogs,
                packets = employeePackets,
                shiftDate = shiftDate,
                shiftType = shiftType,
                nowMillis = nowMillis,
            )
        }
}

fun buildZoneSummaries(
    workers: List<WorkerMonitoringSnapshot>,
): List<MonitoringZoneSummary> {
    val workersByZone = workers
        .filter { it.zoneId != null && it.status != WorkerMonitoringStatus.Offline }
        .groupBy { it.zoneId.orEmpty() }

    return monitoringZones.map { zone ->
        val zoneWorkers = workersByZone[zone.id].orEmpty()
        MonitoringZoneSummary(
            zone = zone,
            totalWorkers = zoneWorkers.size,
            activeWorkers = zoneWorkers.count { it.status == WorkerMonitoringStatus.Active },
            idleWorkers = zoneWorkers.count { it.status == WorkerMonitoringStatus.Idle },
        )
    }
}

private fun buildWorkerMonitoringSnapshot(
    employee: EmployeeEntity,
    binding: BindingEntity?,
    device: DeviceEntity?,
    logs: List<OperationLogEntity>,
    packets: List<PacketQueueEntity>,
    shiftDate: String,
    shiftType: String,
    nowMillis: Long,
): WorkerMonitoringSnapshot {
    val hasIssuedWatch = binding != null && device != null
    val batteryPercent = device?.let {
        when (it.chargeStatus?.lowercase()) {
            "full", "charged", "charging" -> 95
            "high" -> 75
            "medium" -> 50
            "low" -> 20
            "critical" -> 5
            else -> null
        }
    } ?: 0
    // Статус из реального состояния привязки
    val status = when {
        !hasIssuedWatch -> WorkerMonitoringStatus.Offline
        packets.any { it.status == "error" } -> WorkerMonitoringStatus.Idle
        else -> WorkerMonitoringStatus.Active
    }
    val watchOn = hasIssuedWatch && status != WorkerMonitoringStatus.Offline
    // Зона и позиция — только из реальных данных бэкенда (через enrichment)
    val zone: MonitoringZoneDefinition? = null
    val mapPosition: Pair<Float, Float>? = null
    val shiftStartAt = binding?.boundAt ?: nowMillis
    val shiftWindowMinutes = ((nowMillis - shiftStartAt).coerceAtLeast(0L) / 60_000L)
        .coerceAtMost(12 * 60L)
    // Время на объекте — реальное от момента привязки
    val activeDurationMinutes = if (hasIssuedWatch) shiftWindowMinutes else 0L
    val lastSeenAt: Long? = null  // Будет обогащено бэкендом
    // Метрики — только из бэкенда, без seed-генерации
    val heartRate: Int? = null
    val temperature: Float? = null
    val steps = 0
    val smrPercent = 0
    val efficiencyPercent = 0

    return WorkerMonitoringSnapshot(
        employeeId = employee.id,
        fullName = employee.fullName,
        roleLabel = buildRoleLabel(employee),
        companyName = employee.companyName,
        brigadeName = employee.brigadeName,
        personnelNumber = employee.personnelNumber,
        passNumber = employee.passNumber,
        status = status,
        zoneId = zone?.id,
        mapXRatio = mapPosition?.first,
        mapYRatio = mapPosition?.second,
        shiftStartAt = shiftStartAt,
        activeDurationMinutes = activeDurationMinutes,
        heartRate = heartRate,
        temperatureCelsius = temperature,
        steps = steps,
        batteryPercent = batteryPercent,
        watchOn = watchOn,
        watchModel = device?.model,
        deviceId = device?.deviceId ?: binding?.deviceId,
        watchIssuedAt = binding?.boundAt,
        lastSeenAt = lastSeenAt,
        smrPercent = smrPercent,
        efficiencyPercent = efficiencyPercent,
        activeBinding = binding,
        device = device,
        route = emptyList(),  // Маршрут — только из бэкенда
        incidents = buildIncidents(
            binding = binding,
            batteryPercent = batteryPercent,
            watchOn = watchOn,
            status = status,
            logs = logs,
            packets = packets,
            lastSeenAt = lastSeenAt,
        ),
        recentLogs = logs.take(4),
    )
}

private fun buildRoute(
    currentZone: MonitoringZoneDefinition?,
    shiftStartAt: Long,
    nowMillis: Long,
    seed: Int,
    canTrack: Boolean,
): List<WorkerRouteVisit> {
    if (!canTrack || currentZone == null) {
        return emptyList()
    }

    val currentIndex = monitoringZones.indexOfFirst { it.id == currentZone.id }
    val visitCount = 3 + (seed % 2)
    val totalWindow = (nowMillis - shiftStartAt).coerceAtLeast(90L * 60_000L)
    val segmentWindow = (totalWindow / visitCount).coerceAtLeast(25L * 60_000L)
    val startCursor = nowMillis - (segmentWindow * visitCount)

    return (0 until visitCount).map { index ->
        val zoneIndex = (currentIndex - (visitCount - 1 - index) + monitoringZones.size * 4) %
            monitoringZones.size
        val visitStart = startCursor + (segmentWindow * index)
        val isCurrent = index == visitCount - 1
        WorkerRouteVisit(
            zoneId = monitoringZones[zoneIndex].id,
            startAt = visitStart,
            endAt = if (isCurrent) null else visitStart + segmentWindow,
            current = isCurrent,
        )
    }
}

private fun buildIncidents(
    binding: BindingEntity?,
    batteryPercent: Int,
    watchOn: Boolean,
    status: WorkerMonitoringStatus,
    logs: List<OperationLogEntity>,
    packets: List<PacketQueueEntity>,
    lastSeenAt: Long?,
): List<WorkerIncident> {
    val incidents = buildList {
        packets
            .filter { it.status == "error" }
            .forEach { packet ->
                add(
                    WorkerIncident(
                        id = "packet-error-${packet.packetId}",
                        kind = WorkerIncidentKind.PacketError,
                        severity = WorkerIncidentSeverity.Critical,
                        timestamp = packet.createdAt,
                        note = packet.lastError ?: packet.serverStatus,
                    ),
                )
            }

        packets
            .filter { it.status == "pending" }
            .forEach { packet ->
                add(
                    WorkerIncident(
                        id = "packet-pending-${packet.packetId}",
                        kind = WorkerIncidentKind.PacketPending,
                        severity = WorkerIncidentSeverity.Warning,
                        timestamp = packet.createdAt,
                    ),
                )
            }

        if (binding != null && !binding.isSynced) {
            add(
                WorkerIncident(
                    id = "binding-sync-${binding.id}",
                    kind = WorkerIncidentKind.BindingUnsynced,
                    severity = WorkerIncidentSeverity.Warning,
                    timestamp = binding.createdAt,
                ),
            )
        }

        if (binding != null && !binding.dataUploaded) {
            add(
                WorkerIncident(
                    id = "binding-upload-${binding.id}",
                    kind = WorkerIncidentKind.UploadRequired,
                    severity = WorkerIncidentSeverity.Info,
                    timestamp = binding.boundAt,
                ),
            )
        }

        if (binding != null && batteryPercent in 1..20) {
            add(
                WorkerIncident(
                    id = "battery-${binding.id}",
                    kind = WorkerIncidentKind.LowBattery,
                    severity = if (batteryPercent <= 10) {
                        WorkerIncidentSeverity.Warning
                    } else {
                        WorkerIncidentSeverity.Info
                    },
                    timestamp = lastSeenAt ?: binding.boundAt,
                ),
            )
        }

        if (binding != null && !watchOn) {
            add(
                WorkerIncident(
                    id = "watch-${binding.id}",
                    kind = WorkerIncidentKind.WatchDisconnected,
                    severity = WorkerIncidentSeverity.Critical,
                    timestamp = lastSeenAt ?: binding.boundAt,
                ),
            )
        }

        if (binding != null && status == WorkerMonitoringStatus.Idle) {
            add(
                WorkerIncident(
                    id = "idle-${binding.id}",
                    kind = WorkerIncidentKind.InactiveTooLong,
                    severity = WorkerIncidentSeverity.Warning,
                    timestamp = lastSeenAt ?: binding.boundAt,
                ),
            )
        }

        logs
            .filter { it.status == "error" }
            .forEach { log ->
                add(
                    WorkerIncident(
                        id = "log-error-${log.id}",
                        kind = WorkerIncidentKind.OperationError,
                        severity = WorkerIncidentSeverity.Critical,
                        timestamp = log.createdAt,
                        note = log.errorMessage ?: log.details,
                    ),
                )
            }

        logs
            .filter { it.status == "pending" }
            .forEach { log ->
                add(
                    WorkerIncident(
                        id = "log-pending-${log.id}",
                        kind = WorkerIncidentKind.OperationPending,
                        severity = WorkerIncidentSeverity.Warning,
                        timestamp = log.createdAt,
                        note = log.details,
                    ),
                )
            }
    }

    return incidents
        .sortedByDescending { it.timestamp }
        .distinctBy { incident -> incident.kind to incident.note }
        .take(5)
}

private fun buildRoleLabel(employee: EmployeeEntity): String = listOfNotNull(
    employee.position,
    employee.brigadeName,
    employee.companyName,
).firstOrNull().orEmpty().ifBlank {
    employee.status.replaceFirstChar { it.uppercase() }
}

private fun fallbackWatchModel(
    seed: Int,
    hasIssuedWatch: Boolean,
): String? {
    if (!hasIssuedWatch) {
        return null
    }

    return when (seed % 3) {
        0 -> "SmartSite Pro X1"
        1 -> "ConstructWatch 2"
        else -> "SmartSite Lite"
    }
}

private fun deriveBatteryPercent(
    device: DeviceEntity?,
    seed: Int,
    hasIssuedWatch: Boolean,
): Int {
    if (!hasIssuedWatch) {
        return 0
    }

    return when (device?.chargeStatus?.lowercase()) {
        "full", "charged", "charging" -> 86 + (seed % 12)
        "high" -> 68 + (seed % 18)
        "medium" -> 44 + (seed % 16)
        "low" -> 14 + (seed % 12)
        "critical" -> 3 + (seed % 5)
        else -> 28 + (seed % 52)
    }.coerceIn(0, 100)
}

private fun deriveStatus(
    hasIssuedWatch: Boolean,
    batteryPercent: Int,
    packets: List<PacketQueueEntity>,
    logs: List<OperationLogEntity>,
    seed: Int,
): WorkerMonitoringStatus {
    if (!hasIssuedWatch || batteryPercent <= 6) {
        return WorkerMonitoringStatus.Offline
    }

    if (packets.any { it.status == "error" }) {
        return if (batteryPercent <= 18) {
            WorkerMonitoringStatus.Offline
        } else {
            WorkerMonitoringStatus.Idle
        }
    }

    if (logs.any { it.status == "error" }) {
        return if (seed % 2 == 0) {
            WorkerMonitoringStatus.Idle
        } else {
            WorkerMonitoringStatus.Active
        }
    }

    return when {
        batteryPercent <= 24 -> WorkerMonitoringStatus.Idle
        seed % 5 == 0 -> WorkerMonitoringStatus.Idle
        else -> WorkerMonitoringStatus.Active
    }
}

private fun deriveLastSeenAt(
    status: WorkerMonitoringStatus,
    nowMillis: Long,
    seed: Int,
): Long = when (status) {
    WorkerMonitoringStatus.Active -> nowMillis - ((1 + (seed % 5)) * 60_000L)
    WorkerMonitoringStatus.Idle -> nowMillis - ((8 + (seed % 18)) * 60_000L)
    WorkerMonitoringStatus.Offline -> nowMillis - ((45 + (seed % 135)) * 60_000L)
}

private fun defaultShiftStartAt(
    shiftDate: String,
    shiftType: String,
    seed: Int,
    zoneId: ZoneId = ZoneId.systemDefault(),
): Long {
    val localDate = runCatching { LocalDate.parse(shiftDate) }
        .getOrElse { LocalDate.now(zoneId) }
    val baseTime = if (shiftType == "night") {
        LocalTime.of(19, 0)
    } else {
        LocalTime.of(7, 0)
    }
    val minuteOffset = (seed % 4) * 15
    val dateTime = LocalDateTime.of(localDate, baseTime.plusMinutes(minuteOffset.toLong()))
    return dateTime.atZone(zoneId).toInstant().toEpochMilli()
}

private fun stableSeed(raw: String): Int = raw.hashCode().absoluteValue

fun formatMonitoringDuration(minutes: Long): String {
    val safeMinutes = minutes.coerceAtLeast(0L)
    val hours = safeMinutes / 60L
    val remainingMinutes = safeMinutes % 60L
    return "${hours}ч ${remainingMinutes} мин"
}

fun formatZoneLabel(zoneId: String): String = "Зона $zoneId"

fun formatMonitoringTime(timestamp: Long?): String = timestamp?.let {
    formatTimestamp(it, pattern = "HH:mm")
}.orEmpty()

