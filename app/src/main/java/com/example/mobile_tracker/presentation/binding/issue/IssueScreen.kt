package com.example.mobile_tracker.presentation.binding.issue

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Watch
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.mobile_tracker.domain.model.Device
import com.example.mobile_tracker.domain.model.Employee
import org.koin.androidx.compose.koinViewModel

@Composable
fun IssueScreen(
    viewModel: IssueViewModel = koinViewModel(),
) {
    val state by
        viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is IssueEffect.ShowSuccess -> { }
                is IssueEffect.ShowError -> { }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
    ) {
        if (state.step != IssueStep.IDENTIFY_EMPLOYEE) {
            IconButton(
                onClick = {
                    viewModel.onIntent(IssueIntent.GoBack)
                },
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Назад",
                )
            }
        }

        StepIndicator(currentStep = state.step)

        Spacer(modifier = Modifier.height(16.dp))

        AnimatedContent(
            targetState = state.step,
            label = "issue_step",
        ) { step ->
            when (step) {
                IssueStep.IDENTIFY_EMPLOYEE ->
                    IdentifyEmployeeContent(
                        state = state,
                        onIntent = viewModel::onIntent,
                    )
                IssueStep.SELECT_DEVICE ->
                    SelectDeviceContent(
                        state = state,
                        onIntent = viewModel::onIntent,
                    )
                IssueStep.CONFIRM ->
                    ConfirmContent(
                        state = state,
                        onIntent = viewModel::onIntent,
                    )
            }
        }
    }
}

@Composable
private fun StepIndicator(currentStep: IssueStep) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        IssueStep.entries.forEach { step ->
            val isActive = step == currentStep
            val isDone = step.ordinal < currentStep.ordinal
            Text(
                text = when (step) {
                    IssueStep.IDENTIFY_EMPLOYEE ->
                        "1. Сотрудник"
                    IssueStep.SELECT_DEVICE ->
                        "2. Часы"
                    IssueStep.CONFIRM ->
                        "3. Выдача"
                },
                style = MaterialTheme.typography.labelLarge,
                color = when {
                    isActive ->
                        MaterialTheme.colorScheme.primary
                    isDone ->
                        MaterialTheme.colorScheme.primary
                            .copy(alpha = 0.6f)
                    else ->
                        MaterialTheme.colorScheme
                            .onSurfaceVariant
                },
            )
        }
    }
}

@Composable
private fun IdentifyEmployeeContent(
    state: IssueState,
    onIntent: (IssueIntent) -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        OutlinedTextField(
            value = state.personnelQuery,
            onValueChange = {
                onIntent(
                    IssueIntent.UpdatePersonnelQuery(it),
                )
            },
            label = { Text("Табельный номер") },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Number,
                imeAction = ImeAction.Search,
            ),
            keyboardActions = KeyboardActions(
                onSearch = {
                    onIntent(IssueIntent.SearchByPersonnel)
                },
            ),
            trailingIcon = {
                IconButton(
                    onClick = {
                        onIntent(
                            IssueIntent.SearchByPersonnel,
                        )
                    },
                ) {
                    Icon(
                        Icons.Default.Search,
                        contentDescription = "Найти",
                    )
                }
            },
            singleLine = true,
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = state.nameQuery,
            onValueChange = {
                onIntent(
                    IssueIntent.UpdateNameQuery(it),
                )
            },
            label = { Text("Поиск по ФИО") },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(
                imeAction = ImeAction.Search,
            ),
            keyboardActions = KeyboardActions(
                onSearch = {
                    onIntent(IssueIntent.SearchByName)
                },
            ),
            trailingIcon = {
                IconButton(
                    onClick = {
                        onIntent(IssueIntent.SearchByName)
                    },
                ) {
                    Icon(
                        Icons.Default.Search,
                        contentDescription = "Найти",
                    )
                }
            },
            singleLine = true,
        )

        Spacer(modifier = Modifier.height(8.dp))

        if (state.isSearching) {
            CircularProgressIndicator(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(16.dp),
            )
        }

        if (state.error != null) {
            Text(
                text = state.error,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(vertical = 8.dp),
            )
        }

        LazyColumn {
            items(
                items = state.searchResults,
                key = { it.id },
            ) { employee ->
                EmployeeCard(
                    employee = employee,
                    onClick = {
                        onIntent(
                            IssueIntent.SelectEmployee(
                                employee,
                            ),
                        )
                    },
                )
            }
        }
    }
}

