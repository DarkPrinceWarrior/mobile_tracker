package com.example.mobile_tracker.presentation.register_watch

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Watch
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.mobile_tracker.R
import com.example.mobile_tracker.domain.model.Employee
import com.example.mobile_tracker.presentation.common.AppScreenScaffold
import com.example.mobile_tracker.presentation.common.MTCard
import com.example.mobile_tracker.presentation.common.MTCompactTopBar
import com.example.mobile_tracker.presentation.common.StateCard
import com.example.mobile_tracker.ui.theme.AppLayout
import com.example.mobile_tracker.ui.theme.AppRadius
import com.example.mobile_tracker.ui.theme.AppSpacing
import com.example.mobile_tracker.ui.theme.success
import kotlinx.coroutines.flow.collect
import org.koin.androidx.compose.koinViewModel

@Composable
fun RegisterWatchScreen(
    scannedValue: String,
    onBack: () -> Unit,
    onCompleted: () -> Unit = {},
    viewModel: RegisterWatchViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val focusManager = LocalFocusManager.current

    // Employee pending confirmation in dialog
    var pendingEmployee by remember { mutableStateOf<Employee?>(null) }

    LaunchedEffect(scannedValue) {
        if (scannedValue.isNotBlank()) {
            viewModel.applyScannedData(scannedValue)
        }
    }

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is RegisterWatchEffect.ShowSuccess -> Unit
                is RegisterWatchEffect.ShowError -> Unit
            }
        }
    }

    AppScreenScaffold(
        snackbarMessage = state.error,
        topBar = {
            MTCompactTopBar(
                title = stringResource(R.string.register_watch_title),
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.action_back),
                            tint = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                },
            )
        },
    ) { padding ->
        AnimatedContent(
            targetState = state.isRegistered,
            label = "register_watch_content",
            transitionSpec = { fadeIn() togetherWith fadeOut() },
        ) { registered ->
            if (registered) {
                SuccessContent(
                    deviceId = state.deviceId,
                    employeeName = state.registeredEmployeeName,
                    onDone = onCompleted,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(horizontal = AppLayout.screenPadding, vertical = AppSpacing.sm),
                )
            } else {
                SelectEmployeeContent(
                    state = state,
                    onSearchChange = { viewModel.updateSearchQuery(it) },
                    onEmployeeClick = { employee ->
                        focusManager.clearFocus()
                        pendingEmployee = employee
                    },
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                )
            }
        }

        // Confirmation dialog
        pendingEmployee?.let { emp ->
            ConfirmBindingDialog(
                deviceId = state.deviceId,
                employee = emp,
                isRegistering = state.isRegistering,
                onConfirm = {
                    viewModel.registerWithEmployee(emp)
                    pendingEmployee = null
                },
                onDismiss = { pendingEmployee = null },
            )
        }
    }
}

@Composable
private fun SelectEmployeeContent(
    state: RegisterWatchState,
    onSearchChange: (String) -> Unit,
    onEmployeeClick: (Employee) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = AppLayout.screenPadding)
                .padding(top = AppSpacing.sm),
            verticalArrangement = Arrangement.spacedBy(AppSpacing.sm),
        ) {
            DeviceInfoCard(state = state)

            OutlinedTextField(
                value = state.searchQuery,
                onValueChange = onSearchChange,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = {
                    Text(text = stringResource(R.string.register_watch_search_label))
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                },
                shape = RoundedCornerShape(AppRadius.lg),
            )
        }

        if (state.isLoadingEmployees) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = AppSpacing.lg),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.tertiary)
            }
        } else if (state.filteredEmployees.isEmpty()) {
            StateCard(
                message = if (state.searchQuery.isNotBlank()) {
                    "По запросу «${state.searchQuery}» ничего не найдено"
                } else {
                    "Список сотрудников пуст"
                },
                isError = false,
                modifier = Modifier.padding(
                    horizontal = AppLayout.screenPadding,
                    vertical = AppSpacing.sm,
                ),
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(top = AppSpacing.xs),
                verticalArrangement = Arrangement.spacedBy(AppSpacing.xxs),
            ) {
                items(state.filteredEmployees, key = { it.id }) { employee ->
                    EmployeeResultCard(
                        employee = employee,
                        onClick = { onEmployeeClick(employee) },
                        modifier = Modifier.padding(horizontal = AppLayout.screenPadding),
                    )
                }
            }
        }
    }
}

