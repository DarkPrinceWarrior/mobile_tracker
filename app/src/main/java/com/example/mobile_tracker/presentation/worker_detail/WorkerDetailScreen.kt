package com.example.mobile_tracker.presentation.worker_detail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.DeviceUnknown
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.Watch
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.mobile_tracker.R
import com.example.mobile_tracker.presentation.common.AppScreenScaffold
import com.example.mobile_tracker.presentation.common.EmptyState
import com.example.mobile_tracker.presentation.common.LoadingState
import com.example.mobile_tracker.presentation.common.MTCard
import com.example.mobile_tracker.presentation.common.MTCompactTopBar
import com.example.mobile_tracker.presentation.common.MTSectionHeader
import com.example.mobile_tracker.presentation.monitoring.WorkerMonitoringSnapshot
import com.example.mobile_tracker.presentation.monitoring.WorkerMonitoringStatus
import com.example.mobile_tracker.presentation.monitoring.WorkerMonitoringStatus.Active
import com.example.mobile_tracker.presentation.monitoring.WorkerMonitoringStatusBadge
import com.example.mobile_tracker.presentation.monitoring.formatMonitoringDuration
import com.example.mobile_tracker.presentation.monitoring.formatMonitoringTime
import com.example.mobile_tracker.presentation.monitoring.workerStatusColor
import com.example.mobile_tracker.ui.theme.AppLayout
import com.example.mobile_tracker.ui.theme.AppRadius
import com.example.mobile_tracker.ui.theme.AppSpacing
import org.koin.androidx.compose.koinViewModel

@Composable
fun WorkerDetailScreen(
    employeeId: String,
    onBack: (() -> Unit)? = null,
    viewModel: WorkerDetailViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(employeeId) {
        viewModel.bind(employeeId)
    }

    AppScreenScaffold(
        snackbarMessage = state.error,
        topBar = {
            MTCompactTopBar(
                title = stringResource(R.string.worker_detail_title),
                subtitle = stringResource(
                    R.string.worker_detail_subtitle,
                    state.siteName,
                    state.shiftDate,
                ),
                navigationIcon = {
                    if (onBack != null) {
                        IconButton(onClick = onBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.action_back),
                                tint = MaterialTheme.colorScheme.onSurface,
                            )
                        }
                    }
                },
            )
        },
    ) { padding ->
        when {
            state.isLoading -> LoadingState(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
            )
            state.worker == null -> EmptyState(
                title = stringResource(R.string.worker_detail_missing),
                icon = Icons.Default.Person,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
            )
            else -> WorkerDetailContent(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                state = state,
                onAcknowledgeIncident = {
                    viewModel.onIntent(WorkerDetailIntent.AcknowledgeIncident(it))
                },
            )
        }
    }
}

@Composable
private fun WorkerDetailContent(
    state: WorkerDetailState,
    onAcknowledgeIncident: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val worker = state.worker ?: return

    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = AppLayout.screenPadding, vertical = AppSpacing.sm),
        verticalArrangement = Arrangement.spacedBy(AppSpacing.md),
    ) {
        WorkerHeaderCard(worker = worker)
        WorkerShiftInfoSection(worker = worker)
        WorkerVitalsSection(worker = worker)
        WorkerWatchSection(worker = worker)
        WorkerRouteSection(worker = worker)
        WorkerIncidentsSection(
            state = state,
            onAcknowledgeIncident = onAcknowledgeIncident,
        )
        Spacer(modifier = Modifier.height(AppSpacing.lg))
    }
}