@Composable
private fun EmployeeCard(
    employee: Employee,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp,
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Default.Person,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = employee.fullName,
                    style =
                        MaterialTheme.typography.titleMedium,
                )
                if (employee.personnelNumber != null) {
                    Text(
                        text = "Таб. № " +
                            employee.personnelNumber,
                        style = MaterialTheme.typography
                            .bodySmall,
                        color = MaterialTheme.colorScheme
                            .onSurfaceVariant,
                    )
                }
                if (employee.position != null) {
                    Text(
                        text = employee.position,
                        style = MaterialTheme.typography
                            .bodySmall,
                    )
                }
                if (employee.brigadeName != null) {
                    Text(
                        text = "Бригада: " +
                            employee.brigadeName,
                        style = MaterialTheme.typography
                            .bodySmall,
                    )
                }
            }
        }
    }
}

@Composable
private fun SelectDeviceContent(
    state: IssueState,
    onIntent: (IssueIntent) -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        state.selectedEmployee?.let { emp ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme
                        .colorScheme.primaryContainer,
                ),
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment =
                        Alignment.CenterVertically,
                ) {
                    Icon(
                        Icons.Default.Person,
                        contentDescription = null,
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = emp.fullName,
                            style = MaterialTheme.typography
                                .titleSmall,
                        )
                        if (emp.personnelNumber != null) {
                            Text(
                                text = "Таб. № " +
                                    emp.personnelNumber,
                                style = MaterialTheme
                                    .typography.bodySmall,
                            )
                        }
                    }
                }
            }
        }

        Text(
            text = "Выберите часы " +
                "(${state.availableDevices.size} " +
                "свободных)",
            style = MaterialTheme.typography.titleMedium,
        )

        Spacer(modifier = Modifier.height(8.dp))

        if (state.isLoading) {
            CircularProgressIndicator(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(16.dp),
            )
        }

        if (state.error != null) {
            Text(
                text = state.error,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(vertical = 8.dp),
            )
        }

        LazyColumn(modifier = Modifier.weight(1f)) {
            items(
                items = state.availableDevices,
                key = { it.deviceId },
            ) { device ->
                DeviceCard(
                    device = device,
                    isSelected =
                        state.selectedDevice?.deviceId ==
                            device.deviceId,
                    onClick = {
                        onIntent(
                            IssueIntent.SelectDevice(device),
                        )
                    },
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (state.selectedDevice != null) {
            Button(
                onClick = {
                    onIntent(IssueIntent.AutoAssignDevice)
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Далее")
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
            .padding(vertical = 4.dp)
            .clickable(onClick = onClick),
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
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Default.Watch,
                contentDescription = null,
                tint = if (isSelected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme
                        .onSurfaceVariant
                },
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = device.deviceId,
                    style =
                        MaterialTheme.typography.titleMedium,
                )
                if (device.model != null) {
                    Text(
                        text = device.model,
                        style = MaterialTheme.typography
                            .bodySmall,
                    )
                }
                if (device.serialNumber != null) {
                    Text(
                        text = "S/N: " +
                            device.serialNumber,
                        style = MaterialTheme.typography
                            .bodySmall,
                        color = MaterialTheme.colorScheme
                            .onSurfaceVariant,
                    )
                }
            }
            if (isSelected) {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = "Выбрано",
                    tint =
                        MaterialTheme.colorScheme.primary,
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
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "Подтверждение выдачи",
            style = MaterialTheme.typography.headlineSmall,
        )

        Spacer(modifier = Modifier.height(16.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme
                    .colorScheme.surfaceVariant,
            ),
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
            ) {
                Text(
                    text = "Сотрудник",
                    style = MaterialTheme.typography
                        .labelMedium,
                    color = MaterialTheme.colorScheme
                        .onSurfaceVariant,
                )
                Text(
                    text = state.selectedEmployee
                        ?.fullName ?: "",
                    style = MaterialTheme.typography
                        .titleMedium,
                )
                state.selectedEmployee
                    ?.personnelNumber?.let {
                        Text(
                            text = "Таб. № $it",
                            style = MaterialTheme.typography
                                .bodySmall,
                        )
                    }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Часы",
                    style = MaterialTheme.typography
                        .labelMedium,
                    color = MaterialTheme.colorScheme
                        .onSurfaceVariant,
                )
                Text(
                    text = state.selectedDevice
                        ?.deviceId ?: "",
                    style = MaterialTheme.typography
                        .titleMedium,
                )
                state.selectedDevice?.model?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography
                            .bodySmall,
                    )
                }
            }
        }

        if (state.validationError != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = state.validationError,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        FilledTonalButton(
            onClick = {
                onIntent(IssueIntent.ConfirmIssue)
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            enabled = !state.isIssuing,
        ) {
            if (state.isIssuing) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .height(24.dp)
                        .width(24.dp),
                )
            } else {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = null,
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Выдать часы",
                    style = MaterialTheme.typography
                        .titleMedium,
                )
            }
        }
    }
}
