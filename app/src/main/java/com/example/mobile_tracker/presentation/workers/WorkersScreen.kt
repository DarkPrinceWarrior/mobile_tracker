package com.example.mobile_tracker.presentation.workers

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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.mobile_tracker.R
import com.example.mobile_tracker.presentation.common.AppScreenScaffold
import com.example.mobile_tracker.presentation.common.EmptyState
import com.example.mobile_tracker.presentation.common.LoadingState
import com.example.mobile_tracker.presentation.common.MTCompactTopBar
import com.example.mobile_tracker.presentation.common.StateCard
import com.example.mobile_tracker.presentation.monitoring.WorkerMonitoringSnapshot
import com.example.mobile_tracker.presentation.monitoring.WorkerMonitoringStatus
import com.example.mobile_tracker.presentation.monitoring.formatZoneLabel
import com.example.mobile_tracker.presentation.monitoring.workerStatusColor
import org.koin.androidx.compose.koinViewModel

@Composable
fun WorkersScreen(
    onBack: (() -> Unit)? = null,
    onNavigateToMonitoring: () -> Unit = {},
    onNavigateToMaps: () -> Unit = {},
    onNavigateToAlerts: () -> Unit = {},
    onOpenWorkerDetail: (String) -> Unit = {},
    viewModel: WorkersViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    AppScreenScaffold(
        snackbarMessage = state.error,
        topBar = {
            MTCompactTopBar(
                title = stringResource(R.string.monitoring_nav_workers),
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
            else -> WorkersContent(
                state = state,
                onIntent = viewModel::onIntent,
                onOpenWorkerDetail = onOpenWorkerDetail,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
            )
        }
    }
}

@Composable
private fun WorkersContent(
    state: WorkersState,
    onIntent: (WorkersIntent) -> Unit,
    onOpenWorkerDetail: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val availableZones = state.workers
        .mapNotNull { it.zoneId }
        .distinct()
        .sorted()

    Column(
        modifier = modifier.padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (!state.error.isNullOrBlank()) {
            StateCard(message = state.error, isError = true)
        }

        // Search
        WorkersSearchField(
            query = state.query,
            onQueryChange = { onIntent(WorkersIntent.UpdateQuery(it)) },
        )

        // Status filter chips
        WorkersFilterRow(
            selected = state.filter,
            onSelect = { onIntent(WorkersIntent.SetFilter(it)) },
        )

        // Zone filter chips (only when zones are available)
        if (availableZones.isNotEmpty()) {
            ZoneFilterRow(
                selectedZone = state.zoneFilter,
                availableZones = availableZones,
                onSelect = { onIntent(WorkersIntent.SetZoneFilter(it)) },
            )
        }

        // Count
        Text(
            text = stringResource(R.string.workers_found_count, state.filteredWorkers.size),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f),
        )

        // List
        if (state.workers.isEmpty()) {
            EmptyState(
                title = stringResource(R.string.workers_empty),
                icon = Icons.Default.People,
                modifier = Modifier.fillMaxSize(),
            )
        } else if (state.filteredWorkers.isEmpty()) {
            EmptyState(
                title = stringResource(R.string.workers_filtered_empty),
                icon = Icons.Default.People,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                items(state.filteredWorkers, key = { it.employeeId }) { worker ->
                    WorkerListCard(
                        worker = worker,
                        onClick = { onOpenWorkerDetail(worker.employeeId) },
                    )
                }
                item { Spacer(modifier = Modifier.height(8.dp)) }
            }
        }
    }
}

@Composable
private fun WorkersSearchField(
    query: String,
    onQueryChange: (String) -> Unit,
) {
    TextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp)),
        singleLine = true,
        placeholder = {
            Text(stringResource(R.string.workers_search_placeholder))
        },
        leadingIcon = {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f),
            )
        },
        colors = TextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            disabledIndicatorColor = Color.Transparent,
        ),
        shape = RoundedCornerShape(8.dp),
    )
}

