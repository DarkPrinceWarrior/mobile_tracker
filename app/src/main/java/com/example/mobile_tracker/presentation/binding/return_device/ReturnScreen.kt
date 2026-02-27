package com.example.mobile_tracker.presentation.binding.return_device

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Watch
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.mobile_tracker.R
import com.example.mobile_tracker.domain.model.DeviceBinding
import com.example.mobile_tracker.presentation.common.AdaptiveListDetail
import com.example.mobile_tracker.presentation.common.EmptyState
import com.example.mobile_tracker.presentation.common.LoadingState
import com.example.mobile_tracker.presentation.common.StateCard
import com.example.mobile_tracker.presentation.common.rememberIsTablet
import org.koin.androidx.compose.koinViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReturnScreen(
    onBack: (() -> Unit)? = null,
    viewModel: ReturnViewModel = koinViewModel(),
) {
    val state by
        viewModel.state.collectAsStateWithLifecycle()
    val isTablet = rememberIsTablet()
    val selectedBinding = state.activeBindings.firstOrNull {
        it.id == state.selectedBindingId
    }

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is ReturnEffect.ShowSuccess -> { }
                is ReturnEffect.ShowError -> { }
            }
        }
    }

    if (state.showConfirmWithoutUpload) {
        AlertDialog(
            onDismissRequest = {
                viewModel.onIntent(
                    ReturnIntent.DismissConfirmDialog,
                )
            },
            title = {
                Text(
                    stringResource(
                        R.string.return_data_not_uploaded_title,
                    ),
                )
            },
            text = {
                Text(
                    stringResource(
                        R.string.return_data_not_uploaded_message,
                    ),
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.onIntent(
                            ReturnIntent
                                .ConfirmReturnWithoutUpload,
                        )
                    },
                ) {
                    Text(stringResource(R.string.return_continue))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        viewModel.onIntent(
                            ReturnIntent
                                .DismissConfirmDialog,
                        )
                    },
                ) {
                    Text(stringResource(R.string.return_cancel))
                }
            },
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = stringResource(R.string.return_title),
                            fontWeight =
                                FontWeight.SemiBold,
                        )
                        Text(
                            text = stringResource(
                                R.string.return_issued_count,
                                state.activeBindings.size,
                            ),
                            style = MaterialTheme
                                .typography.bodySmall,
                            color = MaterialTheme
                                .colorScheme
                                .onSurfaceVariant,
                        )
                    }
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
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
        ) {
            if (state.error != null) {
                StateCard(
                    message = state.error!!,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
            }

            Box(modifier = Modifier.weight(1f)) {
                AdaptiveListDetail(
                    isTablet = isTablet,
                    listPane = { paneModifier ->
                        if (state.isLoading) {
                            LoadingState(modifier = paneModifier)
                        } else if (state.activeBindings.isEmpty()) {
                            EmptyState(
                                title = stringResource(R.string.return_empty),
                                icon = Icons.Default.Replay,
                                modifier = paneModifier,
                            )
                        } else {
                            LazyColumn(
                                modifier = paneModifier,
                                verticalArrangement =
                                    Arrangement.spacedBy(8.dp),
                            ) {
                                items(
                                    items = state.activeBindings,
                                    key = { it.id },
                                ) { binding ->
                                    BindingCard(
                                        binding = binding,
                                        isSelected =
                                            state.selectedBindingId == binding.id,
                                        isReturning =
                                            state.isReturning &&
                                                state.selectedBindingId == binding.id,
                                        showInlineActions = !isTablet,
                                        onSelect = {
                                            viewModel.onIntent(
                                                ReturnIntent
                                                    .SelectBinding(
                                                        binding,
                                                    ),
                                            )
                                        },
                                        onReturn = {
                                            viewModel.onIntent(
                                                ReturnIntent
                                                    .ConfirmReturn,
                                            )
                                        },
                                        onMarkLost = {
                                            viewModel.onIntent(
                                                ReturnIntent
                                                    .MarkLost(
                                                        binding,
                                                    ),
                                            )
                                        },
                                    )
                                }
                                item {
                                    Spacer(
                                        modifier =
                                            Modifier.height(16.dp),
                                    )
                                }
                            }
                        }
                    },
                    detailPane = { paneModifier ->
                        ReturnDetailPane(
                            modifier = paneModifier.padding(horizontal = 12.dp),
                            binding = selectedBinding,
                            isReturning = state.isReturning,
                            onReturn = {
                                viewModel.onIntent(ReturnIntent.ConfirmReturn)
                            },
                            onMarkLost = {
                                selectedBinding?.let {
                                    viewModel.onIntent(
                                        ReturnIntent.MarkLost(it),
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
private fun BindingCard(
    binding: DeviceBinding,
    isSelected: Boolean,
    isReturning: Boolean,
    showInlineActions: Boolean,
    onSelect: () -> Unit,
    onReturn: () -> Unit,
    onMarkLost: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onSelect),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme
                    .secondaryContainer
            } else {
                MaterialTheme.colorScheme.surface
            },
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isSelected) {
                4.dp
            } else {
                1.dp
            },
        ),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment =
                    Alignment.CenterVertically,
            ) {
                Icon(
                    Icons.Default.Watch,
                    contentDescription = null,
                    tint =
                        MaterialTheme.colorScheme.primary,
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = binding.deviceId,
                        style = MaterialTheme.typography
                            .titleMedium,
                    )
                    Text(
                        text = binding.employeeName,
                        style = MaterialTheme.typography
                            .bodyMedium,
                    )
                    Text(
                        text = stringResource(
                            R.string.return_issued_at,
                            formatTime(binding.boundAt),
                        ),
                        style = MaterialTheme.typography
                            .bodySmall,
                        color = MaterialTheme.colorScheme
                            .onSurfaceVariant,
                    )
                }
                SyncStatusIcon(
                    isSynced = binding.isSynced,
                )
                Spacer(modifier = Modifier.width(4.dp))
                DataStatusIcon(
                    dataUploaded = binding.dataUploaded,
                )
            }

            if (isSelected && showInlineActions) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement =
                        Arrangement.spacedBy(8.dp),
                ) {
                    FilledTonalButton(
                        onClick = onReturn,
                        modifier = Modifier.weight(1f),
                        enabled = !isReturning,
                    ) {
                        if (isReturning) {
                            CircularProgressIndicator(
                                modifier = Modifier
                                    .height(20.dp)
                                    .width(20.dp),
                            )
                        } else {
                            Text(stringResource(R.string.return_button))
                        }
                    }
                    OutlinedButton(
                        onClick = onMarkLost,
                        enabled = !isReturning,
                    ) {
                        Text(stringResource(R.string.return_mark_lost))
                    }
                }
            }
        }
    }
}

