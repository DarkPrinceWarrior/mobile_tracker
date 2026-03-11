package com.example.mobile_tracker.presentation.monitoring

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.mobile_tracker.R
import com.example.mobile_tracker.presentation.common.MTStatusBadge
import com.example.mobile_tracker.presentation.common.MTStatusTone
import com.example.mobile_tracker.ui.theme.AppRadius
import com.example.mobile_tracker.ui.theme.AppSpacing

enum class MonitoringMapMode {
    Heatmap,
    Workers,
}

@Composable
fun WorkerMonitoringStatusBadge(
    status: WorkerMonitoringStatus,
    modifier: Modifier = Modifier,
) {
    MTStatusBadge(
        label = when (status) {
            WorkerMonitoringStatus.Active -> stringResource(R.string.monitoring_status_active)
            WorkerMonitoringStatus.Idle -> stringResource(R.string.monitoring_status_idle)
            WorkerMonitoringStatus.Offline -> stringResource(R.string.monitoring_status_offline)
        },
        tone = when (status) {
            WorkerMonitoringStatus.Active -> MTStatusTone.Success
            WorkerMonitoringStatus.Idle -> MTStatusTone.Warning
            WorkerMonitoringStatus.Offline -> MTStatusTone.Danger
        },
        modifier = modifier,
    )
}

@Composable
fun MonitoringModeChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(AppRadius.pill),
        color = if (selected) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.surfaceContainer
        },
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            style = MaterialTheme.typography.labelLarge,
            color = if (selected) {
                MaterialTheme.colorScheme.onPrimary
            } else {
                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.82f)
            },
        )
    }
}

