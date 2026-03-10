package com.example.mobile_tracker.presentation.alerts

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.SyncProblem
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.mobile_tracker.R
import com.example.mobile_tracker.presentation.common.AdaptiveListDetail
import com.example.mobile_tracker.presentation.common.AppScreenScaffold
import com.example.mobile_tracker.presentation.common.EmptyState
import com.example.mobile_tracker.presentation.common.LoadingState
import com.example.mobile_tracker.presentation.common.MTCard
import com.example.mobile_tracker.presentation.common.MTCompactTopBar
import com.example.mobile_tracker.presentation.common.MTStatusBadge
import com.example.mobile_tracker.presentation.common.MTStatusTone
import com.example.mobile_tracker.presentation.common.StateCard
import com.example.mobile_tracker.presentation.common.rememberIsTablet
import com.example.mobile_tracker.ui.theme.AppLayout
import com.example.mobile_tracker.ui.theme.AppRadius
import com.example.mobile_tracker.ui.theme.AppSpacing
import com.example.mobile_tracker.ui.theme.danger
import com.example.mobile_tracker.ui.theme.info
import com.example.mobile_tracker.ui.theme.success
import com.example.mobile_tracker.ui.theme.warning
import com.example.mobile_tracker.util.formatTimestamp
import org.koin.androidx.compose.koinViewModel

@Composable
fun AlertsScreen(
    onBack: (() -> Unit)? = null,
    onNavigateToDevices: () -> Unit = {},
    onNavigateToReturn: () -> Unit = {},
    onNavigateToJournal: () -> Unit = {},
    viewModel: AlertsViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val isTablet = rememberIsTablet()
    val selectedAlert = state.filteredAlerts.firstOrNull { it.id == state.selectedAlertId }

    AppScreenScaffold(
        snackbarMessage = state.error,
        topBar = {
            MTCompactTopBar(
                title = stringResource(R.string.alerts_title),
                subtitle = stringResource(
                    R.string.alerts_subtitle,
                    state.siteName,
                    state.shiftDate,
                ),
                navigationIcon = {
                    if (onBack != null) {
                        IconButton(onClick = onBack) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.action_back),
                                tint = MaterialTheme.colorScheme.onSurface,
                            )
                        }
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = AppLayout.screenPadding, vertical = AppSpacing.sm),
            verticalArrangement = Arrangement.spacedBy(AppSpacing.md),
        ) {
            AlertsSummaryCard(state = state)

            AlertsFilterRow(
                selectedSeverity = state.selectedSeverity,
                onSelected = { severity ->
                    viewModel.onIntent(AlertsIntent.SetSeverity(severity))
                },
            )

            if (state.error != null) {
                StateCard(message = state.error.orEmpty())
            }

            Box(modifier = Modifier.weight(1f)) {
                AdaptiveListDetail(
                    isTablet = isTablet,
                    listPane = { paneModifier ->
                        when {
                            state.isLoading -> LoadingState(modifier = paneModifier)
                            state.filteredAlerts.isEmpty() -> EmptyState(
                                title = stringResource(R.string.alerts_empty),
                                icon = Icons.Default.Info,
                                modifier = paneModifier,
                            )
                            else -> LazyColumn(
                                modifier = paneModifier,
                                verticalArrangement = Arrangement.spacedBy(AppSpacing.xs),
                            ) {
                                items(state.filteredAlerts, key = { it.id }) { alert ->
                                    AlertCard(
                                        alert = alert,
                                        isSelected = state.selectedAlertId == alert.id,
                                        showInlineAction = !isTablet,
                                        onSelect = {
                                            viewModel.onIntent(AlertsIntent.SelectAlert(alert.id))
                                        },
                                        onOpenTarget = {
                                            openAlertDestination(
                                                alert = alert,
                                                onNavigateToDevices = onNavigateToDevices,
                                                onNavigateToReturn = onNavigateToReturn,
                                                onNavigateToJournal = onNavigateToJournal,
                                            )
                                        },
                                    )
                                }
                                item { Spacer(modifier = Modifier.height(AppSpacing.lg)) }
                            }
                        }
                    },
                    detailPane = { paneModifier ->
                        AlertDetailPane(
                            modifier = paneModifier.padding(start = AppSpacing.sm),
                            alert = selectedAlert,
                            onOpenTarget = {
                                selectedAlert?.let { alert ->
                                    openAlertDestination(
                                        alert = alert,
                                        onNavigateToDevices = onNavigateToDevices,
                                        onNavigateToReturn = onNavigateToReturn,
                                        onNavigateToJournal = onNavigateToJournal,
                                    )
                                }
                            },
                        )
                    },
                )
            }
        }
    }
}

