package com.example.mobile_tracker.presentation.devices

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material.icons.filled.Battery2Bar
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Watch
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.mobile_tracker.domain.model.Device
import com.example.mobile_tracker.ui.theme.Success
import com.example.mobile_tracker.ui.theme.Warning
import com.example.mobile_tracker.ui.theme.Danger
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeviceListScreen(
    viewModel: DeviceListViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Часы на площадке",
                        fontWeight = FontWeight.SemiBold,
                    )
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
                                    "Обновить",
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
                .padding(horizontal = 16.dp),
        ) {
            Spacer(modifier = Modifier.height(4.dp))

            OutlinedTextField(
                value = state.searchQuery,
                onValueChange = {
                    viewModel.onIntent(
                        DeviceListIntent.Search(it),
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.small,
                placeholder = {
                    Text("Поиск по ID или серийному №")
                },
                leadingIcon = {
                    Icon(
                        Icons.Default.Search,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme
                            .onSurfaceVariant,
                    )
                },
                singleLine = true,
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
                            "Все (${state.totalCount})",
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
                            "Свободны " +
                                "(${state.availableCount})",
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
                            "Выданы " +
                                "(${state.issuedCount})",
                        )
                    },
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            when {
                state.isLoading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize(),
                        contentAlignment =
                            Alignment.Center,
                    ) {
                        CircularProgressIndicator()
                    }
                }

                state.error != null -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize(),
                        contentAlignment =
                            Alignment.Center,
                    ) {
                        Card(
                            colors =
                                CardDefaults.cardColors(
                                    containerColor =
                                        MaterialTheme
                                            .colorScheme
                                            .errorContainer,
                                ),
                        ) {
                            Text(
                                text = state.error ?: "",
                                color = MaterialTheme
                                    .colorScheme
                                    .onErrorContainer,
                                modifier = Modifier
                                    .padding(16.dp),
                            )
                        }
                    }
                }

                state.devices.isEmpty() -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize(),
                        contentAlignment =
                            Alignment.Center,
                    ) {
                        Column(
                            horizontalAlignment =
                                Alignment
                                    .CenterHorizontally,
                        ) {
                            Icon(
                                Icons.Default.Watch,
                                contentDescription =
                                    null,
                                modifier = Modifier
                                    .size(48.dp),
                                tint = MaterialTheme
                                    .colorScheme
                                    .outlineVariant,
                            )
                            Spacer(
                                modifier = Modifier
                                    .height(8.dp),
                            )
                            Text(
                                "Нет часов " +
                                    "для отображения",
                                style = MaterialTheme
                                    .typography
                                    .bodyLarge,
                                color = MaterialTheme
                                    .colorScheme
                                    .onSurfaceVariant,
                            )
                        }
                    }
                }

                else -> {
                    LazyColumn(
                        verticalArrangement =
                            Arrangement.spacedBy(
                                8.dp,
                            ),
                    ) {
                        items(
                            state.devices,
                            key = { it.deviceId },
                        ) { device ->
                            DeviceCard(device)
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
        }
    }
}

@Composable
private fun DeviceCard(device: Device) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme
                .surfaceContainerLow,
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
                        text = "S/N: $it",
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
                            "Заряжен",
                            Success,
                        )
                        "low" -> Triple(
                            Icons.Default.Battery2Bar,
                            "Низкий заряд",
                            Warning,
                        )
                        "critical" -> Triple(
                            Icons.Default.BatteryAlert,
                            "Критический",
                            Danger,
                        )
                        "charging" -> Triple(
                            Icons.Default
                                .BatteryChargingFull,
                            "Заряжается",
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
                            text = "Выдан: $it",
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
private fun StatusBadge(status: String) {
    val (text, color) = when (status) {
        "available" -> "Свободен" to Success
        "issued" -> "Выдан" to Warning
        "discharged" -> "Разряжен" to Danger
        "faulty" -> "Неисправен" to Color(0xFF9E9E9E)
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
