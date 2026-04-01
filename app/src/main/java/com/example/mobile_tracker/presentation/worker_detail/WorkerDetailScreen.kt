package com.example.mobile_tracker.presentation.worker_detail

import androidx.compose.foundation.Image
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
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
            )
        }
    }
}

@Composable
private fun WorkerDetailContent(
    state: WorkerDetailState,
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
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = worker.fullName,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(AppSpacing.xs),
                ) {
                    if (worker.roleLabel.isNotBlank()) {
                        Text(
                            text = worker.roleLabel,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false),
                        )
                    }
                    WorkerMonitoringStatusBadge(status = worker.status)
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

    val now = remember { System.currentTimeMillis() }
    val shiftDurationMs = 12L * 60 * 60 * 1000
    val shiftEndAt = worker.shiftStartAt + shiftDurationMs
    val progress = ((now - worker.shiftStartAt).toFloat() / shiftDurationMs).coerceIn(0f, 1f)

    MTCard {
        Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.sm)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(horizontalAlignment = Alignment.Start) {
                    Text(
                        text = stringResource(R.string.worker_detail_shift_start),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = formatMonitoringTime(worker.shiftStartAt),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Сейчас",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.tertiary,
                    )
                    Text(
                        text = formatMonitoringTime(now),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.tertiary,
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Конец",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = formatMonitoringTime(shiftEndAt),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }

            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = MaterialTheme.colorScheme.tertiary,
                trackColor = MaterialTheme.colorScheme.surfaceContainer,
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(AppSpacing.xxs),
            ) {
                Icon(
                    imageVector = Icons.Default.Bolt,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.tertiary,
                )
                Text(
                    text = stringResource(R.string.worker_detail_shift_activity) +
                        ": " + formatMonitoringDuration(worker.activeDurationMinutes),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
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
                    .clip(RoundedCornerShape(AppRadius.lg)),
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ecg_panel),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFF08130F).copy(alpha = 0.12f)),
                )
                Column(
                    modifier = Modifier.padding(16.dp),
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
    }

}

@Composable
private fun WorkerWatchSection(
    worker: WorkerMonitoringSnapshot,
) {
    MTSectionHeader(title = stringResource(R.string.worker_detail_section_watch))

    if (worker.deviceId == null) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(AppRadius.lg),
            color = MaterialTheme.colorScheme.surfaceContainerLowest,
        ) {
            androidx.compose.foundation.layout.Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(AppLayout.cardPadding),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = stringResource(R.string.worker_detail_watch_missing),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        return
    }

    val batteryColor = if (worker.batteryPercent > 20) {
        workerStatusColor(Active)
    } else {
        workerStatusColor(WorkerMonitoringStatus.Idle)
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
            // Left: watch image + battery badge below
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(AppSpacing.xs),
            ) {
                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .clip(RoundedCornerShape(AppRadius.lg))
                        .background(MaterialTheme.colorScheme.surfaceContainer),
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.watch_render),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                    )
                }
                // Battery badge below the image
                Surface(
                    shape = RoundedCornerShape(AppRadius.pill),
                    color = batteryColor.copy(alpha = 0.15f),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Default.Bolt,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = batteryColor,
                        )
                        Text(
                            text = stringResource(
                                R.string.worker_detail_watch_battery_value,
                                worker.batteryPercent,
                            ),
                            style = MaterialTheme.typography.labelMedium,
                            color = batteryColor,
                        )
                    }
                }
            }

            // Right: model, wear status, issued chip
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(AppSpacing.xs),
            ) {
                Text(
                    text = worker.watchModel.orEmpty().ifBlank { "—" },
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = if (worker.watchOn) {
                        stringResource(R.string.worker_detail_watch_status_on)
                    } else {
                        stringResource(R.string.worker_detail_watch_status_off)
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = if (worker.watchOn) {
                        workerStatusColor(Active)
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
                WatchInfoChip(
                    label = stringResource(R.string.worker_detail_watch_issued),
                    value = worker.watchIssuedAt?.let { formatMonitoringTime(it) }
                        ?: stringResource(R.string.worker_detail_no_data_short),
                )
            }
        }
    }
}

@Composable
private fun WatchInfoChip(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
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
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
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
        val zoneSummaries = emptyList<com.example.mobile_tracker.presentation.monitoring.MonitoringZoneSummary>()
        com.example.mobile_tracker.presentation.monitoring.MonitoringSiteMap(
            workers = listOf(worker),
            zoneSummaries = zoneSummaries,
            mode = com.example.mobile_tracker.presentation.monitoring.MonitoringMapMode.Workers,
            highlightedWorkerId = worker.employeeId,
            route = worker.route,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

