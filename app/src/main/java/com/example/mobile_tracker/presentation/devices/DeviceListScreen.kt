package com.example.mobile_tracker.presentation.devices

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
import androidx.compose.material.icons.filled.Battery2Bar
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Watch
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import com.example.mobile_tracker.domain.model.Device
import com.example.mobile_tracker.presentation.common.AdaptiveListDetail
import com.example.mobile_tracker.presentation.common.AppScreenScaffold
import com.example.mobile_tracker.presentation.common.EmptyState
import com.example.mobile_tracker.presentation.common.LoadingState
import com.example.mobile_tracker.presentation.common.MTCard
import com.example.mobile_tracker.presentation.common.MTCompactTopBar
import com.example.mobile_tracker.presentation.common.MTStatusBadge
import com.example.mobile_tracker.presentation.common.MTStatusTone
import com.example.mobile_tracker.presentation.common.SearchField
import com.example.mobile_tracker.presentation.common.StateCard
import com.example.mobile_tracker.presentation.common.rememberIsTablet
import com.example.mobile_tracker.ui.theme.AppLayout
import com.example.mobile_tracker.ui.theme.AppRadius
import com.example.mobile_tracker.ui.theme.AppSpacing
import com.example.mobile_tracker.ui.theme.danger
import com.example.mobile_tracker.ui.theme.success
import com.example.mobile_tracker.ui.theme.warning
import org.koin.androidx.compose.koinViewModel

