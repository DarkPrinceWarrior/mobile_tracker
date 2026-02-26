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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Watch
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.mobile_tracker.domain.model.DeviceBinding
import org.koin.androidx.compose.koinViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ReturnScreen(
    viewModel: ReturnViewModel = koinViewModel(),
) {
    val state by
        viewModel.state.collectAsStateWithLifecycle()

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
            title = { Text("Данные не выгружены") },
            text = {
                Text(
                    "Данные с часов ещё не выгружены. " +
                        "Продолжить возврат без выгрузки?",
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
                    Text("Продолжить")
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
                    Text("Отмена")
                }
            },
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
    ) {
        Text(
            text = "Возврат часов",
            style = MaterialTheme.typography.headlineSmall,
        )
        Text(
            text = "Выданных: " +
                "${state.activeBindings.size}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme
                .onSurfaceVariant,
        )

        Spacer(modifier = Modifier.height(12.dp))

        if (state.error != null) {
            Text(
                text = state.error!!,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(bottom = 8.dp),
            )
        }

        if (state.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }
        } else if (state.activeBindings.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "Нет выданных часов",
                    style = MaterialTheme.typography
                        .bodyLarge,
                    color = MaterialTheme.colorScheme
                        .onSurfaceVariant,
                )
            }
        } else {
            LazyColumn(
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
                            state.selectedBinding?.id ==
                                binding.id,
                        isReturning = state.isReturning &&
                            state.selectedBinding?.id ==
                            binding.id,
                        onSelect = {
                            viewModel.onIntent(
                                ReturnIntent.SelectBinding(
                                    binding,
                                ),
                            )
                        },
                        onReturn = {
                            viewModel.onIntent(
                                ReturnIntent.ConfirmReturn,
                            )
                        },
                        onMarkLost = {
                            viewModel.onIntent(
                                ReturnIntent.MarkLost(
                                    binding,
                                ),
                            )
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun BindingCard(
    binding: DeviceBinding,
    isSelected: Boolean,
    isReturning: Boolean,
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
                        text = "Выдано: " +
                            formatTime(binding.boundAt),
                        style = MaterialTheme.typography
                            .bodySmall,
                        color = MaterialTheme.colorScheme
                            .onSurfaceVariant,
                    )
                }
                DataStatusIcon(
                    dataUploaded = binding.dataUploaded,
                )
            }

            if (isSelected) {
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
                            Text("Принять часы")
                        }
                    }
                    OutlinedButton(
                        onClick = onMarkLost,
                        enabled = !isReturning,
                    ) {
                        Text("Потеряны")
                    }
                }
            }
        }
    }
}

@Composable
private fun DataStatusIcon(dataUploaded: Boolean) {
    if (dataUploaded) {
        Icon(
            Icons.Default.CheckCircle,
            contentDescription = "Данные выгружены",
            tint = MaterialTheme.colorScheme.primary,
        )
    } else {
        Icon(
            Icons.Default.Warning,
            contentDescription = "Данные не выгружены",
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
