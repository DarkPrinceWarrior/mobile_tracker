package com.example.mobile_tracker.presentation.context_selection

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContextSelectionScreen(
    onContextSelected: () -> Unit,
    viewModel: ContextSelectionViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                ContextSelectionEffect.NavigateToHome ->
                    onContextSelected()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Выбор контекста работы") },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Площадка",
                style = MaterialTheme.typography.titleMedium,
            )

            var isExpanded by remember { mutableStateOf(false) }

            ExposedDropdownMenuBox(
                expanded = isExpanded,
                onExpandedChange = { isExpanded = it },
            ) {
                OutlinedTextField(
                    value = state.selectedSite?.name ?: "",
                    onValueChange = {},
                    readOnly = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(MenuAnchorType.PrimaryEditable),
                    trailingIcon = {
                        ExposedDropdownMenuDefaults
                            .TrailingIcon(expanded = isExpanded)
                    },
                    label = { Text("Выберите площадку") },
                )

                ExposedDropdownMenu(
                    expanded = isExpanded,
                    onDismissRequest = { isExpanded = false },
                ) {
                    state.sites.forEach { site ->
                        DropdownMenuItem(
                            text = { Text(site.name) },
                            onClick = {
                                viewModel.onIntent(
                                    ContextSelectionIntent
                                        .SiteSelected(site),
                                )
                                isExpanded = false
                            },
                        )
                    }
                }
            }

            Text(
                text = "Дата смены",
                style = MaterialTheme.typography.titleMedium,
            )

            OutlinedTextField(
                value = state.shiftDate,
                onValueChange = {
                    viewModel.onIntent(
                        ContextSelectionIntent.DateChanged(it),
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("ГГГГ-ММ-ДД") },
                singleLine = true,
            )

            Text(
                text = "Тип смены",
                style = MaterialTheme.typography.titleMedium,
            )

            Row(
                horizontalArrangement =
                    Arrangement.spacedBy(12.dp),
            ) {
                FilterChip(
                    selected = state.shiftType == "day",
                    onClick = {
                        viewModel.onIntent(
                            ContextSelectionIntent
                                .ShiftTypeChanged("day"),
                        )
                    },
                    label = { Text("Дневная") },
                )

                FilterChip(
                    selected = state.shiftType == "night",
                    onClick = {
                        viewModel.onIntent(
                            ContextSelectionIntent
                                .ShiftTypeChanged("night"),
                        )
                    },
                    label = { Text("Ночная") },
                )
            }

            if (state.error != null) {
                Text(
                    text = state.error!!,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = {
                    viewModel.onIntent(
                        ContextSelectionIntent.StartWork,
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                enabled = !state.isLoading
                    && state.selectedSite != null,
            ) {
                Text(
                    text = "Начать работу",
                    style = MaterialTheme.typography.titleMedium,
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
