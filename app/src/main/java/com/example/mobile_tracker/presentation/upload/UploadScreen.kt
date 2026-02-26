package com.example.mobile_tracker.presentation.upload

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BluetoothSearching
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.mobile_tracker.data.ble.BlePermissionManager
import org.koin.androidx.compose.koinViewModel

@Composable
fun UploadScreen(
    deviceId: String = "",
    employeeId: String? = null,
    employeeName: String? = null,
    bindingId: Long? = null,
    viewModel: UploadViewModel = koinViewModel(),
) {
    val state by viewModel.state
        .collectAsStateWithLifecycle()
    val context = LocalContext.current

    val permissionLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts
                .RequestMultiplePermissions(),
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
        if (deviceId.isNotBlank() &&
            state.step == UploadStep.Idle
        ) {
            if (BlePermissionManager.hasPermissions(
                    context,
                )
            ) {
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
                    BlePermissionManager
                        .requiredPermissions()
                        .toTypedArray(),
                )
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.Center,
    ) {
        when (state.step) {
            UploadStep.Idle -> _IdleContent(
                deviceId = deviceId,
                onStart = {
                    if (BlePermissionManager
                            .hasPermissions(context)
                    ) {
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
                            BlePermissionManager
                                .requiredPermissions()
                                .toTypedArray(),
                        )
                    }
                },
            )
            UploadStep.Error -> _ErrorContent(
                error = state.error ?: "Неизвестная ошибка",
                onRetry = {
                    viewModel.onIntent(UploadIntent.Retry)
                },
                onCancel = {
                    viewModel.onIntent(UploadIntent.Cancel)
                },
            )
            UploadStep.Done -> _DoneContent(
                isServerUploaded = state.isServerUploaded,
                packetId = state.packetId,
            )
            else -> _ProgressContent(state = state)
        }
    }
}

@Composable
private fun _IdleContent(
    deviceId: String,
    onStart: () -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector =
                Icons.Default.BluetoothSearching,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.primary,
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Выгрузка данных",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
        )
        Spacer(modifier = Modifier.height(8.dp))
        if (deviceId.isNotBlank()) {
            Text(
                text = "Устройство: $deviceId",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme
                    .onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(24.dp))
        }
        Button(onClick = onStart) {
            Icon(
                imageVector = Icons.Default.CloudUpload,
                contentDescription = null,
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Начать выгрузку")
        }
    }
}

@Composable
private fun _ProgressContent(state: UploadState) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 4.dp,
        ),
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment =
                Alignment.CenterHorizontally,
        ) {
            CircularProgressIndicator()
            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = stepLabel(state.step),
                style =
                    MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )

            Spacer(modifier = Modifier.height(8.dp))

            if (state.deviceId.isNotBlank()) {
                Text(
                    text = state.deviceId,
                    style =
                        MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme
                        .onSurfaceVariant,
                )
            }

            AnimatedVisibility(
                visible = state.step ==
                    UploadStep.ReadingChunks &&
                    state.totalChunks > 0,
            ) {
                Column(
                    modifier = Modifier.padding(top = 16.dp),
                    horizontalAlignment =
                        Alignment.CenterHorizontally,
                ) {
                    val progress =
                        if (state.totalChunks > 0) {
                            state.chunksReceived.toFloat() /
                                state.totalChunks
                        } else {
                            0f
                        }
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(
                        modifier = Modifier.height(8.dp),
                    )
                    Text(
                        text = "${state.chunksReceived}" +
                            " / ${state.totalChunks} " +
                            "чанков",
                        style = MaterialTheme.typography
                            .bodySmall,
                    )
                    Text(
                        text = "${
                            (progress * 100).toInt()
                        }%",
                        style = MaterialTheme.typography
                            .titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme
                            .primary,
                    )
                }
            }
        }
    }
}

@Composable
private fun _ErrorContent(
    error: String,
    onRetry: () -> Unit,
    onCancel: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme
                .errorContainer,
        ),
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment =
                Alignment.CenterHorizontally,
        ) {
            Icon(
                imageVector = Icons.Default.Error,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.error,
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Ошибка выгрузки",
                style =
                    MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme
                    .onErrorContainer,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = error,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme
                    .onErrorContainer,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(24.dp))
            Row(
                horizontalArrangement =
                    Arrangement.spacedBy(12.dp),
            ) {
                OutlinedButton(onClick = onCancel) {
                    Text("Отмена")
                }
                Button(onClick = onRetry) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = null,
                    )
                    Spacer(
                        modifier = Modifier.width(8.dp),
                    )
                    Text("Повторить")
                }
            }
        }
    }
}

@Composable
private fun _DoneContent(
    isServerUploaded: Boolean,
    packetId: String?,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme
                .primaryContainer,
        ),
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment =
                Alignment.CenterHorizontally,
        ) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Данные выгружены",
                style =
                    MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme
                    .onPrimaryContainer,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = if (isServerUploaded) {
                    "Отправлено на сервер"
                } else {
                    "Сохранено локально, " +
                        "будет отправлено при " +
                        "подключении к сети"
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme
                    .onPrimaryContainer,
                textAlign = TextAlign.Center,
            )
            if (packetId != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "ID: $packetId",
                    style =
                        MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme
                        .onPrimaryContainer
                        .copy(alpha = 0.7f),
                )
            }
        }
    }
}

private fun stepLabel(step: UploadStep): String =
    when (step) {
        UploadStep.Idle -> "Ожидание"
        UploadStep.Scanning ->
            "Поиск часов..."
        UploadStep.Connecting ->
            "Подключение к часам..."
        UploadStep.ReadingMeta ->
            "Чтение метаданных..."
        UploadStep.ReadingChunks ->
            "Считывание данных..."
        UploadStep.Verifying ->
            "Проверка целостности..."
        UploadStep.SendingAck ->
            "Подтверждение приёма..."
        UploadStep.SavingLocally ->
            "Сохранение..."
        UploadStep.UploadingToServer ->
            "Отправка на сервер..."
        UploadStep.Done -> "Готово"
        UploadStep.Error -> "Ошибка"
    }