@Composable
private fun AlertsSummaryCard(
    state: AlertsState,
) {
    MTCard {
        Text(
            text = stringResource(R.string.alerts_summary_title),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(AppSpacing.xs),
        ) {
            AlertCountTile(
                modifier = Modifier.weight(1f),
                label = stringResource(R.string.alerts_filter_critical),
                value = state.criticalCount.toString(),
                color = MaterialTheme.colorScheme.danger,
            )
            AlertCountTile(
                modifier = Modifier.weight(1f),
                label = stringResource(R.string.alerts_filter_warning),
                value = state.warningCount.toString(),
                color = MaterialTheme.colorScheme.warning,
            )
            AlertCountTile(
                modifier = Modifier.weight(1f),
                label = stringResource(R.string.alerts_filter_info),
                value = state.infoCount.toString(),
                color = MaterialTheme.colorScheme.info,
            )
        }
    }
}

@Composable
private fun AlertCountTile(
    modifier: Modifier = Modifier,
    label: String,
    value: String,
    color: Color,
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
private fun AlertsFilterRow(
    selectedSeverity: AlertSeverity?,
    onSelected: (AlertSeverity?) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(AppSpacing.xs),
    ) {
        AlertsFilterChip(
            label = stringResource(R.string.alerts_filter_all),
            selected = selectedSeverity == null,
            onClick = { onSelected(null) },
        )
        AlertsFilterChip(
            label = stringResource(R.string.alerts_filter_critical),
            selected = selectedSeverity == AlertSeverity.Critical,
            onClick = { onSelected(AlertSeverity.Critical) },
        )
        AlertsFilterChip(
            label = stringResource(R.string.alerts_filter_warning),
            selected = selectedSeverity == AlertSeverity.Warning,
            onClick = { onSelected(AlertSeverity.Warning) },
        )
        AlertsFilterChip(
            label = stringResource(R.string.alerts_filter_info),
            selected = selectedSeverity == AlertSeverity.Info,
            onClick = { onSelected(AlertSeverity.Info) },
        )
    }
}

@Composable
private fun AlertsFilterChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier.clickable(onClick = onClick),
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
                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
            },
        )
    }
}

@Composable
private fun AlertCard(
    alert: OperatorAlertItem,
    isSelected: Boolean,
    showInlineAction: Boolean,
    onSelect: () -> Unit,
    onOpenTarget: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .semantics { selected = isSelected }
            .clickable(
                onClickLabel = stringResource(R.string.alerts_open_details),
                role = Role.Button,
                onClick = onSelect,
            ),
        shape = RoundedCornerShape(AppRadius.lg),
        border = if (isSelected) {
            BorderStroke(1.dp, alertColor(alert.severity).copy(alpha = 0.32f))
        } else {
            null
        },
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.surfaceContainer
            } else {
                MaterialTheme.colorScheme.surfaceContainerLowest
            },
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
                        .background(alertColor(alert.severity).copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = alertIcon(alert.category),
                        contentDescription = null,
                        tint = alertColor(alert.severity),
                    )
                }
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Text(
                        text = alertTitle(alert),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = alert.subject,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = alert.details,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(AppSpacing.xxs),
                ) {
                    MTStatusBadge(
                        label = alertSeverityLabel(alert.severity),
                        tone = alertTone(alert.severity),
                    )
                    Text(
                        text = formatTimestamp(alert.timestamp, pattern = "HH:mm"),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            if (showInlineAction) {
                Button(
                    onClick = onOpenTarget,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(AppRadius.xl),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                    ),
                ) {
                    Text(text = alertActionLabel(alert.destination))
                    Spacer(modifier = Modifier.size(AppSpacing.xxs))
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun AlertDetailPane(
    modifier: Modifier,
    alert: OperatorAlertItem?,
    onOpenTarget: () -> Unit,
) {
    if (alert == null) {
        EmptyState(
            title = stringResource(R.string.alerts_detail_empty),
            icon = Icons.Default.Info,
            modifier = modifier.fillMaxSize(),
        )
        return
    }

    Card(
        modifier = modifier.fillMaxSize(),
        shape = RoundedCornerShape(AppRadius.xl),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
        ),
    ) {
        Column(
            modifier = Modifier.padding(AppSpacing.lg),
            verticalArrangement = Arrangement.spacedBy(AppSpacing.md),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(alertColor(alert.severity).copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = alertIcon(alert.category),
                        contentDescription = null,
                        tint = alertColor(alert.severity),
                    )
                }
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Text(
                        text = alertTitle(alert),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = formatTimestamp(alert.timestamp, pattern = "dd.MM HH:mm"),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                MTStatusBadge(
                    label = alertSeverityLabel(alert.severity),
                    tone = alertTone(alert.severity),
                )
            }

            AlertDetailRow(
                label = stringResource(R.string.alerts_detail_subject),
                value = alert.subject,
            )
            AlertDetailRow(
                label = stringResource(R.string.alerts_detail_message),
                value = alert.details,
            )
            AlertDetailRow(
                label = stringResource(R.string.alerts_detail_action),
                value = alertActionLabel(alert.destination),
            )

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = onOpenTarget,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(AppRadius.xl),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ),
            ) {
                Text(text = alertActionLabel(alert.destination))
            }
        }
    }
}

