package com.example.mobile_tracker.presentation.binding.return_device

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Construction
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Watch
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.mobile_tracker.R
import com.example.mobile_tracker.domain.model.DeviceBinding
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
import kotlinx.coroutines.flow.collect
import org.koin.androidx.compose.koinViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ReturnScreen(
    onBack: (() -> Unit)? = null,
    onCompleted: () -> Unit = {},
    scannedDeviceId: String? = null,
    onOpenQrScan: () -> Unit = {},
    viewModel: ReturnViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val isTablet = rememberIsTablet()
    val selectedBinding = state.activeBindings.firstOrNull { it.id == state.selectedBindingId }

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is ReturnEffect.ShowSuccess -> onCompleted()
                is ReturnEffect.ShowError -> Unit
            }
        }
    }

    LaunchedEffect(scannedDeviceId, state.activeBindings) {
        if (!scannedDeviceId.isNullOrBlank()) {
            viewModel.onIntent(ReturnIntent.ApplyScannedDevice(scannedDeviceId))
        }
    }

    if (state.showConfirmWithoutUpload) {
        AlertDialog(
            onDismissRequest = { viewModel.onIntent(ReturnIntent.DismissConfirmDialog) },
            title = { Text(stringResource(R.string.return_data_not_uploaded_title)) },
            text = { Text(stringResource(R.string.return_data_not_uploaded_message)) },
            confirmButton = {
                Button(onClick = { viewModel.onIntent(ReturnIntent.ConfirmReturnWithoutUpload) }) {
                    Text(stringResource(R.string.return_continue))
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.onIntent(ReturnIntent.DismissConfirmDialog) }) {
                    Text(stringResource(R.string.return_cancel))
                }
            },
        )
    }

    if (state.showProblemDialog) {
        ReturnProblemDialog(
            selectedReason = state.selectedProblemReason,
            comment = state.problemComment,
            onReasonSelected = {
                viewModel.onIntent(ReturnIntent.SelectProblemReason(it))
            },
            onCommentChanged = {
                viewModel.onIntent(ReturnIntent.UpdateProblemComment(it))
            },
            onDismiss = {
                viewModel.onIntent(ReturnIntent.DismissProblemDialog)
            },
            onConfirm = {
                viewModel.onIntent(ReturnIntent.ConfirmProblemReturn)
            },
        )
    }

    AppScreenScaffold(
        snackbarMessage = state.error,
        topBar = {
            MTCompactTopBar(
                title = stringResource(R.string.return_title),
                subtitle = stringResource(R.string.return_issued_count, state.activeBindings.size),
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
            ReturnSummaryCard(
                totalCount = state.activeBindings.size,
                selectedBinding = selectedBinding,
                onOpenQrScan = onOpenQrScan,
            )

            if (state.error != null) {
                StateCard(message = state.error!!, isError = true)
            }

            Box(modifier = Modifier.weight(1f)) {
                AdaptiveListDetail(
                    isTablet = isTablet,
                    listPane = { paneModifier ->
                        when {
                            state.isLoading -> LoadingState(modifier = paneModifier)
                            state.activeBindings.isEmpty() -> EmptyState(
                                title = stringResource(R.string.return_empty),
                                icon = Icons.Default.Replay,
                                modifier = paneModifier,
                            )
                            else -> LazyColumn(
                                modifier = paneModifier,
                                verticalArrangement = Arrangement.spacedBy(AppSpacing.xs),
                            ) {
                                items(state.activeBindings, key = { it.id }) { binding ->
                                    BindingCard(
                                        binding = binding,
                                        isSelected = state.selectedBindingId == binding.id,
                                        isReturning = state.isReturning &&
                                            state.selectedBindingId == binding.id,
                                        showInlineActions = !isTablet,
                                        onSelect = {
                                            viewModel.onIntent(ReturnIntent.SelectBinding(binding))
                                        },
                                        onReturn = { viewModel.onIntent(ReturnIntent.ConfirmReturn) },
                                        onOpenProblemFlow = {
                                            viewModel.onIntent(ReturnIntent.OpenProblemFlow(binding))
                                        },
                                    )
                                }
                                item { Spacer(modifier = Modifier.height(AppSpacing.lg)) }
                            }
                        }
                    },
                    detailPane = { paneModifier ->
                        ReturnDetailPane(
                            modifier = paneModifier.padding(start = AppSpacing.sm),
                            binding = selectedBinding,
                            isReturning = state.isReturning,
                            onReturn = { viewModel.onIntent(ReturnIntent.ConfirmReturn) },
                            onOpenProblemFlow = {
                                selectedBinding?.let {
                                    viewModel.onIntent(ReturnIntent.OpenProblemFlow(it))
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
private fun ReturnSummaryCard(
    totalCount: Int,
    selectedBinding: DeviceBinding?,
    onOpenQrScan: () -> Unit,
) {
    MTCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(AppSpacing.xxs),
            ) {
                Text(
                    text = stringResource(R.string.return_summary_title),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = totalCount.toString(),
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = stringResource(R.string.home_return_subtitle),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            MTStatusBadge(
                label = stringResource(R.string.return_summary_status),
                tone = MTStatusTone.Warning,
            )
        }

        if (selectedBinding != null) {
            Spacer(modifier = Modifier.height(AppSpacing.xs))
            Surface(
                shape = RoundedCornerShape(AppRadius.pill),
                color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.45f),
            ) {
                Text(
                    text = stringResource(
                        R.string.return_selected_binding,
                        selectedBinding.deviceId,
                        selectedBinding.employeeName,
                    ),
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.tertiary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        Spacer(modifier = Modifier.height(AppSpacing.xs))
        Button(
            onClick = onOpenQrScan,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(AppRadius.xl),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainer,
                contentColor = MaterialTheme.colorScheme.onSurface,
            ),
        ) {
            Icon(
                imageVector = Icons.Default.QrCodeScanner,
                contentDescription = null,
            )
            Spacer(modifier = Modifier.size(AppSpacing.xs))
            Text(text = stringResource(R.string.return_scan_qr))
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
    onOpenProblemFlow: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .semantics { selected = isSelected }
            .clickable(
                onClickLabel = stringResource(R.string.action_open_return_details),
                role = Role.Button,
                onClick = onSelect,
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
            modifier = Modifier.padding(AppLayout.cardPadding),
            verticalArrangement = Arrangement.spacedBy(AppSpacing.sm),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.55f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Default.Watch,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.tertiary,
                    )
                }
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Text(
                        text = binding.deviceId,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = binding.employeeName,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = stringResource(R.string.return_issued_at, formatTime(binding.boundAt)),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Column(
                    verticalArrangement = Arrangement.spacedBy(AppSpacing.xxs),
                    horizontalAlignment = Alignment.End,
                ) {
                    SyncBadge(isSynced = binding.isSynced)
                    UploadBadge(dataUploaded = binding.dataUploaded)
                }
            }

            if (isSelected && showInlineActions) {
                ReturnActionRow(
                    isReturning = isReturning,
                    onReturn = onReturn,
                    onOpenProblemFlow = onOpenProblemFlow,
                )
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
    onOpenProblemFlow: () -> Unit,
) {
    if (binding == null) {
        EmptyState(
            title = stringResource(R.string.return_detail_empty),
            icon = Icons.Default.Replay,
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
                        .background(MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.55f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Default.Watch,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.tertiary,
                    )
                }
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Text(
                        text = binding.deviceId,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = binding.employeeName,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            ReturnDetailRow(
                label = stringResource(R.string.return_detail_employee),
                value = binding.employeeName,
            )
            ReturnDetailRow(
                label = stringResource(R.string.return_detail_issued_at),
                value = formatTime(binding.boundAt),
            )
            ReturnDetailRow(
                label = stringResource(R.string.return_detail_shift),
                value = "${binding.shiftDate} · ${binding.shiftType}",
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(AppSpacing.xs),
            ) {
                SyncBadge(isSynced = binding.isSynced)
                UploadBadge(dataUploaded = binding.dataUploaded)
            }

            Spacer(modifier = Modifier.weight(1f))

            ReturnActionRow(
                isReturning = isReturning,
                onReturn = onReturn,
                onOpenProblemFlow = onOpenProblemFlow,
            )
        }
    }
}

@Composable
private fun ReturnActionRow(
    isReturning: Boolean,
    onReturn: () -> Unit,
    onOpenProblemFlow: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(AppSpacing.xs),
    ) {
        Button(
            onClick = onReturn,
            modifier = Modifier
                .weight(1f)
                .height(52.dp),
            enabled = !isReturning,
            shape = RoundedCornerShape(AppRadius.xl),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ),
        ) {
            if (isReturning) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary,
                )
            } else {
                Text(stringResource(R.string.return_button))
            }
        }
        OutlinedButton(
            onClick = onOpenProblemFlow,
            modifier = Modifier.height(52.dp),
            enabled = !isReturning,
            shape = RoundedCornerShape(AppRadius.xl),
        ) {
            Icon(
                imageVector = Icons.Default.Construction,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
            )
            Spacer(modifier = Modifier.size(AppSpacing.xs))
            Text(stringResource(R.string.return_mark_problem))
        }
    }
}

@Composable
private fun ReturnProblemDialog(
    selectedReason: ReturnProblemReason,
    comment: String,
    onReasonSelected: (ReturnProblemReason) -> Unit,
    onCommentChanged: (String) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.return_problem_title)) },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(AppSpacing.sm),
            ) {
                Text(
                    text = stringResource(R.string.return_problem_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Column(
                    verticalArrangement = Arrangement.spacedBy(AppSpacing.xs),
                ) {
                    ReturnProblemReason.entries.forEach { reason ->
                        ProblemReasonRow(
                            reason = reason,
                            selected = reason == selectedReason,
                            onClick = { onReasonSelected(reason) },
                        )
                    }
                }
                OutlinedTextField(
                    value = comment,
                    onValueChange = onCommentChanged,
                    minLines = 3,
                    modifier = Modifier.fillMaxWidth(),
                    label = {
                        Text(stringResource(R.string.return_problem_comment_label))
                    },
                    placeholder = {
                        Text(stringResource(R.string.return_problem_comment_placeholder))
                    },
                    shape = RoundedCornerShape(AppRadius.lg),
                )
            }
        },
        confirmButton = {
            Button(onClick = onConfirm) {
                Text(stringResource(R.string.return_problem_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.return_cancel))
            }
        },
    )
}

@Composable
private fun ProblemReasonRow(
    reason: ReturnProblemReason,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        shape = RoundedCornerShape(AppRadius.lg),
        color = if (selected) {
            MaterialTheme.colorScheme.surfaceContainer
        } else {
            MaterialTheme.colorScheme.surfaceContainerLowest
        },
        border = if (selected) {
            BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary.copy(alpha = 0.35f))
        } else {
            null
        },
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = problemReasonLabel(reason),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = problemReasonDescription(reason),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun SyncBadge(isSynced: Boolean) {
    MTStatusBadge(
        label = if (isSynced) {
            stringResource(R.string.binding_synced)
        } else {
            stringResource(R.string.binding_pending_sync)
        },
        tone = if (isSynced) MTStatusTone.Neutral else MTStatusTone.Warning,
    )
}

@Composable
private fun UploadBadge(dataUploaded: Boolean) {
    MTStatusBadge(
        label = if (dataUploaded) {
            stringResource(R.string.return_data_uploaded)
        } else {
            stringResource(R.string.return_data_pending)
        },
        tone = if (dataUploaded) MTStatusTone.Success else MTStatusTone.Danger,
    )
}

@Composable
private fun ReturnDetailRow(
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
private fun problemReasonLabel(reason: ReturnProblemReason): String = when (reason) {
    ReturnProblemReason.Lost -> stringResource(R.string.return_problem_lost)
    ReturnProblemReason.Faulty -> stringResource(R.string.return_problem_faulty)
    ReturnProblemReason.NoConnection -> stringResource(R.string.return_problem_no_connection)
    ReturnProblemReason.Other -> stringResource(R.string.return_problem_other)
}

@Composable
private fun problemReasonDescription(reason: ReturnProblemReason): String = when (reason) {
    ReturnProblemReason.Lost -> stringResource(R.string.return_problem_lost_desc)
    ReturnProblemReason.Faulty -> stringResource(R.string.return_problem_faulty_desc)
    ReturnProblemReason.NoConnection -> stringResource(R.string.return_problem_no_connection_desc)
    ReturnProblemReason.Other -> stringResource(R.string.return_problem_other_desc)
}

private fun formatTime(timestampMs: Long): String {
    val sdf = SimpleDateFormat("HH:mm", Locale("ru"))
    return sdf.format(Date(timestampMs))
}
