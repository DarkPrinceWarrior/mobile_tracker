package com.example.mobile_tracker.presentation.maps

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.People
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.example.mobile_tracker.presentation.monitoring.MonitoringMapMode
import com.example.mobile_tracker.presentation.monitoring.MonitoringModeChip
import com.example.mobile_tracker.presentation.monitoring.MonitoringSiteMap
import com.example.mobile_tracker.presentation.monitoring.MonitoringZoneSummary
import com.example.mobile_tracker.presentation.monitoring.WorkerMonitoringSnapshot
import com.example.mobile_tracker.presentation.monitoring.WorkerStatusLegendRow
import com.example.mobile_tracker.presentation.monitoring.formatZoneLabel
import com.example.mobile_tracker.presentation.monitoring.workerStatusColor
import com.example.mobile_tracker.ui.theme.AppLayout
import com.example.mobile_tracker.ui.theme.AppRadius
import com.example.mobile_tracker.ui.theme.AppSpacing
import org.koin.androidx.compose.koinViewModel

@Composable
fun MapsScreen(
    onBack: (() -> Unit)? = null,
    onOpenWorkerDetail: (String) -> Unit = {},
    viewModel: MapsViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    AppScreenScaffold(
        snackbarMessage = state.error,
        topBar = {
            MTCompactTopBar(
                title = stringResource(R.string.maps_title),
                subtitle = stringResource(
                    R.string.maps_subtitle,
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
            state.workers.isEmpty() -> EmptyState(
                title = stringResource(R.string.maps_empty),
                icon = Icons.Default.Map,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
            )
            else -> LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = AppLayout.screenPadding, vertical = AppSpacing.sm),
                verticalArrangement = Arrangement.spacedBy(AppSpacing.md),
            ) {
                item {
                    MapsSummaryCard(state = state)
                }
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(AppSpacing.xs),
                    ) {
                        MonitoringModeChip(
                            label = stringResource(R.string.maps_mode_heatmap),
                            selected = state.mode == MonitoringMapMode.Heatmap,
                            onClick = {
                                viewModel.onIntent(
                                    MapsIntent.SetMode(MonitoringMapMode.Heatmap),
                                )
                            },
                        )
                        MonitoringModeChip(
                            label = stringResource(R.string.maps_mode_workers),
                            selected = state.mode == MonitoringMapMode.Workers,
                            onClick = {
                                viewModel.onIntent(
                                    MapsIntent.SetMode(MonitoringMapMode.Workers),
                                )
                            },
                        )
                    }
                }
                item {
                    MonitoringSiteMap(
                        workers = state.workersOnMap,
                        zoneSummaries = state.zoneSummaries,
                        mode = state.mode,
                        onWorkerClick = onOpenWorkerDetail,
                    )
                }
                item {
                    MTSectionHeader(
                        title = if (state.mode == MonitoringMapMode.Heatmap) {
                            stringResource(R.string.maps_legend_zones)
                        } else {
                            stringResource(R.string.maps_legend_workers)
                        },
                    )
                }
                if (state.mode == MonitoringMapMode.Heatmap) {
                    items(state.zoneSummaries, key = { it.zone.id }) { summary ->
                        MapsZoneRow(summary = summary)
                    }
                } else {
                    item {
                        WorkerStatusLegendRow()
                    }
                    if (state.workersOnMap.isEmpty()) {
                        item {
                            EmptyState(
                                title = stringResource(R.string.maps_workers_empty),
                                icon = Icons.Default.People,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    } else {
                        items(state.workersOnMap, key = { it.employeeId }) { worker ->
                            WorkerMapRow(
                                worker = worker,
                                onClick = { onOpenWorkerDetail(worker.employeeId) },
                            )
                        }
                    }
                }
                item {
                    Spacer(modifier = Modifier.height(AppSpacing.lg))
                }
            }
        }
    }
}

@Composable
private fun MapsSummaryCard(
    state: MapsState,
) {
    MTCard {
        Text(
            text = stringResource(R.string.maps_summary_title),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(AppSpacing.xs),
        ) {
            MapsMetricTile(
                modifier = Modifier.weight(1f),
                label = stringResource(R.string.monitoring_status_active),
                value = state.activeCount.toString(),
                color = workerStatusColor(com.example.mobile_tracker.presentation.monitoring.WorkerMonitoringStatus.Active),
            )
            MapsMetricTile(
                modifier = Modifier.weight(1f),
                label = stringResource(R.string.monitoring_status_idle),
                value = state.idleCount.toString(),
                color = workerStatusColor(com.example.mobile_tracker.presentation.monitoring.WorkerMonitoringStatus.Idle),
            )
            MapsMetricTile(
                modifier = Modifier.weight(1f),
                label = stringResource(R.string.monitoring_status_offline),
                value = state.offlineCount.toString(),
                color = workerStatusColor(com.example.mobile_tracker.presentation.monitoring.WorkerMonitoringStatus.Offline),
            )
        }
    }
}

@Composable
private fun MapsMetricTile(
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(AppRadius.lg),
        color = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                color = color,
            )
        }
    }
}

@Composable
private fun MapsZoneRow(
    summary: MonitoringZoneSummary,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(AppRadius.lg),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(AppLayout.cardPadding),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm),
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(zoneIndicatorColor(summary)),
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = formatZoneLabel(summary.zone.id),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = stringResource(
                        R.string.maps_zone_breakdown,
                        summary.activeWorkers,
                        summary.idleWorkers,
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                text = stringResource(R.string.maps_zone_workers, summary.totalWorkers),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun WorkerMapRow(
    worker: WorkerMonitoringSnapshot,
    onClick: () -> Unit,
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(AppRadius.lg),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(AppLayout.cardPadding),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm),
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(workerStatusColor(worker.status).copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier
                        .size(18.dp)
                        .clip(CircleShape)
                        .background(workerStatusColor(worker.status)),
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = worker.fullName,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = worker.zoneId?.let(::formatZoneLabel)
                        ?: stringResource(R.string.maps_zone_unknown),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun zoneIndicatorColor(summary: MonitoringZoneSummary): Color = when {
    summary.totalWorkers >= 5 -> Color(0xFFD2A232)
    summary.totalWorkers > 0 -> Color(0xFF2D9552)
    else -> MaterialTheme.colorScheme.outlineVariant
}