@Composable
private fun WorkerHeaderCard(
    worker: WorkerMonitoringSnapshot,
) {
    MTCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm),
        ) {
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.55f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.tertiary,
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = worker.fullName,
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = worker.roleLabel,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(AppSpacing.xs),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    WorkerMonitoringStatusBadge(status = worker.status)
                    Surface(
                        shape = RoundedCornerShape(AppRadius.pill),
                        color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.45f),
                    ) {
                        Text(
                            text = stringResource(
                                R.string.worker_detail_smr,
                                worker.smrPercent,
                            ),
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.tertiary,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun WorkerShiftInfoSection(
    worker: WorkerMonitoringSnapshot,
) {
    MTSectionHeader(title = stringResource(R.string.worker_detail_section_shift))
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(AppSpacing.xs),
    ) {
        WorkerValueTile(
            modifier = Modifier.weight(1f),
            label = stringResource(R.string.worker_detail_shift_start),
            value = formatMonitoringTime(worker.shiftStartAt),
        )
        WorkerValueTile(
            modifier = Modifier.weight(1f),
            label = stringResource(R.string.worker_detail_shift_activity),
            value = formatMonitoringDuration(worker.activeDurationMinutes),
        )
    }
}

@Composable
private fun WorkerVitalsSection(
    worker: WorkerMonitoringSnapshot,
) {
    MTSectionHeader(title = stringResource(R.string.worker_detail_section_stats))

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(AppSpacing.xs),
        verticalAlignment = Alignment.Top,
    ) {
        Card(
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(AppRadius.lg),
            colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(152.dp)
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                Color(0xFF08130F),
                                Color(0xFF0F2C1A),
                            ),
                        ),
                    )
                    .padding(16.dp),
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(AppSpacing.xs),
                ) {
                    Icon(
                        imageVector = Icons.Default.Favorite,
                        contentDescription = null,
                        tint = Color(0xFFAFFF69),
                    )
                    Text(
                        text = worker.heartRate?.toString()
                            ?: stringResource(R.string.worker_detail_no_data_short),
                        style = MaterialTheme.typography.displaySmall,
                        color = Color(0xFFAFFF69),
                    )
                    Text(
                        text = stringResource(R.string.worker_detail_heart_rate_unit),
                        style = MaterialTheme.typography.labelLarge,
                        color = Color(0xB3EAFFE8),
                    )
                }
            }
        }

        Card(
            modifier = Modifier.weight(0.52f),
            shape = RoundedCornerShape(AppRadius.lg),
            colors = CardDefaults.cardColors(
                containerColor = if (worker.temperatureCelsius != null) {
                    Color(0xFFF58D43)
                } else {
                    MaterialTheme.colorScheme.surfaceContainer
                },
            ),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(152.dp)
                    .padding(14.dp),
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = if (worker.temperatureCelsius != null) {
                        stringResource(R.string.worker_detail_temperature_normal)
                    } else {
                        stringResource(R.string.worker_detail_temperature_no_data)
                    },
                    style = MaterialTheme.typography.titleSmall,
                    color = if (worker.temperatureCelsius != null) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(AppSpacing.xxs),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Default.Thermostat,
                        contentDescription = null,
                        tint = if (worker.temperatureCelsius != null) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = worker.temperatureCelsius?.let { String.format("%.1f", it) }
                            ?: stringResource(R.string.worker_detail_no_data_short),
                        style = MaterialTheme.typography.headlineSmall,
                        color = if (worker.temperatureCelsius != null) Color.White else MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = stringResource(R.string.worker_detail_temperature_unit),
                        style = MaterialTheme.typography.labelMedium,
                        color = if (worker.temperatureCelsius != null) Color.White.copy(alpha = 0.82f) else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }

    Surface(
        shape = RoundedCornerShape(AppRadius.lg),
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(AppSpacing.xs),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.DirectionsWalk,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = worker.steps.toString(),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = stringResource(R.string.worker_detail_steps_unit),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun WorkerWatchSection(
    worker: WorkerMonitoringSnapshot,
) {
    MTSectionHeader(title = stringResource(R.string.worker_detail_section_watch))

    if (worker.deviceId == null) {
        EmptyState(
            title = stringResource(R.string.worker_detail_watch_missing),
            icon = Icons.Default.DeviceUnknown,
            modifier = Modifier.fillMaxWidth(),
        )
        return
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(AppRadius.xl),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(AppLayout.cardPadding),
            horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm),
            verticalAlignment = Alignment.Top,
        ) {
            Surface(
                modifier = Modifier.size(128.dp),
                shape = RoundedCornerShape(AppRadius.lg),
                color = MaterialTheme.colorScheme.surfaceContainer,
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceBetween,
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.45f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Default.Watch,
                            contentDescription = null,
                            modifier = Modifier.size(32.dp),
                            tint = MaterialTheme.colorScheme.tertiary,
                        )
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(AppSpacing.xxs),
                    ) {
                        Icon(
                            imageVector = Icons.Default.Bolt,
                            contentDescription = null,
                            tint = if (worker.batteryPercent > 20) {
                                workerStatusColor(Active)
                            } else {
                                workerStatusColor(WorkerMonitoringStatus.Idle)
                            },
                        )
                        Text(
                            text = stringResource(
                                R.string.worker_detail_watch_battery_value,
                                worker.batteryPercent,
                            ),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(AppSpacing.xs),
            ) {
                WorkerInfoRow(
                    label = stringResource(R.string.worker_detail_watch_device),
                    value = worker.watchModel.orEmpty(),
                )
                WorkerInfoRow(
                    label = stringResource(R.string.worker_detail_watch_issued),
                    value = worker.watchIssuedAt?.let {
                        formatMonitoringTime(it)
                    } ?: stringResource(R.string.worker_detail_no_data_short),
                )
                WorkerInfoRow(
                    label = stringResource(R.string.worker_detail_watch_last_seen),
                    value = relativeSeenLabel(worker.lastSeenAt),
                )
                WorkerInfoRow(
                    label = stringResource(R.string.worker_detail_watch_status),
                    value = if (worker.watchOn) {
                        stringResource(R.string.worker_detail_watch_status_on)
                    } else {
                        stringResource(R.string.worker_detail_watch_status_off)
                    },
                )
            }
        }
    }
}

