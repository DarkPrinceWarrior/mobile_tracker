package com.example.mobile_tracker.presentation.monitoring

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.People
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.mobile_tracker.R
import com.example.mobile_tracker.presentation.common.AppScreenScaffold
import com.example.mobile_tracker.presentation.common.EmptyState
import com.example.mobile_tracker.presentation.common.LoadingState
import com.example.mobile_tracker.presentation.common.MTCompactTopBar
import com.example.mobile_tracker.presentation.common.StateCard
import org.koin.androidx.compose.koinViewModel

// ── Preview zone summaries (all 5 zones with demo worker counts) ──────────────
private val previewZoneSummaries = monitoringZones.mapIndexed { i, zone ->
    MonitoringZoneSummary(
        zone = zone,
        totalWorkers = when (i) { 0 -> 8; 1 -> 7; 2 -> 5; 3 -> 6; else -> 4 },
        activeWorkers = when (i) { 0 -> 6; 1 -> 5; 2 -> 3; 3 -> 4; else -> 3 },
        idleWorkers = when (i) { 0 -> 2; 1 -> 2; 2 -> 2; 3 -> 2; else -> 1 },
    )
}

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
        topBar = {
            MTCompactTopBar(
                title = stringResource(R.string.monitoring_title),
                subtitle = if (state.siteName.isNotBlank()) state.siteName else null,
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
        // ── No bottom bar: monitoring is a sub-screen, not a root tab ─────────
    ) { padding ->
        when {
            state.isLoading -> LoadingState(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
            )
            state.totalWorkers == 0 -> MonitoringEmptyPane(
                error = state.error,
                onNavigateToWorkers = onNavigateToWorkers,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
            )
            else -> MonitoringContent(
                state = state,
                error = state.error,
                onNavigateToMaps = onNavigateToMaps,
                onNavigateToWorkers = onNavigateToWorkers,
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
    onNavigateToMaps: () -> Unit,
    onNavigateToWorkers: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val zones = if (state.zoneSummaries.isNotEmpty()) state.zoneSummaries else previewZoneSummaries

    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        if (!error.isNullOrBlank()) {
            StateCard(message = error, isError = true)
        }

        // ── Statistics ─────────────────────────────────────────────────────
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            SectionLabel(
                text = if (state.siteName.isNotBlank()) {
                    "Статистика · ${state.siteName}"
                } else {
                    stringResource(R.string.monitoring_stats_title)
                },
            )
            StatisticsPanel(
                efficiencyPercent = state.efficiencyPercent,
                activeCount = state.activeCount,
                idleCount = state.idleCount,
                offlineCount = state.offlineCount,
            )
        }

        // ── Zones ──────────────────────────────────────────────────────────
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            SectionLabel(text = "Зоны площадки")
            ZonesPanel(
                zones = zones,
                shiftWindow = monitoringShiftWindow(state.shiftType),
                onZoneClick = onNavigateToMaps,
            )
        }

        // ── Workers button ─────────────────────────────────────────────────
        WorkersButton(onClick = onNavigateToWorkers)

        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
private fun MonitoringEmptyPane(
    error: String?,
    onNavigateToWorkers: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
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

// ── Section label ──────────────────────────────────────────────────────────────

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.82f),
    )
}

// ── Statistics panel ───────────────────────────────────────────────────────────

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
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            EfficiencyHeroCard(efficiencyPercent = efficiencyPercent)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
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
private fun EfficiencyHeroCard(efficiencyPercent: Int) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF122C1E))
            .border(1.dp, Color(0x5500A36A), RoundedCornerShape(8.dp)),
    ) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(Color(0x3300A36A), Color.Transparent),
                    ),
                ),
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "$efficiencyPercent",
                style = MaterialTheme.typography.displayLarge.copy(
                    fontSize = 72.sp,
                    lineHeight = 72.sp,
                ),
                fontWeight = FontWeight.Bold,
                color = Color(0xFFAFFF69),
            )
            Text(
                text = stringResource(R.string.monitoring_efficiency_compact_label),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = Color(0xFFF9F9F9),
                maxLines = 1,
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
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
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
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f),
                )
            }
        }
    }
}

// ── Zones panel ────────────────────────────────────────────────────────────────

@Composable
private fun ZonesPanel(
    zones: List<MonitoringZoneSummary>,
    shiftWindow: String,
    onZoneClick: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 5.dp, top = 8.dp, end = 8.dp, bottom = 8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            zones.forEach { summary ->
                ZoneRow(
                    summary = summary,
                    shiftWindow = shiftWindow,
                    onClick = onZoneClick,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ZoneRow(
    summary: MonitoringZoneSummary,
    shiftWindow: String,
    onClick: () -> Unit,
) {
    val isActive = summary.totalWorkers > 0
    val statusColor = if (isActive) {
        Color(0xFF00A36A)
    } else {
        workerStatusColor(WorkerMonitoringStatus.Idle)
    }

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
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(statusColor),
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = formatZoneLabel(summary.zone.id),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = shiftWindow,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f),
                )
            }
            if (isActive) {
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "${summary.totalWorkers} чел.",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = "акт.: ${summary.activeWorkers} · прост.: ${summary.idleWorkers}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f),
                    )
                }
            } else {
                Text(
                    text = "Нет работников",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f),
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f),
                modifier = Modifier.size(14.dp),
            )
        }
    }
}

// ── Workers button ──────────────────────────────────────────────────────────────

@Composable
private fun WorkersButton(onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            contentColor = MaterialTheme.colorScheme.onSurface,
        ),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
    ) {
        Icon(
            imageVector = Icons.Default.People,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f),
        )
        Text(
            text = "  Список работников",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Medium,
        )
    }
}

private fun monitoringShiftWindow(shiftType: String): String = if (shiftType == "night") {
    "смена 19:00–06:30"
} else {
    "смена 07:00–18:30"
}
