package com.example.mobile_tracker.presentation.context_selection

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
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
    val scrollBehavior =
        TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                ContextSelectionEffect.NavigateToHome ->
                    onContextSelected()
            }
        }
    }

    Scaffold(
        modifier = Modifier.nestedScroll(
            scrollBehavior.nestedScrollConnection,
        ),
        topBar = {
            LargeTopAppBar(
                title = {
                    Text(
                        "Выбор контекста\nработы",
                        fontWeight = FontWeight.Bold,
                    )
                },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults
                    .largeTopAppBarColors(
                        containerColor = MaterialTheme
                            .colorScheme.surface,
                        scrolledContainerColor =
                            MaterialTheme.colorScheme
                                .surfaceContainer,
                    ),
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            verticalArrangement =
                Arrangement.spacedBy(16.dp),
        ) {
            Spacer(modifier = Modifier.height(4.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme
                        .colorScheme
                        .surfaceContainerLow,
                ),
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement =
                        Arrangement.spacedBy(12.dp),
                ) {
                    Row(
                        verticalAlignment =
                            Alignment.CenterVertically,
                    ) {
                        Icon(
                            Icons.Default.LocationOn,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme
                                .primary,
                            modifier = Modifier.size(20.dp),
                        )
                        Spacer(
                            modifier = Modifier.width(8.dp),
                        )
                        Text(
                            text = "Площадка",
                            style = MaterialTheme.typography
                                .titleMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }

                    var isExpanded by remember {
                        mutableStateOf(false)
                    }

                    ExposedDropdownMenuBox(
                        expanded = isExpanded,
                        onExpandedChange = {
                            isExpanded = it
                        },
                    ) {
                        OutlinedTextField(
                            value = state.selectedSite
                                ?.name ?: "",
                            onValueChange = {},
                            readOnly = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(
                                    MenuAnchorType
                                        .PrimaryEditable,
                                ),
                            shape = MaterialTheme
                                .shapes.small,
                            trailingIcon = {
                                ExposedDropdownMenuDefaults
                                    .TrailingIcon(
                                        expanded =
                                            isExpanded,
                                    )
                            },
                            label = {
                                Text("Выберите площадку")
                            },
                        )

                        ExposedDropdownMenu(
                            expanded = isExpanded,
                            onDismissRequest = {
                                isExpanded = false
                            },
                        ) {
                            state.sites.forEach { site ->
                                DropdownMenuItem(
                                    text = {
                                        Text(site.name)
                                    },
                                    onClick = {
                                        viewModel.onIntent(
                                            ContextSelectionIntent
                                                .SiteSelected(
                                                    site,
                                                ),
                                        )
                                        isExpanded = false
                                    },
                                )
                            }
                        }
                    }
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme
                        .colorScheme
                        .surfaceContainerLow,
                ),
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement =
                        Arrangement.spacedBy(12.dp),
                ) {
                    Row(
                        verticalAlignment =
                            Alignment.CenterVertically,
                    ) {
                        Icon(
                            Icons.Default.CalendarMonth,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme
                                .primary,
                            modifier = Modifier.size(20.dp),
                        )
                        Spacer(
                            modifier = Modifier.width(8.dp),
                        )
                        Text(
                            text = "Дата смены",
                            style = MaterialTheme.typography
                                .titleMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }

                    OutlinedTextField(
                        value = state.shiftDate,
                        onValueChange = {
                            viewModel.onIntent(
                                ContextSelectionIntent
                                    .DateChanged(it),
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape =
                            MaterialTheme.shapes.small,
                        label = { Text("ГГГГ-ММ-ДД") },
                        singleLine = true,
                    )
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme
                        .colorScheme
                        .surfaceContainerLow,
                ),
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement =
                        Arrangement.spacedBy(12.dp),
                ) {
                    Row(
                        verticalAlignment =
                            Alignment.CenterVertically,
                    ) {
                        Icon(
                            Icons.Default.Schedule,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme
                                .primary,
                            modifier = Modifier.size(20.dp),
                        )
                        Spacer(
                            modifier = Modifier.width(8.dp),
                        )
                        Text(
                            text = "Тип смены",
                            style = MaterialTheme.typography
                                .titleMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }

                    Row(
                        horizontalArrangement =
                            Arrangement.spacedBy(12.dp),
                    ) {
                        FilterChip(
                            selected =
                                state.shiftType == "day",
                            onClick = {
                                viewModel.onIntent(
                                    ContextSelectionIntent
                                        .ShiftTypeChanged(
                                            "day",
                                        ),
                                )
                            },
                            label = { Text("Дневная") },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.LightMode,
                                    contentDescription =
                                        null,
                                    modifier = Modifier
                                        .size(
                                            FilterChipDefaults
                                                .IconSize,
                                        ),
                                )
                            },
                        )

                        FilterChip(
                            selected =
                                state.shiftType == "night",
                            onClick = {
                                viewModel.onIntent(
                                    ContextSelectionIntent
                                        .ShiftTypeChanged(
                                            "night",
                                        ),
                                )
                            },
                            label = { Text("Ночная") },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.DarkMode,
                                    contentDescription =
                                        null,
                                    modifier = Modifier
                                        .size(
                                            FilterChipDefaults
                                                .IconSize,
                                        ),
                                )
                            },
                        )
                    }
                }
            }

            if (state.error != null) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme
                            .colorScheme.errorContainer,
                    ),
                ) {
                    Text(
                        text = state.error!!,
                        color = MaterialTheme.colorScheme
                            .onErrorContainer,
                        style = MaterialTheme.typography
                            .bodyMedium,
                        modifier = Modifier.padding(12.dp),
                    )
                }
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
                    .height(52.dp),
                enabled = !state.isLoading
                    && state.selectedSite != null,
                shape = MaterialTheme.shapes.small,
                elevation = ButtonDefaults
                    .buttonElevation(
                        defaultElevation = 2.dp,
                    ),
            ) {
                Icon(
                    Icons.Default.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Начать работу",
                    style = MaterialTheme.typography
                        .titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