@Composable
private fun WorkerRouteSection(
    worker: WorkerMonitoringSnapshot,
) {
    MTSectionHeader(title = stringResource(R.string.worker_detail_section_route))

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(AppRadius.xl),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
        ),
    ) {
        Column(
            modifier = Modifier.padding(AppLayout.cardPadding),
            verticalArrangement = Arrangement.spacedBy(AppSpacing.sm),
        ) {
            val zoneSummaries = com.example.mobile_tracker.presentation.monitoring.buildZoneSummaries(
                listOf(worker),
            )
            com.example.mobile_tracker.presentation.monitoring.MonitoringSiteMap(
                workers = listOf(worker),
                zoneSummaries = zoneSummaries,
                mode = com.example.mobile_tracker.presentation.monitoring.MonitoringMapMode.Workers,
                highlightedWorkerId = worker.employeeId,
                modifier = Modifier.fillMaxWidth(),
            )

            if (worker.route.isEmpty()) {
                EmptyState(
                    title = stringResource(R.string.worker_detail_route_empty),
                    icon = Icons.Default.Info,
                    modifier = Modifier.fillMaxWidth(),
                )
            } else {
                worker.route.forEach { visit ->
                    Surface(
                        shape = RoundedCornerShape(AppRadius.lg),
                        color = if (visit.current) {
                            MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.22f)
                        } else {
                            MaterialTheme.colorScheme.surfaceContainer
                        },
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm),
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(12.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (visit.current) {
                                            workerStatusColor(Active)
                                        } else {
                                            MaterialTheme.colorScheme.outlineVariant
                                        },
                                    ),
                            )
                            Text(
                                text = com.example.mobile_tracker.presentation.monitoring.formatZoneLabel(
                                    visit.zoneId,
                                ),
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Text(
                                text = buildString {
                                    append(formatMonitoringTime(visit.startAt))
                                    visit.endAt?.let {
                                        append(" - ")
                                        append(formatMonitoringTime(it))
                                    }
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            if (visit.current) {
                                Surface(
                                    shape = RoundedCornerShape(AppRadius.pill),
                                    color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.14f),
                                ) {
                                    Text(
                                        text = stringResource(R.string.worker_detail_route_current),
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.tertiary,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WorkerIncidentsSection(
    state: WorkerDetailState,
    onAcknowledgeIncident: (String) -> Unit,
) {
    val incidents = state.worker?.incidents.orEmpty()

    MTSectionHeader(title = stringResource(R.string.worker_detail_section_alerts))

    if (incidents.isEmpty()) {
        EmptyState(
            title = stringResource(R.string.worker_detail_alerts_empty),
            icon = Icons.Default.Info,
            modifier = Modifier.fillMaxWidth(),
        )
        return
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(AppSpacing.xs),
    ) {
        incidents.forEach { incident ->
            val acknowledged = state.isAcknowledged(incident)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(AppRadius.lg),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLowest.copy(
                        alpha = if (acknowledged) 0.62f else 1f,
                    ),
                ),
            ) {
                Column(
                    modifier = Modifier.padding(AppLayout.cardPadding),
                    verticalArrangement = Arrangement.spacedBy(AppSpacing.sm),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm),
                        verticalAlignment = Alignment.Top,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(46.dp)
                                .clip(CircleShape)
                                .background(incidentColor(incident.severity).copy(alpha = 0.14f)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = incidentIcon(incident.kind),
                                contentDescription = null,
                                tint = incidentColor(incident.severity),
                            )
                        }
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(2.dp),
                        ) {
                            Text(
                                text = incidentTitle(incident.kind),
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Text(
                                text = incident.note ?: incidentFallbackDescription(incident.kind),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = formatMonitoringTime(incident.timestamp),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        androidx.compose.material3.Button(
                            onClick = { onAcknowledgeIncident(incident.id) },
                            enabled = !acknowledged,
                            shape = RoundedCornerShape(AppRadius.xl),
                            colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary,
                                disabledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                                disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            ),
                        ) {
                            Text(
                                text = if (acknowledged) {
                                    stringResource(R.string.worker_detail_alert_accepted)
                                } else {
                                    stringResource(R.string.worker_detail_alert_acknowledge)
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WorkerValueTile(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(AppRadius.lg),
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@Composable
private fun WorkerInfoRow(
    label: String,
    value: String,
) {
    Surface(
        shape = RoundedCornerShape(AppRadius.lg),
        color = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun incidentTitle(kind: com.example.mobile_tracker.presentation.monitoring.WorkerIncidentKind): String = when (kind) {
    com.example.mobile_tracker.presentation.monitoring.WorkerIncidentKind.PacketError -> stringResource(R.string.worker_detail_incident_packet_error)
    com.example.mobile_tracker.presentation.monitoring.WorkerIncidentKind.PacketPending -> stringResource(R.string.worker_detail_incident_packet_pending)
    com.example.mobile_tracker.presentation.monitoring.WorkerIncidentKind.BindingUnsynced -> stringResource(R.string.worker_detail_incident_binding_unsynced)
    com.example.mobile_tracker.presentation.monitoring.WorkerIncidentKind.UploadRequired -> stringResource(R.string.worker_detail_incident_upload_required)
    com.example.mobile_tracker.presentation.monitoring.WorkerIncidentKind.LowBattery -> stringResource(R.string.worker_detail_incident_low_battery)
    com.example.mobile_tracker.presentation.monitoring.WorkerIncidentKind.InactiveTooLong -> stringResource(R.string.worker_detail_incident_inactive)
    com.example.mobile_tracker.presentation.monitoring.WorkerIncidentKind.WatchDisconnected -> stringResource(R.string.worker_detail_incident_watch_off)
    com.example.mobile_tracker.presentation.monitoring.WorkerIncidentKind.OperationError -> stringResource(R.string.worker_detail_incident_operation_error)
    com.example.mobile_tracker.presentation.monitoring.WorkerIncidentKind.OperationPending -> stringResource(R.string.worker_detail_incident_operation_pending)
}

@Composable
private fun incidentFallbackDescription(kind: com.example.mobile_tracker.presentation.monitoring.WorkerIncidentKind): String = when (kind) {
    com.example.mobile_tracker.presentation.monitoring.WorkerIncidentKind.PacketError -> stringResource(R.string.worker_detail_incident_packet_error_desc)
    com.example.mobile_tracker.presentation.monitoring.WorkerIncidentKind.PacketPending -> stringResource(R.string.worker_detail_incident_packet_pending_desc)
    com.example.mobile_tracker.presentation.monitoring.WorkerIncidentKind.BindingUnsynced -> stringResource(R.string.worker_detail_incident_binding_unsynced_desc)
    com.example.mobile_tracker.presentation.monitoring.WorkerIncidentKind.UploadRequired -> stringResource(R.string.worker_detail_incident_upload_required_desc)
    com.example.mobile_tracker.presentation.monitoring.WorkerIncidentKind.LowBattery -> stringResource(R.string.worker_detail_incident_low_battery_desc)
    com.example.mobile_tracker.presentation.monitoring.WorkerIncidentKind.InactiveTooLong -> stringResource(R.string.worker_detail_incident_inactive_desc)
    com.example.mobile_tracker.presentation.monitoring.WorkerIncidentKind.WatchDisconnected -> stringResource(R.string.worker_detail_incident_watch_off_desc)
    com.example.mobile_tracker.presentation.monitoring.WorkerIncidentKind.OperationError -> stringResource(R.string.worker_detail_incident_operation_error_desc)
    com.example.mobile_tracker.presentation.monitoring.WorkerIncidentKind.OperationPending -> stringResource(R.string.worker_detail_incident_operation_pending_desc)
}

@Composable
private fun incidentColor(
    severity: com.example.mobile_tracker.presentation.monitoring.WorkerIncidentSeverity,
): Color = when (severity) {
    com.example.mobile_tracker.presentation.monitoring.WorkerIncidentSeverity.Critical -> workerStatusColor(WorkerMonitoringStatus.Offline)
    com.example.mobile_tracker.presentation.monitoring.WorkerIncidentSeverity.Warning -> workerStatusColor(WorkerMonitoringStatus.Idle)
    com.example.mobile_tracker.presentation.monitoring.WorkerIncidentSeverity.Info -> workerStatusColor(Active)
}

private fun incidentIcon(
    kind: com.example.mobile_tracker.presentation.monitoring.WorkerIncidentKind,
): androidx.compose.ui.graphics.vector.ImageVector = when (kind) {
    com.example.mobile_tracker.presentation.monitoring.WorkerIncidentKind.PacketError -> Icons.Default.Bolt
    com.example.mobile_tracker.presentation.monitoring.WorkerIncidentKind.PacketPending -> Icons.Default.Info
    com.example.mobile_tracker.presentation.monitoring.WorkerIncidentKind.BindingUnsynced -> Icons.Default.Info
    com.example.mobile_tracker.presentation.monitoring.WorkerIncidentKind.UploadRequired -> Icons.Default.Watch
    com.example.mobile_tracker.presentation.monitoring.WorkerIncidentKind.LowBattery -> Icons.Default.Bolt
    com.example.mobile_tracker.presentation.monitoring.WorkerIncidentKind.InactiveTooLong -> Icons.AutoMirrored.Filled.DirectionsWalk
    com.example.mobile_tracker.presentation.monitoring.WorkerIncidentKind.WatchDisconnected -> Icons.Default.Watch
    com.example.mobile_tracker.presentation.monitoring.WorkerIncidentKind.OperationError -> Icons.Default.Info
    com.example.mobile_tracker.presentation.monitoring.WorkerIncidentKind.OperationPending -> Icons.Default.Info
}

@Composable
private fun relativeSeenLabel(timestamp: Long?): String {
    if (timestamp == null) {
        return stringResource(R.string.worker_detail_temperature_no_data)
    }
    val minutes = ((System.currentTimeMillis() - timestamp) / 60_000L).coerceAtLeast(0L)
    return when {
        minutes < 1L -> stringResource(R.string.worker_detail_seen_now)
        minutes < 60L -> stringResource(R.string.worker_detail_seen_minutes, minutes)
        else -> stringResource(R.string.worker_detail_seen_hours, minutes / 60L)
    }
}
