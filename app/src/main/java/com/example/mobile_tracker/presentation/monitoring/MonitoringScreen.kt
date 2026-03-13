package com.example.mobile_tracker.presentation.monitoring

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.IconButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.mobile_tracker.R
import com.example.mobile_tracker.presentation.common.AppScreenScaffold
import com.example.mobile_tracker.presentation.common.EmptyState
import com.example.mobile_tracker.presentation.common.LoadingState
import com.example.mobile_tracker.presentation.common.StateCard
import org.koin.androidx.compose.koinViewModel

/** Accent color for the "Online" badge, as specified in the Figma design. */
private val OnlineAccentColor = Color(0xFF00A36A)

@Composable
fun MonitoringScreen(
    onBack: (() -> Unit)? = null,
    onNavigateToWorkers: () -> Unit = {},
    onNavigateToMaps: () -> Unit = {},
    onNavigateToAlerts: () -> Unit = {},
    onOpenWorkerDetail: (String) -> Unit = {},
    viewModel: MonitoringViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    AppScreenScaffold(
        snackbarMessage = state.error,
        bottomBar = {
            MonitoringBottomBar(
                current = MonitoringTab.Monitoring,
                onNavigateToMonitoring = {},
                onNavigateToWorkers = onNavigateToWorkers,
                onNavigateToMaps = onNavigateToMaps,
                onNavigateToAlerts = onNavigateToAlerts,
            )
        },
    ) { padding ->
        when {
            state.isLoading -> LoadingState(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
            )
            state.totalWorkers == 0 -> MonitoringEmptyPane(
                onBack = onBack,
                error = state.error,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
            )
            else -> MonitoringContent(
                state = state,
                error = state.error,
                onBack = onBack,
                onNavigateToMaps = onNavigateToMaps,
                onNavigateToWorkers = onNavigateToWorkers,
                onNavigateToAlerts = onNavigateToAlerts,
                onOpenWorkerDetail = onOpenWorkerDetail,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
            )
        }
    }
}

@Composable
private fun MonitoringContent(
    state: MonitoringState,
    error: String?,
    onBack: (() -> Unit)?,
    onNavigateToMaps: () -> Unit,
    onNavigateToWorkers: () -> Unit,
    onNavigateToAlerts: () -> Unit,
    onOpenWorkerDetail: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 8.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(30.dp),
    ) {
        MonitoringTitleRow(onBack = onBack)
        if (!error.isNullOrBlank()) {
            StateCard(message = error, isError = true)
        }
        ZoneHeaderCard(
            zoneLabel = state.topZoneLabel.ifBlank { stringResource(R.string.maps_zone_unknown) },
            shiftWindow = monitoringShiftWindow(state.shiftType),
            isOnline = state.activeCount > 0,
            onClick = onNavigateToMaps,
        )

        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            SectionLabel(text = stringResource(R.string.monitoring_stats_title))
            StatisticsPanel(
                efficiencyPercent = state.efficiencyPercent,
                activeCount = state.activeCount,
                idleCount = state.idleCount,
                offlineCount = state.offlineCount,
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            MonitoringSectionHeader(
                title = stringResource(R.string.monitoring_alerts_title),
                badge = stringResource(R.string.monitoring_alerts_timeframe),
                actionLabel = stringResource(R.string.monitoring_action_all),
                onActionClick = onNavigateToAlerts,
            )
            AlertsPanel(
                alerts = state.alertsPreview,
                onClick = onNavigateToAlerts,
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            MonitoringSectionHeader(
                title = stringResource(R.string.monitoring_workers_title),
                actionLabel = stringResource(R.string.monitoring_action_all),
                onActionClick = onNavigateToWorkers,
            )
            WorkersPanel(
                workers = state.activeWorkersPreview.take(3),
                onOpenWorkerDetail = onOpenWorkerDetail,
            )
        }

        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
private fun MonitoringEmptyPane(
    onBack: (() -> Unit)?,
    error: String?,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(horizontal = 8.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        MonitoringTitleRow(onBack = onBack)
        if (!error.isNullOrBlank()) {
            StateCard(message = error, isError = true)
        }
        EmptyState(
            title = stringResource(R.string.monitoring_empty),
            icon = Icons.Default.People,
            modifier = Modifier.fillMaxSize(),
        )
    }
}

@Composable
private fun MonitoringTitleRow(
    onBack: (() -> Unit)?,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (onBack != null) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.action_back),
                    tint = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
        Text(
            text = stringResource(R.string.monitoring_title),
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun ZoneHeaderCard(
    zoneLabel: String,
    shiftWindow: String,
    isOnline: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick),
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        shape = RoundedCornerShape(8.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = zoneLabel,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isOnline) {
                                        OnlineAccentColor
                                    } else {
                                        workerStatusColor(WorkerMonitoringStatus.Idle)
                                    },
                                ),
                        )
                        Text(
                            text = if (isOnline) {
                                stringResource(R.string.monitoring_badge_online)
                            } else {
                                stringResource(R.string.monitoring_badge_low_activity)
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (isOnline) {
                                OnlineAccentColor
                            } else {
                                workerStatusColor(WorkerMonitoringStatus.Idle)
                            },
                        )
                    }
                }
                Text(
                    text = shiftWindow,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f),
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f),
                modifier = Modifier.size(16.dp),
            )
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.82f),
    )
}

