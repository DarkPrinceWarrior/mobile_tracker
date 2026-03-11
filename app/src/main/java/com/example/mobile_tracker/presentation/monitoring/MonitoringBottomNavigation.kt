package com.example.mobile_tracker.presentation.monitoring

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.mobile_tracker.R

enum class MonitoringTab {
    Monitoring,
    Workers,
    Maps,
    Alerts,
}

@Composable
fun MonitoringBottomBar(
    current: MonitoringTab,
    onNavigateToMonitoring: () -> Unit,
    onNavigateToWorkers: () -> Unit,
    onNavigateToMaps: () -> Unit,
    onNavigateToAlerts: () -> Unit,
) {
    Surface(color = Color.White, tonalElevation = 0.dp) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            MonitoringNavItem(
                icon = Icons.Default.Home,
                label = stringResource(R.string.monitoring_title),
                selected = current == MonitoringTab.Monitoring,
                onClick = onNavigateToMonitoring,
            )
            MonitoringNavItem(
                icon = Icons.Default.People,
                label = stringResource(R.string.monitoring_nav_workers),
                selected = current == MonitoringTab.Workers,
                onClick = onNavigateToWorkers,
            )
            MonitoringNavItem(
                icon = Icons.Outlined.LocationOn,
                label = stringResource(R.string.monitoring_nav_maps),
                selected = current == MonitoringTab.Maps,
                onClick = onNavigateToMaps,
            )
            MonitoringNavItem(
                icon = Icons.Default.Warning,
                label = stringResource(R.string.monitoring_nav_alerts),
                selected = current == MonitoringTab.Alerts,
                onClick = onNavigateToAlerts,
            )
        }
    }
}

@Composable
private fun MonitoringNavItem(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val contentColor = if (selected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f)
    }

    Column(
        modifier = Modifier
            .width(80.dp)
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = contentColor,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = contentColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
