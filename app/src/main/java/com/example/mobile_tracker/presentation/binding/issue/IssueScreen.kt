package com.example.mobile_tracker.presentation.binding.issue

import androidx.compose.animation.AnimatedContent
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Watch
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.mobile_tracker.R
import com.example.mobile_tracker.domain.model.Device
import com.example.mobile_tracker.domain.model.Employee
import com.example.mobile_tracker.presentation.common.AppScreenScaffold
import com.example.mobile_tracker.presentation.common.MTCard
import com.example.mobile_tracker.presentation.common.MTCompactTopBar
import com.example.mobile_tracker.presentation.common.MTStatusBadge
import com.example.mobile_tracker.presentation.common.MTStatusTone
import com.example.mobile_tracker.presentation.common.StateCard
import com.example.mobile_tracker.ui.theme.AppLayout
import com.example.mobile_tracker.ui.theme.AppRadius
import com.example.mobile_tracker.ui.theme.AppSpacing
import kotlinx.coroutines.flow.collect
import org.koin.androidx.compose.koinViewModel

@Composable
fun IssueScreen(
    onBack: (() -> Unit)? = null,
    onCompleted: () -> Unit = {},
    scannedDeviceId: String? = null,
    onOpenQrScan: () -> Unit = {},
    viewModel: IssueViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is IssueEffect.ShowSuccess -> onCompleted()
                is IssueEffect.ShowError -> Unit
            }
        }
    }

    LaunchedEffect(scannedDeviceId, state.availableDevices, state.step) {
        if (!scannedDeviceId.isNullOrBlank() && state.step != IssueStep.IDENTIFY_EMPLOYEE) {
            viewModel.onIntent(IssueIntent.ApplyScannedDevice(scannedDeviceId))
        }
    }

    AppScreenScaffold(
        snackbarMessage = state.validationError,
        topBar = {
            MTCompactTopBar(
                title = stringResource(R.string.issue_title),
                navigationIcon = {
                    val backAction: (() -> Unit)? = when {
                        state.step != IssueStep.IDENTIFY_EMPLOYEE -> {
                            { viewModel.onIntent(IssueIntent.GoBack) }
                        }
                        onBack != null -> onBack
                        else -> null
                    }
                    if (backAction != null) {
                        IconButton(onClick = backAction) {
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
            IssueStepIndicator(currentStep = state.step)

            Box(modifier = Modifier.weight(1f)) {
                AnimatedContent(
                    targetState = state.step,
                    label = "issue_step",
                ) { step ->
                    when (step) {
                        IssueStep.IDENTIFY_EMPLOYEE -> IdentifyEmployeeContent(
                            state = state,
                            onIntent = viewModel::onIntent,
                        )
                        IssueStep.SELECT_DEVICE -> SelectDeviceContent(
                            state = state,
                            onIntent = viewModel::onIntent,
                        )
                        IssueStep.CONFIRM -> ConfirmContent(
                            state = state,
                            onIntent = viewModel::onIntent,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun IssueStepIndicator(currentStep: IssueStep) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(AppSpacing.xs),
    ) {
        IssueStep.entries.forEach { step ->
            val selected = step == currentStep
            val completed = step.ordinal < currentStep.ordinal
            Surface(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(AppRadius.lg),
                color = when {
                    selected -> MaterialTheme.colorScheme.primary
                    completed -> MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.55f)
                    else -> MaterialTheme.colorScheme.surfaceContainer
                },
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stepLabel(step),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (selected) {
                            MaterialTheme.colorScheme.onPrimary
                        } else if (completed) {
                            MaterialTheme.colorScheme.onSurface
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        maxLines = 1,
                        overflow = TextOverflow.Clip,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }
}

@Composable
private fun IdentifyEmployeeContent(
    state: IssueState,
    onIntent: (IssueIntent) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(AppSpacing.sm),
    ) {
        OutlinedTextField(
            value = state.searchQuery,
            onValueChange = { onIntent(IssueIntent.UpdateSearchQuery(it)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            label = {
                Text(text = stringResource(R.string.issue_search_employee_label))
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

        if (state.isLoadingEmployees) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = AppSpacing.sm),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.tertiary,
                )
            }
        } else if (state.error != null) {
            StateCard(message = state.error, isError = true)
        } else if (state.filteredEmployees.isEmpty()) {
            StateCard(
                message = if (state.searchQuery.isNotBlank()) {
                    "По запросу «${state.searchQuery}» ничего не найдено"
                } else {
                    "Список сотрудников пуст"
                },
                isError = false,
            )
        }

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(AppSpacing.xs),
        ) {
            items(state.filteredEmployees, key = { it.id }) { employee ->
                IssueEmployeeResultCard(
                    employee = employee,
                    onClick = { onIntent(IssueIntent.SelectEmployee(employee)) },
                )
            }
        }
    }
}

@Composable
private fun IssueEmployeeResultCard(
    employee: Employee,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier
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
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = employee.fullName,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                employee.personnelNumber?.let {
                    Text(
                        text = stringResource(R.string.issue_personnel_short, it),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                val secondaryLine = listOfNotNull(employee.position, employee.brigadeName).joinToString(" · ")
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
            )
        }
    }
}

@Composable
private fun SelectDeviceContent(
    state: IssueState,
    onIntent: (IssueIntent) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(AppSpacing.sm),
    ) {
        state.selectedEmployee?.let { emp ->
            SelectionSummaryCard(
                title = emp.fullName,
                subtitle = emp.personnelNumber?.let {
                    stringResource(R.string.issue_personnel_short, it)
                } ?: emp.position.orEmpty(),
            )
        }

        if (state.validationError != null) {
            StateCard(message = state.validationError, isError = true)
        }

        OutlinedTextField(
            value = state.deviceSearchQuery,
            onValueChange = { onIntent(IssueIntent.UpdateDeviceSearchQuery(it)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            label = {
                Text(text = stringResource(R.string.devices_search_hint))
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

        if (state.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = AppSpacing.sm),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.tertiary,
                )
            }
        }

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(AppSpacing.xs),
        ) {
            if (!state.isLoading && state.filteredDevices.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(AppRadius.lg),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
                        ),
                    ) {
                        Text(
                            text = if (state.deviceSearchQuery.isNotBlank()) {
                                "По запросу «${state.deviceSearchQuery}» ничего не найдено"
                            } else {
                                "Нет доступных часов"
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(AppLayout.cardPadding),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            } else {
                items(state.filteredDevices, key = { it.deviceId }) { device ->
                    IssueDeviceResultCard(
                        device = device,
                        isSelected = state.selectedDevice?.deviceId == device.deviceId,
                        onClick = { onIntent(IssueIntent.SelectDevice(device)) },
                    )
                }
            }
        }

        Button(
            onClick = { onIntent(IssueIntent.ContinueWithSelectedDevice) },
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding(),
            enabled = state.selectedDevice != null && !state.isLoading,
            shape = RoundedCornerShape(AppRadius.xl),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ),
            contentPadding = ButtonDefaults.ContentPadding,
        ) {
            if (state.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary,
                )
            } else {
                Text(text = stringResource(R.string.issue_next))
            }
        }
    }
}

@Composable
private fun IssueDeviceResultCard(
    device: Device,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .semantics { selected = isSelected }
            .clickable(
                onClickLabel = stringResource(R.string.action_select_device),
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
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(AppLayout.cardPadding),
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
                    imageVector = Icons.Default.Watch,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.tertiary,
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
                device.model?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                device.serialNumber?.let {
                    Text(
                        text = stringResource(R.string.devices_serial, it),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            if (isSelected) {
                MTStatusBadge(
                    label = stringResource(R.string.issue_selected),
                    tone = MTStatusTone.Success,
                )
            }
        }
    }
}

@Composable
private fun ConfirmContent(
    state: IssueState,
    onIntent: (IssueIntent) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(AppSpacing.md),
    ) {
        SelectionSummaryCard(
            title = state.selectedEmployee?.fullName.orEmpty(),
            subtitle = state.selectedEmployee?.personnelNumber?.let {
                stringResource(R.string.issue_personnel_short, it)
            } ?: "",
        )
        SelectionSummaryCard(
            title = state.selectedDevice?.deviceId.orEmpty(),
            subtitle = state.selectedDevice?.model ?: state.selectedDevice?.serialNumber.orEmpty(),
        )

        if (state.validationError != null) {
            StateCard(message = state.validationError, isError = true)
        }

        Spacer(modifier = Modifier.weight(1f))

        Button(
            onClick = { onIntent(IssueIntent.ConfirmIssue) },
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding(),
            enabled = !state.isIssuing,
            shape = RoundedCornerShape(AppRadius.xl),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ),
            contentPadding = ButtonDefaults.ContentPadding,
        ) {
            if (state.isIssuing) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary,
                )
            } else {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                )
                Spacer(modifier = Modifier.width(AppSpacing.xs))
                Text(text = stringResource(R.string.issue_button))
            }
        }
    }
}

@Composable
private fun SelectionSummaryCard(
    title: String,
    subtitle: String,
) {
    MTCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                if (subtitle.isNotBlank()) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun stepLabel(step: IssueStep): String = when (step) {
    IssueStep.IDENTIFY_EMPLOYEE -> stringResource(R.string.issue_step_employee_short)
    IssueStep.SELECT_DEVICE -> stringResource(R.string.issue_step_device_short)
    IssueStep.CONFIRM -> stringResource(R.string.issue_step_confirm_short)
}