@Composable
private fun StatisticsPanel(
    efficiencyPercent: Int,
    activeCount: Int,
    idleCount: Int,
    offlineCount: Int,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            EfficiencyHeroCard(efficiencyPercent = efficiencyPercent)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                MonitoringSummaryTile(
                    modifier = Modifier.weight(1f),
                    value = activeCount.toString(),
                    label = stringResource(R.string.monitoring_stat_active),
                    dotColor = workerStatusColor(WorkerMonitoringStatus.Active),
                )
                MonitoringSummaryTile(
                    modifier = Modifier.weight(1f),
                    value = idleCount.toString(),
                    label = stringResource(R.string.monitoring_stat_idle),
                    dotColor = workerStatusColor(WorkerMonitoringStatus.Idle),
                )
                MonitoringSummaryTile(
                    modifier = Modifier.weight(1f),
                    value = offlineCount.toString(),
                    label = stringResource(R.string.monitoring_stat_offline),
                    dotColor = workerStatusColor(WorkerMonitoringStatus.Offline),
                )
            }
        }
    }
}

@Composable
private fun EfficiencyHeroCard(
    efficiencyPercent: Int,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF122C1E))
            .border(1.dp, Color(0x5500A36A), RoundedCornerShape(8.dp)),
    ) {
        // Inset glow — Figma: inset 0 0 30.6px #00A36A
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Color(0x3300A36A),
                            Color.Transparent,
                        ),
                    ),
                ),
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Column(
                modifier = Modifier.width(132.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                // Adjacent number above — 30% opacity, Figma: 70px
                Text(
                    text = (efficiencyPercent - 1).coerceAtLeast(0).toString(),
                    style = MaterialTheme.typography.displayLarge,
                    color = Color(0x4DAFFF69),
                )
                // Hero number — Figma: 86px
                Text(
                    text = efficiencyPercent.toString(),
                    style = MaterialTheme.typography.displayLarge.copy(
                        fontSize = 86.sp,
                        lineHeight = 86.sp,
                    ),
                    color = Color(0xFFAFFF69),
                )
                // Adjacent number below — 30% opacity, Figma: 70px
                Text(
                    text = (efficiencyPercent + 1).coerceAtMost(100).toString(),
                    style = MaterialTheme.typography.displayLarge,
                    color = Color(0x4DAFFF69),
                )
            }
            Text(
                text = stringResource(R.string.monitoring_efficiency_compact_label),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = Color(0xFFF9F9F9),
            )
        }
    }
}

@Composable
private fun MonitoringSummaryTile(
    value: String,
    label: String,
    dotColor: Color,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(dotColor),
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f),
                )
            }
        }
    }
}

@Composable
private fun MonitoringSectionHeader(
    title: String,
    actionLabel: String,
    onActionClick: () -> Unit,
    badge: String? = null,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SectionLabel(text = title)
            if (badge != null) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceContainer,
                ) {
                    Text(
                        text = badge,
                        modifier = Modifier.padding(horizontal = 6.dp),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f),
                    )
                }
            }
        }
        Text(
            text = actionLabel,
            modifier = Modifier.clickable(onClick = onActionClick),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = OnlineAccentColor,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AlertsPanel(
    alerts: List<MonitoringAlertPreview>,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
    ) {
        if (alerts.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
            ) {
                Text(
                    text = stringResource(R.string.monitoring_alerts_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 5.dp, top = 8.dp, end = 8.dp, bottom = 8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                alerts.forEach { alert ->
                    Card(
                        onClick = onClick,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.45f),
                        ),
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 8.dp, end = 12.dp, top = 8.dp, bottom = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(4.dp)
                                    .height(49.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(alertSeverityColor(alert.severity).copy(alpha = 0.5f)),
                            )
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                Text(
                                    text = alert.employeeName,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.82f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Text(
                                    text = alert.description,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f),
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                            // Alert time — matches Figma/TS design
                            Text(
                                text = formatMonitoringTime(alert.timestamp),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WorkersPanel(
    workers: List<WorkerMonitoringSnapshot>,
    onOpenWorkerDetail: (String) -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
    ) {
        if (workers.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
            ) {
                Text(
                    text = stringResource(R.string.monitoring_workers_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 5.dp, top = 8.dp, end = 8.dp, bottom = 8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                workers.forEach { worker ->
                    Card(
                        onClick = { onOpenWorkerDetail(worker.employeeId) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.45f),
                        ),
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Text(
                                text = buildAnnotatedString {
                                    append(worker.fullName)
                                    append(" ")
                                    withStyle(
                                        SpanStyle(
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f),
                                        ),
                                    ) {
                                        append(worker.roleLabel)
                                    }
                                },
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.82f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Favorite,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f),
                                        modifier = Modifier.size(16.dp),
                                    )
                                    Text(
                                        text = worker.heartRate?.toString()
                                            ?: stringResource(R.string.worker_detail_no_data_short),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f),
                                    )
                                }
                                Surface(
                                    shape = RoundedCornerShape(48.dp),
                                    color = Color(0x2B60D188),
                                ) {
                                    Text(
                                        text = stringResource(R.string.worker_detail_smr, worker.smrPercent),
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        style = MaterialTheme.typography.labelMedium,
                                        color = workerStatusColor(WorkerMonitoringStatus.Active),
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

// VerticalPreviewScrollbar removed — panels now wrap content dynamically.

private fun monitoringShiftWindow(shiftType: String): String = if (shiftType == "night") {
    "смена от 19:00 до 06:30"
} else {
    "смена от 7:00 до 18:30"
}

@Composable
private fun alertSeverityColor(severity: WorkerIncidentSeverity): Color = when (severity) {
    WorkerIncidentSeverity.Critical -> workerStatusColor(WorkerMonitoringStatus.Offline)
    WorkerIncidentSeverity.Warning -> workerStatusColor(WorkerMonitoringStatus.Idle)
    WorkerIncidentSeverity.Info -> workerStatusColor(WorkerMonitoringStatus.Active)
}
