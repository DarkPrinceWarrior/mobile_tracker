package com.example.mobile_tracker.presentation.upload

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.BluetoothSearching
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.mobile_tracker.R
import com.example.mobile_tracker.data.ble.BlePermissionManager
import com.example.mobile_tracker.presentation.common.AppScreenScaffold
import com.example.mobile_tracker.presentation.common.MTCompactTopBar
import com.example.mobile_tracker.presentation.common.MTStatusBadge
import com.example.mobile_tracker.presentation.common.MTStatusTone
import com.example.mobile_tracker.presentation.common.StateCard
import com.example.mobile_tracker.ui.theme.AppLayout
import com.example.mobile_tracker.ui.theme.AppRadius
import com.example.mobile_tracker.ui.theme.AppSpacing
import org.koin.androidx.compose.koinViewModel

@Composable
fun UploadScreen(
    deviceId: String = "",
    employeeId: String? = null,
    employeeName: String? = null,
    bindingId: Long? = null,
    onBack: (() -> Unit)? = null,
    viewModel: UploadViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { results ->
        if (results.values.all { it }) {
            viewModel.onIntent(
                UploadIntent.StartUpload(
                    deviceId = deviceId,
                    employeeId = employeeId,
                    employeeName = employeeName,
                    bindingId = bindingId,
                ),
            )
        }
    }

    LaunchedEffect(deviceId) {
        if (deviceId.isNotBlank() && state.step == UploadStep.Idle) {
            if (BlePermissionManager.hasPermissions(context)) {
                viewModel.onIntent(
                    UploadIntent.StartUpload(
                        deviceId = deviceId,
                        employeeId = employeeId,
                        employeeName = employeeName,
                        bindingId = bindingId,
                    ),
                )
            } else {
                permissionLauncher.launch(BlePermissionManager.requiredPermissions().toTypedArray())
            }
        }
    }

    AppScreenScaffold(
        snackbarMessage = state.error,
        topBar = {
            MTCompactTopBar(
                title = stringResource(R.string.tab_upload),
                subtitle = if (deviceId.isNotBlank()) {
                    stringResource(R.string.upload_device_label, deviceId)
                } else {
                    stringResource(R.string.upload_data_title)
                },
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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = AppLayout.screenPadding, vertical = AppSpacing.sm),
            contentAlignment = Alignment.Center,
        ) {
            when (state.step) {
                UploadStep.Idle -> IdleUploadContent(
                    deviceId = deviceId,
                    employeeName = employeeName,
                    onStart = {
                        if (BlePermissionManager.hasPermissions(context)) {
                            viewModel.onIntent(
                                UploadIntent.StartUpload(
                                    deviceId = deviceId,
                                    employeeId = employeeId,
                                    employeeName = employeeName,
                                    bindingId = bindingId,
                                ),
                            )
                        } else {
                            permissionLauncher.launch(
                                BlePermissionManager.requiredPermissions().toTypedArray(),
                            )
                        }
                    },
                )
                UploadStep.Error -> ErrorUploadContent(
                    error = state.error ?: stringResource(R.string.error_unknown),
                    onRetry = { viewModel.onIntent(UploadIntent.Retry) },
                    onCancel = { viewModel.onIntent(UploadIntent.Cancel) },
                )
                UploadStep.Done -> DoneUploadContent(
                    isServerUploaded = state.isServerUploaded,
                    packetId = state.packetId,
                )
                else -> ProgressUploadContent(state = state)
            }
        }
    }
}

@Composable
private fun IdleUploadContent(
    deviceId: String,
    employeeName: String?,
    onStart: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(AppRadius.xl),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primary,
        ),
    ) {
        Column(
            modifier = Modifier.padding(AppSpacing.lg),
            verticalArrangement = Arrangement.spacedBy(AppSpacing.md),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                MTStatusBadge(
                    label = stringResource(R.string.upload_idle_status),
                    tone = MTStatusTone.Success,
                )
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.BluetoothSearching,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary,
                    )
                }
            }

            Text(
                text = stringResource(R.string.upload_data_title),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onPrimary,
            )
            Text(
                text = stringResource(R.string.home_upload_subtitle),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.74f),
            )

            if (deviceId.isNotBlank()) {
                UploadMetaPill(label = stringResource(R.string.upload_device_label, deviceId))
            }
            if (!employeeName.isNullOrBlank()) {
                UploadMetaPill(label = employeeName)
            }

            Button(
                onClick = onStart,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape = RoundedCornerShape(AppRadius.xl),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.onPrimary,
                    contentColor = MaterialTheme.colorScheme.primary,
                ),
            ) {
                Icon(imageVector = Icons.Default.CloudUpload, contentDescription = null)
                Spacer(modifier = Modifier.size(AppSpacing.xs))
                Text(text = stringResource(R.string.upload_start_button))
            }
        }
    }
}