@Composable
private fun ReturnDetailPane(
    modifier: Modifier,
    binding: DeviceBinding?,
    isReturning: Boolean,
    onReturn: () -> Unit,
    onMarkLost: () -> Unit,
) {
    if (binding == null) {
        EmptyState(
            title = stringResource(R.string.return_empty),
            icon = Icons.Default.Replay,
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
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = binding.deviceId,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = binding.employeeName,
                style = MaterialTheme.typography.bodyLarge,
            )
            Text(
                text = stringResource(
                    R.string.return_issued_at,
                    formatTime(binding.boundAt),
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SyncStatusIcon(isSynced = binding.isSynced)
                DataStatusIcon(dataUploaded = binding.dataUploaded)
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FilledTonalButton(
                    onClick = onReturn,
                    modifier = Modifier.weight(1f),
                    enabled = !isReturning,
                ) {
                    Text(stringResource(R.string.return_button))
                }
                OutlinedButton(
                    onClick = onMarkLost,
                    enabled = !isReturning,
                ) {
                    Text(stringResource(R.string.return_mark_lost))
                }
            }
        }
    }
}

@Composable
private fun SyncStatusIcon(isSynced: Boolean) {
    if (isSynced) {
        Icon(
            Icons.Default.CloudDone,
            contentDescription = stringResource(R.string.binding_synced),
            tint = MaterialTheme.colorScheme
                .onSurfaceVariant
                .copy(alpha = 0.5f),
        )
    } else {
        Icon(
            Icons.Default.CloudOff,
            contentDescription =
                stringResource(R.string.binding_pending_sync),
            tint = MaterialTheme.colorScheme.tertiary,
        )
    }
}

@Composable
private fun DataStatusIcon(dataUploaded: Boolean) {
    if (dataUploaded) {
        Icon(
            Icons.Default.CheckCircle,
            contentDescription = stringResource(R.string.return_data_uploaded),
            tint = MaterialTheme.colorScheme.primary,
        )
    } else {
        Icon(
            Icons.Default.Warning,
            contentDescription = stringResource(R.string.return_data_pending),
            tint = MaterialTheme.colorScheme.error,
        )
    }
}

private fun formatTime(timestampMs: Long): String {
    val sdf = SimpleDateFormat(
        "HH:mm",
        Locale("ru"),
    )
    return sdf.format(Date(timestampMs))
}