@Composable
fun DeviceListScreen(
    onBack: (() -> Unit)? = null,
    viewModel: DeviceListViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val isTablet = rememberIsTablet()
    val selectedDevice = state.devices.firstOrNull { it.deviceId == state.selectedDeviceId }

    AppScreenScaffold(
        snackbarMessage = state.error,
        topBar = {
            MTCompactTopBar(
                title = stringResource(R.string.devices_title),
                subtitle = stringResource(R.string.devices_search_hint),
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
                actions = {
                    if (state.isSyncing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.tertiary,
                        )
                    } else {
                        IconButton(onClick = { viewModel.onIntent(DeviceListIntent.SyncDevices) }) {
                            Icon(
                                Icons.Default.Refresh,
                                contentDescription = stringResource(R.string.sync_refresh),
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
            DeviceSummaryCard(
                totalCount = state.totalCount,
                availableCount = state.availableCount,
                issuedCount = state.issuedCount,
            )

            SearchField(
                query = state.searchQuery,
                onQueryChange = { viewModel.onIntent(DeviceListIntent.Search(it)) },
                placeholder = stringResource(R.string.devices_search_hint),
            )

            DeviceFilterRow(
                selectedFilter = state.filterStatus,
                totalCount = state.totalCount,
                availableCount = state.availableCount,
                issuedCount = state.issuedCount,
                onFilterSelected = { status ->
                    viewModel.onIntent(DeviceListIntent.FilterByStatus(status))
                },
            )

            Box(modifier = Modifier.weight(1f)) {
                AdaptiveListDetail(
                    isTablet = isTablet,
                    listPane = { paneModifier ->
                        when {
                            state.isLoading -> LoadingState(modifier = paneModifier)
                            state.error != null -> Box(modifier = paneModifier) {
                                StateCard(message = state.error.orEmpty())
                            }
                            state.devices.isEmpty() -> EmptyState(
                                title = stringResource(R.string.devices_empty),
                                icon = Icons.Default.Watch,
                                modifier = paneModifier,
                            )
                            else -> LazyColumn(
                                modifier = paneModifier,
                                verticalArrangement = Arrangement.spacedBy(AppSpacing.xs),
                            ) {
                                items(state.devices, key = { it.deviceId }) { device ->
                                    DeviceCard(
                                        device = device,
                                        isSelected = state.selectedDeviceId == device.deviceId,
                                        onClick = {
                                            viewModel.onIntent(
                                                DeviceListIntent.SelectDevice(device.deviceId),
                                            )
                                        },
                                    )
                                }
                                item { Spacer(modifier = Modifier.height(AppSpacing.lg)) }
                            }
                        }
                    },
                    detailPane = { paneModifier ->
                        DeviceDetailPane(
                            modifier = paneModifier.padding(start = AppSpacing.sm),
                            device = selectedDevice,
                        )
                    },
                )
            }
        }
    }
}

@Composable
private fun DeviceSummaryCard(
    totalCount: Int,
    availableCount: Int,
    issuedCount: Int,
) {
    MTCard {
        Text(
            text = stringResource(R.string.devices_section_title),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(AppSpacing.xs),
        ) {
            DeviceCountTile(
                title = stringResource(R.string.devices_filter_all),
                value = totalCount.toString(),
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f),
            )
            DeviceCountTile(
                title = stringResource(R.string.devices_filter_available),
                value = availableCount.toString(),
                color = MaterialTheme.colorScheme.success,
                modifier = Modifier.weight(1f),
            )
            DeviceCountTile(
                title = stringResource(R.string.devices_filter_issued),
                value = issuedCount.toString(),
                color = MaterialTheme.colorScheme.warning,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun DeviceCountTile(
    title: String,
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
                text = title,
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
private fun DeviceFilterRow(
    selectedFilter: String?,
    totalCount: Int,
    availableCount: Int,
    issuedCount: Int,
    onFilterSelected: (String?) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(AppSpacing.xs),
    ) {
        DeviceFilterChip(
            label = "${stringResource(R.string.devices_filter_all)} ($totalCount)",
            selected = selectedFilter == null,
            onClick = { onFilterSelected(null) },
        )
        DeviceFilterChip(
            label = "${stringResource(R.string.devices_filter_available)} ($availableCount)",
            selected = selectedFilter == "available",
            onClick = { onFilterSelected("available") },
        )
        DeviceFilterChip(
            label = "${stringResource(R.string.devices_filter_issued)} ($issuedCount)",
            selected = selectedFilter == "issued",
            onClick = { onFilterSelected("issued") },
        )
    }
}

@Composable
private fun DeviceFilterChip(
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
private fun DeviceCard(
    device: Device,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    val chargeMeta = deviceChargeMeta(chargeStatus = device.chargeStatus)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .semantics { selected = isSelected }
            .clickable(
                onClickLabel = stringResource(R.string.action_open_device_details),
                role = Role.Button,
                onClick = onClick,
            ),
        shape = RoundedCornerShape(AppRadius.lg),
        border = if (isSelected) {
            BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary.copy(alpha = 0.32f))
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
            modifier = Modifier
                .fillMaxWidth()
                .padding(AppLayout.cardPadding),
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
                        .background(deviceStatusContainerColor(device.localStatus)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Default.Watch,
                        contentDescription = null,
                        modifier = Modifier.size(22.dp),
                        tint = deviceStatusContentColor(device.localStatus),
                    )
                }
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Text(
                        text = device.deviceId,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    val secondaryLine = listOfNotNull(device.serialNumber, device.model)
                        .joinToString(" · ")
                    if (secondaryLine.isNotBlank()) {
                        Text(
                            text = secondaryLine,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                DeviceStatusBadge(status = device.localStatus)
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                DeviceChargeChip(meta = chargeMeta)
                if (device.localStatus == "issued" && !device.employeeName.isNullOrBlank()) {
                    Text(
                        text = stringResource(R.string.devices_issued_to, device.employeeName),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.tertiary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

@Composable
private fun DeviceDetailPane(
    modifier: Modifier,
    device: Device?,
) {
    if (device == null) {
        EmptyState(
            title = stringResource(R.string.devices_detail_empty),
            icon = Icons.Default.Watch,
            modifier = modifier.fillMaxSize(),
        )
        return
    }

    val chargeMeta = deviceChargeMeta(chargeStatus = device.chargeStatus)

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
                        .background(deviceStatusContainerColor(device.localStatus)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Default.Watch,
                        contentDescription = null,
                        tint = deviceStatusContentColor(device.localStatus),
                    )
                }
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Text(
                        text = device.deviceId,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    device.model?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                DeviceStatusBadge(status = device.localStatus)
            }

            DeviceChargeChip(meta = chargeMeta)

            device.serialNumber?.let {
                DeviceDetailRow(
                    label = stringResource(R.string.devices_detail_serial),
                    value = it,
                )
            }
            device.employeeName?.let {
                DeviceDetailRow(
                    label = stringResource(R.string.devices_status_issued),
                    value = it,
                )
            }
            device.lastSyncAt?.let {
                DeviceDetailRow(
                    label = stringResource(R.string.devices_detail_last_sync),
                    value = it,
                )
            }
        }
    }
}

@Composable
private fun DeviceDetailRow(
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

@Composable
private fun DeviceStatusBadge(status: String) {
    val (label, tone) = when (status) {
        "available" -> stringResource(R.string.devices_status_available) to MTStatusTone.Success
        "issued" -> stringResource(R.string.devices_status_issued) to MTStatusTone.Warning
        "discharged" -> stringResource(R.string.devices_status_discharged) to MTStatusTone.Danger
        "faulty" -> stringResource(R.string.devices_status_faulty) to MTStatusTone.Neutral
        else -> status to MTStatusTone.Neutral
    }
    MTStatusBadge(label = label, tone = tone)
}

private data class DeviceChargeMeta(
    val icon: ImageVector,
    val label: String,
    val color: Color,
)

@Composable
private fun deviceChargeMeta(chargeStatus: String): DeviceChargeMeta {
    return when (chargeStatus) {
        "charged" -> DeviceChargeMeta(
            icon = Icons.Default.BatteryFull,
            label = stringResource(R.string.devices_charge_charged),
            color = MaterialTheme.colorScheme.success,
        )
        "low" -> DeviceChargeMeta(
            icon = Icons.Default.Battery2Bar,
            label = stringResource(R.string.devices_charge_low),
            color = MaterialTheme.colorScheme.warning,
        )
        "critical" -> DeviceChargeMeta(
            icon = Icons.Default.BatteryAlert,
            label = stringResource(R.string.devices_charge_critical),
            color = MaterialTheme.colorScheme.danger,
        )
        "charging" -> DeviceChargeMeta(
            icon = Icons.Default.BatteryChargingFull,
            label = stringResource(R.string.devices_charge_charging),
            color = MaterialTheme.colorScheme.tertiary,
        )
        else -> DeviceChargeMeta(
            icon = Icons.Default.Battery2Bar,
            label = stringResource(R.string.devices_charge_low),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun DeviceChargeChip(meta: DeviceChargeMeta) {
    Surface(
        shape = RoundedCornerShape(AppRadius.pill),
        color = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AppSpacing.xxs),
        ) {
            Icon(
                imageVector = meta.icon,
                contentDescription = null,
                modifier = Modifier.size(14.dp),
                tint = meta.color,
            )
            Text(
                text = meta.label,
                style = MaterialTheme.typography.labelMedium,
                color = meta.color,
            )
        }
    }
}

@Composable
private fun deviceStatusContainerColor(status: String): Color {
    return when (status) {
        "available" -> MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.55f)
        "issued" -> MaterialTheme.colorScheme.secondaryContainer
        "discharged" -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.7f)
        else -> MaterialTheme.colorScheme.surfaceContainer
    }
}

@Composable
private fun deviceStatusContentColor(status: String): Color {
    return when (status) {
        "available" -> MaterialTheme.colorScheme.tertiary
        "issued" -> MaterialTheme.colorScheme.onSecondaryContainer
        "discharged" -> MaterialTheme.colorScheme.danger
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
}