@Composable
private fun ProgressUploadContent(state: UploadState) {
    Card(
        modifier = Modifier.fillMaxWidth(),
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
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                MTStatusBadge(
                    label = stringResource(R.string.upload_progress_status),
                    tone = MTStatusTone.Warning,
                )
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.55f)),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.tertiary,
                    )
                }
            }

            Text(
                text = stepLabel(state.step),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
            if (state.deviceId.isNotBlank()) {
                Text(
                    text = state.deviceId,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            UploadStepTimeline(currentStep = state.step)

            AnimatedVisibility(
                visible = state.step == UploadStep.ReadingChunks && state.totalChunks > 0,
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.xs)) {
                    val progress = if (state.totalChunks > 0) {
                        state.chunksReceived.toFloat() / state.totalChunks
                    } else {
                        0f
                    }
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.tertiary,
                        trackColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            text = stringResource(
                                R.string.upload_chunks_progress,
                                state.chunksReceived,
                                state.totalChunks,
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            text = "${(progress * 100).toInt()}%",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.tertiary,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ErrorUploadContent(
    error: String,
    onRetry: () -> Unit,
    onCancel: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(AppSpacing.md),
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(AppRadius.xl),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
            ),
        ) {
            Column(
                modifier = Modifier.padding(AppSpacing.lg),
                verticalArrangement = Arrangement.spacedBy(AppSpacing.sm),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.8f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Default.Error,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                    )
                }
                Text(
                    text = stringResource(R.string.upload_error_title),
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }

        StateCard(message = error, isError = true)

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(AppSpacing.xs),
        ) {
            OutlinedButton(
                onClick = onCancel,
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp),
                shape = RoundedCornerShape(AppRadius.xl),
            ) {
                Text(stringResource(R.string.return_cancel))
            }
            Button(
                onClick = onRetry,
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp),
                shape = RoundedCornerShape(AppRadius.xl),
            ) {
                Icon(imageVector = Icons.Default.Refresh, contentDescription = null)
                Spacer(modifier = Modifier.size(AppSpacing.xs))
                Text(stringResource(R.string.upload_retry_button))
            }
        }
    }
}

@Composable
private fun DoneUploadContent(
    isServerUploaded: Boolean,
    packetId: String?,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(AppRadius.xl),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
        ),
    ) {
        Column(
            modifier = Modifier.padding(AppSpacing.lg),
            verticalArrangement = Arrangement.spacedBy(AppSpacing.md),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.55f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.tertiary,
                )
            }
            MTStatusBadge(
                label = stringResource(R.string.upload_done_status),
                tone = MTStatusTone.Success,
            )
            Text(
                text = stringResource(R.string.upload_done_title),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = if (isServerUploaded) {
                    stringResource(R.string.upload_done_server)
                } else {
                    stringResource(R.string.upload_done_local)
                },
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            if (packetId != null) {
                UploadMetaPill(label = stringResource(R.string.upload_packet_id, packetId))
            }
        }
    }
}

@Composable
private fun UploadMetaPill(label: String) {
    Surface(
        shape = RoundedCornerShape(AppRadius.pill),
        color = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.18f),
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onPrimary,
        )
    }
}

@Composable
private fun UploadStepTimeline(currentStep: UploadStep) {
    Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.xs)) {
        uploadStepsProgression.forEach { step ->
            val state = when {
                step.ordinal < currentStep.ordinal -> MTStatusTone.Success
                step == currentStep -> MTStatusTone.Warning
                else -> MTStatusTone.Neutral
            }
            Surface(
                shape = RoundedCornerShape(AppRadius.lg),
                color = MaterialTheme.colorScheme.surfaceContainer,
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(AppSpacing.xs),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    MTStatusBadge(label = stepOrder(step), tone = state)
                    Text(
                        text = stepLabel(step),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        }
    }
}

private val uploadStepsProgression = listOf(
    UploadStep.Scanning,
    UploadStep.Connecting,
    UploadStep.ReadingMeta,
    UploadStep.ReadingChunks,
    UploadStep.Verifying,
    UploadStep.SendingAck,
    UploadStep.SavingLocally,
    UploadStep.UploadingToServer,
)

@Composable
private fun stepOrder(step: UploadStep): String = when (step) {
    UploadStep.Scanning -> "1"
    UploadStep.Connecting -> "2"
    UploadStep.ReadingMeta -> "3"
    UploadStep.ReadingChunks -> "4"
    UploadStep.Verifying -> "5"
    UploadStep.SendingAck -> "6"
    UploadStep.SavingLocally -> "7"
    UploadStep.UploadingToServer -> "8"
    UploadStep.Idle -> "0"
    UploadStep.Done -> "9"
    UploadStep.Error -> "!"
}

@Composable
private fun stepLabel(step: UploadStep): String = when (step) {
    UploadStep.Idle -> stringResource(R.string.upload_step_idle)
    UploadStep.Scanning -> stringResource(R.string.upload_step_scanning)
    UploadStep.Connecting -> stringResource(R.string.upload_step_connecting)
    UploadStep.ReadingMeta -> stringResource(R.string.upload_step_meta)
    UploadStep.ReadingChunks -> stringResource(R.string.upload_step_chunks)
    UploadStep.Verifying -> stringResource(R.string.upload_step_verifying)
    UploadStep.SendingAck -> stringResource(R.string.upload_step_ack)
    UploadStep.SavingLocally -> stringResource(R.string.upload_step_saving)
    UploadStep.UploadingToServer -> stringResource(R.string.upload_step_uploading)
    UploadStep.Done -> stringResource(R.string.upload_step_done)
    UploadStep.Error -> stringResource(R.string.upload_step_error)
}
