package com.example.mobile_tracker.presentation.devices

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material.icons.filled.Battery2Bar
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Watch
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.mobile_tracker.R
import com.example.mobile_tracker.domain.model.Device
import com.example.mobile_tracker.presentation.common.AdaptiveListDetail
import com.example.mobile_tracker.presentation.common.AppScreenScaffold
import com.example.mobile_tracker.presentation.common.EmptyState
import com.example.mobile_tracker.presentation.common.LoadingState
import com.example.mobile_tracker.presentation.common.SearchField
import com.example.mobile_tracker.presentation.common.StateCard
import com.example.mobile_tracker.presentation.common.rememberIsTablet
import com.example.mobile_tracker.ui.theme.Success
import com.example.mobile_tracker.ui.theme.Warning
import com.example.mobile_tracker.ui.theme.Danger
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeviceListScreen(
    onBack: (() -> Unit)? = null,
    viewModel: DeviceListViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val isTablet = rememberIsTablet()
    val selectedDevice = state.devices.firstOrNull {
        it.deviceId == state.selectedDeviceId
    }

    AppScreenScaffold(
        snackbarMessage = state.error,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(R.string.devices_title),
                        fontWeight = FontWeight.SemiBold,
                    )
                },
                navigationIcon = {
                    if (onBack != null) {
                        IconButton(onClick = onBack) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.action_back),
                            )
                        }
                    }
                },
                colors =
                    TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme
                            .colorScheme.surface,
                    ),
                actions = {
                    if (state.isSyncing) {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .size(24.dp)
                                .padding(end = 8.dp),
                            strokeWidth = 2.dp,
                        )
                    } else {
                        IconButton(onClick = {
                            viewModel.onIntent(
                                DeviceListIntent
                                    .SyncDevices,
                            )
                        }) {
                            Icon(
                                Icons.Default.Refresh,
                                contentDescription =
                                    stringResource(R.string.sync_refresh),
                            )
                        }
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(horizontal = 16.dp),
        ) {
            Spacer(modifier = Modifier.height(4.dp))

            SearchField(
                query = state.searchQuery,
                onQueryChange = {
                    viewModel.onIntent(DeviceListIntent.Search(it))
                },
                placeholder = stringResource(R.string.devices_search_hint),
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                horizontalArrangement =
                    Arrangement.spacedBy(8.dp),
            ) {
                FilterChip(
                    selected =
                        state.filterStatus == null,
                    onClick = {
                        viewModel.onIntent(
                            DeviceListIntent
                                .FilterByStatus(null),
                        )
                    },
                    label = {
                        Text(
                            "${stringResource(R.string.devices_filter_all)} (${state.totalCount})",
                        )
                    },
                )
                FilterChip(
                    selected =
                        state.filterStatus ==
                            "available",
                    onClick = {
                        viewModel.onIntent(
                            DeviceListIntent
                                .FilterByStatus(
                                    "available",
                                ),
                        )
                    },
                    label = {
                        Text(
                            "${stringResource(R.string.devices_filter_available)} (${state.availableCount})",
                        )
                    },
                )
                FilterChip(
                    selected =
                        state.filterStatus == "issued",
                    onClick = {
                        viewModel.onIntent(
                            DeviceListIntent
                                .FilterByStatus(
                                    "issued",
                                ),
                        )
                    },
                    label = {
                        Text(
                            "${stringResource(R.string.devices_filter_issued)} (${state.issuedCount})",
                        )
                    },
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Box(
                modifier = Modifier.weight(1f),
            ) {
                AdaptiveListDetail(
                    isTablet = isTablet,
                    listPane = { paneModifier ->
                        when {
                            state.isLoading -> {
                                LoadingState(modifier = paneModifier)
                            }

                            state.error != null -> {
                                Box(modifier = paneModifier) {
                                    StateCard(message = state.error.orEmpty())
                                }
                            }

                            state.devices.isEmpty() -> {
                                EmptyState(
                                    title = stringResource(R.string.devices_empty),
                                    icon = Icons.Default.Watch,
                                    modifier = paneModifier,
                                )
                            }

                            else -> {
                                LazyColumn(
                                    modifier = paneModifier,
                                    verticalArrangement =
                                        Arrangement.spacedBy(
                                            8.dp,
                                        ),
                                ) {
                                    items(
                                        state.devices,
                                        key = { it.deviceId },
                                    ) { device ->
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
                                    item {
                                        Spacer(
                                            modifier = Modifier
                                                .height(16.dp),
                                        )
                                    }
                                }
                            }
                        }
                    },
                    detailPane = { paneModifier ->
                        DeviceDetailPane(
                            modifier = paneModifier.padding(horizontal = 12.dp),
                            device = selectedDevice,
                        )
                    },
                )
            }
        }
    }
}

@Composable
private fun DeviceCard(
    device: Device,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.secondaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceContainerLow
            },
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment =
                Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(
                        when (device.localStatus) {
                            "available" ->
                                MaterialTheme.colorScheme
                                    .primaryContainer
                            "issued" ->
                                MaterialTheme.colorScheme
                                    .tertiaryContainer
                            else ->
                                MaterialTheme.colorScheme
                                    .surfaceVariant
                        },
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Default.Watch,
                    contentDescription = null,
                    modifier = Modifier.size(22.dp),
                    tint = when (device.localStatus) {
                        "available" ->
                            MaterialTheme.colorScheme
                                .onPrimaryContainer
                        "issued" ->
                            MaterialTheme.colorScheme
                                .onTertiaryContainer
                        else ->
                            MaterialTheme.colorScheme
                                .onSurfaceVariant
                    },
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = device.deviceId,
                    style = MaterialTheme.typography
                        .titleSmall,
                    fontWeight = FontWeight.Medium,
                )
                device.serialNumber?.let {
                    Text(
                        text = stringResource(
                            R.string.devices_serial,
                            it,
                        ),
                        style = MaterialTheme
                            .typography.bodySmall,
                        color = MaterialTheme
                            .colorScheme
                            .onSurfaceVariant,
                    )
                }
                device.model?.let {
                    Text(
                        text = it,
                        style = MaterialTheme
                            .typography.bodySmall,
                        color = MaterialTheme
                            .colorScheme
                            .onSurfaceVariant,
                    )
                }
                val (chargeIcon, chargeLabel, chargeColor) =
                    when (device.chargeStatus) {
                        "charged" -> Triple(
                            Icons.Default.BatteryFull,
                            stringResource(R.string.devices_charge_charged),
                            Success,
                        )
                        "low" -> Triple(
                            Icons.Default.Battery2Bar,
                            stringResource(R.string.devices_charge_low),
                            Warning,
                        )
                        "critical" -> Triple(
                            Icons.Default.BatteryAlert,
                            stringResource(R.string.devices_charge_critical),
                            Danger,
                        )
                        "charging" -> Triple(
                            Icons.Default
                                .BatteryChargingFull,
                            stringResource(R.string.devices_charge_charging),
                            MaterialTheme.colorScheme
                                .primary,
                        )
                        else -> Triple(null, null, null)
                    }
                if (chargeIcon != null &&
                    chargeLabel != null &&
                    chargeColor != null
                ) {
                    Row(
                        verticalAlignment =
                            Alignment.CenterVertically,
                    ) {
                        Icon(
                            chargeIcon,
                            contentDescription = null,
                            modifier =
                                Modifier.size(14.dp),
                            tint = chargeColor,
                        )
                        Spacer(
                            modifier =
                                Modifier.width(4.dp),
                        )
                        Text(
                            text = chargeLabel,
                            style = MaterialTheme
                                .typography.bodySmall,
                            color = chargeColor,
                        )
                    }
                }
                if (device.localStatus == "issued") {
                    device.employeeName?.let {
                        Text(
                            text = stringResource(
                                R.string.devices_issued_to,
                                it,
                            ),
                            style = MaterialTheme
                                .typography
                                .bodySmall,
                            color = MaterialTheme
                                .colorScheme
                                .tertiary,
                        )
                    }
                }
            }
            StatusBadge(device.localStatus)
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
            title = stringResource(R.string.devices_empty),
            icon = Icons.Default.Watch,
            modifier = modifier.fillMaxSize(),
        )
        return
    }

    Card(
        modifier = modifier.fillMaxSize(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = device.deviceId,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
            device.serialNumber?.let {
                Text(
                    text = stringResource(R.string.devices_serial, it),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            device.model?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            device.employeeName?.let {
                Text(
                    text = stringResource(R.string.devices_issued_to, it),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = stringResource(R.string.devices_status_issued),
                    style = MaterialTheme.typography.labelLarge,
                )
                StatusBadge(device.localStatus)
            }
        }
    }
}

@Composable
private fun StatusBadge(status: String) {
    val (text, color) = when (status) {
        "available" ->
            stringResource(R.string.devices_status_available) to Success
        "issued" ->
            stringResource(R.string.devices_status_issued) to Warning
        "discharged" ->
            stringResource(R.string.devices_status_discharged) to Danger
        "faulty" ->
            stringResource(R.string.devices_status_faulty) to Color(0xFF9E9E9E)
        else -> status to Color.Gray
    }
    Card(
        colors = CardDefaults.cardColors(
            containerColor = color.copy(alpha = 0.12f),
        ),
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(
                horizontal = 10.dp,
                vertical = 4.dp,
            ),
            style = MaterialTheme.typography
                .labelSmall,
            fontWeight = FontWeight.Medium,
            color = color,
        )
    }
}