@Composable
private fun AlertDetailRow(
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
                .padding(horizontal = 14.dp, vertical = 12.dp),
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
            )
        }
    }
}

private fun openAlertDestination(
    alert: OperatorAlertItem,
    onNavigateToDevices: () -> Unit,
    onNavigateToReturn: () -> Unit,
    onNavigateToJournal: () -> Unit,
) {
    when (alert.destination) {
        AlertDestination.Devices -> onNavigateToDevices()
        AlertDestination.Return -> onNavigateToReturn()
        AlertDestination.Journal -> onNavigateToJournal()
    }
}

@Composable
private fun alertSeverityLabel(severity: AlertSeverity): String = when (severity) {
    AlertSeverity.Critical -> stringResource(R.string.alerts_filter_critical)
    AlertSeverity.Warning -> stringResource(R.string.alerts_filter_warning)
    AlertSeverity.Info -> stringResource(R.string.alerts_filter_info)
}

@Composable
private fun alertActionLabel(destination: AlertDestination): String = when (destination) {
    AlertDestination.Devices -> stringResource(R.string.alerts_action_devices)
    AlertDestination.Return -> stringResource(R.string.alerts_action_return)
    AlertDestination.Journal -> stringResource(R.string.alerts_action_journal)
}

@Composable
private fun alertTitle(alert: OperatorAlertItem): String = when (alert.category) {
    AlertCategory.PacketError -> stringResource(R.string.alerts_packet_error_title)
    AlertCategory.PacketPending -> stringResource(R.string.alerts_packet_pending_title)
    AlertCategory.BindingUnsynced -> stringResource(R.string.alerts_binding_unsynced_title)
    AlertCategory.BindingUploadRequired -> stringResource(R.string.alerts_binding_upload_title)
    AlertCategory.LogError -> stringResource(R.string.alerts_log_error_title)
    AlertCategory.LogPending -> stringResource(R.string.alerts_log_pending_title)
}

private fun alertColor(severity: AlertSeverity): Color = when (severity) {
    AlertSeverity.Critical -> Color(0xFFC43232)
    AlertSeverity.Warning -> Color(0xFFD2A232)
    AlertSeverity.Info -> Color(0xFF2D9552)
}

private fun alertTone(severity: AlertSeverity): MTStatusTone = when (severity) {
    AlertSeverity.Critical -> MTStatusTone.Danger
    AlertSeverity.Warning -> MTStatusTone.Warning
    AlertSeverity.Info -> MTStatusTone.Info
}

private fun alertIcon(category: AlertCategory): ImageVector = when (category) {
    AlertCategory.PacketError -> Icons.Default.Error
    AlertCategory.PacketPending -> Icons.Default.CloudOff
    AlertCategory.BindingUnsynced -> Icons.Default.SyncProblem
    AlertCategory.BindingUploadRequired -> Icons.Default.Warning
    AlertCategory.LogError -> Icons.Default.Error
    AlertCategory.LogPending -> Icons.Default.Info
}