@Composable
fun MonitoringSiteMap(
    workers: List<WorkerMonitoringSnapshot>,
    zoneSummaries: List<MonitoringZoneSummary>,
    mode: MonitoringMapMode,
    modifier: Modifier = Modifier,
    highlightedWorkerId: String? = null,
    onWorkerClick: ((String) -> Unit)? = null,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(AppRadius.xl),
        color = Color.Transparent,
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .height(360.dp)
                .clip(RoundedCornerShape(AppRadius.xl))
                .background(Color(0xFF092114)),
        ) {
            Image(
                painter = painterResource(id = R.drawable.site_heatmap_map),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color(0x22B6F5D4),
                                Color(0x330D2417),
                                Color(0x8A08120D),
                            ),
                        ),
                    ),
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF062115).copy(alpha = if (mode == MonitoringMapMode.Heatmap) 0.16f else 0.26f)),
            )

            Box(
                modifier = Modifier
                    .offset(x = maxWidth * 0.05f, y = maxHeight * 0.10f)
                    .size(width = maxWidth * 0.22f, height = maxHeight * 0.18f)
                    .clip(RoundedCornerShape(AppRadius.xl))
                    .background(Color.White.copy(alpha = 0.05f)),
            )
            Box(
                modifier = Modifier
                    .offset(x = maxWidth * 0.68f, y = maxHeight * 0.18f)
                    .size(width = maxWidth * 0.18f, height = maxHeight * 0.30f)
                    .clip(RoundedCornerShape(AppRadius.xl))
                    .background(Color.White.copy(alpha = 0.04f)),
            )
            Box(
                modifier = Modifier
                    .offset(x = maxWidth * 0.22f, y = maxHeight * 0.62f)
                    .size(width = maxWidth * 0.36f, height = maxHeight * 0.14f)
                    .clip(RoundedCornerShape(AppRadius.xl))
                    .background(Color.White.copy(alpha = 0.05f)),
            )

            zoneSummaries.forEach { summary ->
                val zone = summary.zone
                val accent = zoneAccentColor(summary)
                val fill = accent.copy(alpha = if (mode == MonitoringMapMode.Heatmap) {
                    (0.14f + (summary.totalWorkers * 0.03f)).coerceAtMost(0.34f)
                } else {
                    0.1f
                })
                Surface(
                    modifier = Modifier
                        .offset(
                            x = maxWidth * zone.xRatio,
                            y = maxHeight * zone.yRatio,
                        )
                        .size(
                            width = maxWidth * zone.widthRatio,
                            height = maxHeight * zone.heightRatio,
                        ),
                    shape = RoundedCornerShape(14.dp),
                    color = fill,
                    border = androidx.compose.foundation.BorderStroke(1.dp, accent.copy(alpha = 0.9f)),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(10.dp),
                        verticalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            text = zone.id,
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White.copy(alpha = 0.94f),
                            fontWeight = FontWeight.SemiBold,
                        )
                        Surface(
                            shape = RoundedCornerShape(AppRadius.pill),
                            color = Color.Black.copy(alpha = 0.22f),
                        ) {
                            Text(
                                text = stringResource(
                                    R.string.maps_zone_workers,
                                    summary.totalWorkers,
                                ),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelMedium,
                                color = Color.White.copy(alpha = 0.82f),
                            )
                        }
                    }
                }
            }

            if (mode == MonitoringMapMode.Heatmap) {
                Box(
                    modifier = Modifier
                        .offset(x = maxWidth * 0.40f, y = maxHeight * 0.50f)
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF93FFDA)),
                    )
                }
            } else {
                workers
                    .filter { it.status != WorkerMonitoringStatus.Offline }
                    .filter { it.mapXRatio != null && it.mapYRatio != null }
                    .forEach { worker ->
                        val isHighlighted = worker.employeeId == highlightedWorkerId
                        Column(
                            modifier = Modifier
                                .offset(
                                    x = (maxWidth * worker.mapXRatio.orEmpty()) - 22.dp,
                                    y = (maxHeight * worker.mapYRatio.orEmpty()) - 42.dp,
                                )
                                .clickable(enabled = onWorkerClick != null) {
                                    onWorkerClick?.invoke(worker.employeeId)
                                },
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color.Black.copy(alpha = if (isHighlighted) 0.46f else 0.32f),
                            ) {
                                Text(
                                    text = worker.fullName.substringBefore(" "),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = Color.White.copy(alpha = 0.9f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .size(if (isHighlighted) 44.dp else 38.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = if (isHighlighted) 0.30f else 0.18f)),
                                contentAlignment = Alignment.Center,
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(if (isHighlighted) 24.dp else 20.dp)
                                        .clip(CircleShape)
                                        .background(workerStatusColor(worker.status)),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Person,
                                        contentDescription = null,
                                        modifier = Modifier.size(12.dp),
                                        tint = Color.White,
                                    )
                                }
                            }
                        }
                    }
            }

            Surface(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(4.dp),
                shape = RoundedCornerShape(10.dp),
                color = Color.Black.copy(alpha = 0.28f),
            ) {
                Text(
                    text = stringResource(R.string.maps_drag_hint),
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.78f),
                )
            }
        }
    }
}

@Composable
fun WorkerStatusLegendRow(
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(AppSpacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        WorkerLegendItem(
            color = workerStatusColor(WorkerMonitoringStatus.Active),
            label = stringResource(R.string.monitoring_status_active),
        )
        WorkerLegendItem(
            color = workerStatusColor(WorkerMonitoringStatus.Idle),
            label = stringResource(R.string.monitoring_status_idle),
        )
        WorkerLegendItem(
            color = workerStatusColor(WorkerMonitoringStatus.Offline),
            label = stringResource(R.string.monitoring_status_offline),
        )
    }
}

@Composable
private fun WorkerLegendItem(
    color: Color,
    label: String,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(AppSpacing.xxs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(color),
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

fun workerStatusColor(status: WorkerMonitoringStatus): Color = when (status) {
    WorkerMonitoringStatus.Active -> Color(0xFF2D9552)
    WorkerMonitoringStatus.Idle -> Color(0xFFD2A232)
    WorkerMonitoringStatus.Offline -> Color(0xFFC43232)
}

private fun zoneAccentColor(summary: MonitoringZoneSummary): Color {
    if (summary.totalWorkers >= 5) {
        return Color(0xFFD2A232)
    }
    return when (summary.zone.id) {
        "А1", "Б1" -> Color(0xFF2D9552)
        "А2", "Б2" -> Color(0xFFD2A232)
        else -> Color(0xFFC43232)
    }
}

private fun Float?.orEmpty(): Float = this ?: 0f