@Composable
private fun DeviceInfoCard(state: RegisterWatchState) {
    MTCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Default.Watch,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp),
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = stringResource(R.string.register_watch_device_title),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = state.deviceId.ifBlank { stringResource(R.string.register_watch_no_device) },
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold,
                )
                state.model?.let {
                    Text(
                        text = stringResource(R.string.register_watch_model) + ": $it",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                state.firmware?.let {
                    Text(
                        text = stringResource(R.string.register_watch_firmware) + ": $it",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun EmployeeResultCard(
    employee: Employee,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(
                onClickLabel = stringResource(R.string.action_select_employee),
                role = Role.Button,
                onClick = onClick,
            ),
        shape = RoundedCornerShape(AppRadius.lg),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(AppLayout.cardPadding),
            horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.55f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.size(20.dp),
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = employee.fullName,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Medium,
                )
                val secondaryLine = listOfNotNull(employee.position, employee.brigadeName)
                    .joinToString(" · ")
                if (secondaryLine.isNotBlank()) {
                    Text(
                        text = secondaryLine,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ConfirmBindingDialog(
    deviceId: String,
    employee: Employee,
    isRegistering: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    BasicAlertDialog(
        onDismissRequest = { if (!isRegistering) onDismiss() },
    ) {
        Card(
            shape = RoundedCornerShape(AppRadius.xl),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface,
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = AppSpacing.md),
                verticalArrangement = Arrangement.spacedBy(AppSpacing.sm),
            ) {
                // Title
                Text(
                    text = stringResource(R.string.register_watch_confirm_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(horizontal = AppSpacing.lg),
                )

                // Binding summary card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = AppSpacing.md),
                    shape = RoundedCornerShape(AppRadius.lg),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.5f),
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        // Device row
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = AppSpacing.md, vertical = AppSpacing.sm),
                            horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(
                                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                                    ),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Watch,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(17.dp),
                                )
                            }
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(1.dp),
                            ) {
                                Text(
                                    text = stringResource(R.string.register_watch_device_title),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Text(
                                    text = deviceId,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                            }
                        }

                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = AppSpacing.md),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                        )

                        // Employee row
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = AppSpacing.md, vertical = AppSpacing.sm),
                            horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(
                                        MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.6f),
                                    ),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.tertiary,
                                    modifier = Modifier.size(17.dp),
                                )
                            }
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(1.dp),
                            ) {
                                Text(
                                    text = stringResource(R.string.register_watch_summary_employee),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Text(
                                    text = employee.fullName,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                employee.position?.let {
                                    Text(
                                        text = it,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        }
                    }
                }

                // Equal-width Yes / No buttons
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = AppSpacing.md)
                        .padding(top = AppSpacing.xs),
                    horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm),
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        enabled = !isRegistering,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(AppRadius.lg),
                    ) {
                        Text(
                            text = "Нет",
                            fontWeight = FontWeight.Medium,
                        )
                    }
                    Button(
                        onClick = onConfirm,
                        enabled = !isRegistering,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(AppRadius.lg),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                        ),
                    ) {
                        if (isRegistering) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary,
                            )
                        } else {
                            Text(
                                text = "Да",
                                fontWeight = FontWeight.Medium,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SuccessContent(
    deviceId: String,
    employeeName: String,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(AppSpacing.lg),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(modifier = Modifier.height(AppSpacing.xl))

        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.success.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.success,
            )
        }

        Text(
            text = stringResource(R.string.register_watch_success_title),
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )

        Text(
            text = stringResource(R.string.register_watch_success_message, deviceId, employeeName),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )




        Spacer(modifier = Modifier.weight(1f))

        Button(
            onClick = onDone,
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp),
            shape = RoundedCornerShape(AppRadius.xl),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ),
        ) {
            Text(
                text = stringResource(R.string.register_watch_done),
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}