@Composable
private fun WorkersFilterRow(
    selected: WorkersFilter,
    onSelect: (WorkersFilter) -> Unit,
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        item {
            WorkersFilterChip(
                label = stringResource(R.string.workers_filter_all),
                selected = selected == WorkersFilter.All,
                onClick = { onSelect(WorkersFilter.All) },
            )
        }
        item {
            WorkersFilterChip(
                label = stringResource(R.string.workers_filter_active),
                selected = selected == WorkersFilter.Active,
                onClick = { onSelect(WorkersFilter.Active) },
            )
        }
        item {
            WorkersFilterChip(
                label = stringResource(R.string.workers_filter_idle),
                selected = selected == WorkersFilter.Idle,
                onClick = { onSelect(WorkersFilter.Idle) },
            )
        }
        item {
            WorkersFilterChip(
                label = stringResource(R.string.workers_filter_offline),
                selected = selected == WorkersFilter.Offline,
                onClick = { onSelect(WorkersFilter.Offline) },
            )
        }
    }
}

@Composable
private fun ZoneFilterRow(
    selectedZone: String?,
    availableZones: List<String>,
    onSelect: (String?) -> Unit,
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // "All zones" chip
        item {
            WorkersFilterChip(
                label = "Все зоны",
                selected = selectedZone == null,
                onClick = { onSelect(null) },
            )
        }
        items(availableZones) { zoneId ->
            WorkersFilterChip(
                label = formatZoneLabel(zoneId),
                selected = selectedZone == zoneId,
                onClick = { onSelect(zoneId) },
            )
        }
    }
}

@Composable
private fun WorkersFilterChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        color = if (selected) {
            MaterialTheme.colorScheme.onSurface
        } else {
            MaterialTheme.colorScheme.surfaceContainer
        },
        shape = RoundedCornerShape(16.dp),
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            color = if (selected) {
                MaterialTheme.colorScheme.surface
            } else {
                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.82f)
            },
        )
    }
}

@Composable
private fun WorkerListCard(
    worker: WorkerMonitoringSnapshot,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.45f),
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            // Name + role + zone badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Text(
                    text = buildAnnotatedString {
                        withStyle(
                            SpanStyle(
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f),
                            ),
                        ) {
                            append(worker.fullName)
                        }
                        append("\n")
                        withStyle(
                            SpanStyle(
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f),
                            ),
                        ) {
                            append(worker.roleLabel)
                        }
                    },
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.weight(1f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                // Zone badge
                worker.zoneId?.let { zone ->
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        modifier = Modifier.padding(start = 8.dp),
                    ) {
                        Text(
                            text = formatZoneLabel(zone),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f),
                        )
                    }
                }
            }

            // Row 1: Status dot
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(workerStatusColor(worker.status)),
                    )
                    Text(
                        text = workerStatusLabel(worker.status),
                        style = MaterialTheme.typography.bodySmall,
                        color = workerStatusColor(worker.status),
                    )
                }
            }

            // Row 2: Heart / Steps / Battery — spread full width so nothing overflows
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                    WorkerStatMini(
                        icon = Icons.Default.Favorite,
                        value = worker.heartRate?.toString()
                            ?: stringResource(R.string.worker_detail_no_data_short),
                        unit = stringResource(R.string.workers_heart_unit),
                    )
                    WorkerStatMini(
                        icon = Icons.AutoMirrored.Filled.DirectionsWalk,
                        value = worker.steps.toString(),
                        unit = stringResource(R.string.workers_steps_unit),
                    )
                    WorkerStatMini(
                        icon = Icons.Default.Bolt,
                        value = worker.batteryPercent.toString(),
                        unit = stringResource(R.string.workers_battery_unit),
                    )
            }
        }
    }
}

@Composable
private fun WorkerStatMini(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    value: String,
    unit: String,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(14.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f),
        )
        Text(
            text = buildAnnotatedString {
                withStyle(SpanStyle(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f))) {
                    append(value)
                }
                append(" ")
                withStyle(SpanStyle(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.28f))) {
                    append(unit)
                }
            },
            style = MaterialTheme.typography.labelSmall,
        )
    }
}

@Composable
private fun workerStatusLabel(status: WorkerMonitoringStatus): String = when (status) {
    WorkerMonitoringStatus.Active -> stringResource(R.string.workers_status_active)
    WorkerMonitoringStatus.Idle -> stringResource(R.string.workers_status_idle)
    WorkerMonitoringStatus.Offline -> stringResource(R.string.workers_status_offline)
}
