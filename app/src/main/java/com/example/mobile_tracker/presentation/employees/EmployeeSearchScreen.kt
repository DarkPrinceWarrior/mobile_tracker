package com.example.mobile_tracker.presentation.employees

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.mobile_tracker.domain.model.Employee
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmployeeSearchScreen(
    viewModel: EmployeeSearchViewModel =
        koinViewModel(),
) {
    val state by viewModel.state.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Поиск сотрудника",
                        fontWeight = FontWeight.SemiBold,
                    )
                },
                colors =
                    TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme
                            .colorScheme.surface,
                    ),
                actions = {
                    if (state.isSyncing) {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .size(24.dp)
                                .padding(end = 8.dp),
                            strokeWidth = 2.dp,
                        )
                    } else {
                        IconButton(onClick = {
                            viewModel.onIntent(
                                EmployeeSearchIntent
                                    .SyncEmployees,
                            )
                        }) {
                            Icon(
                                Icons.Default.Refresh,
                                contentDescription =
                                    "Синхронизировать",
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
                .padding(horizontal = 16.dp),
        ) {
            Spacer(modifier = Modifier.height(4.dp))

            OutlinedTextField(
                value = state.query,
                onValueChange = {
                    viewModel.onIntent(
                        EmployeeSearchIntent
                            .UpdateQuery(it),
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.small,
                placeholder = {
                    Text(
                        "Табельный номер или ФИО",
                    )
                },
                leadingIcon = {
                    Icon(
                        Icons.Default.Search,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme
                            .onSurfaceVariant,
                    )
                },
                singleLine = true,
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Найдено: ${state.totalCount}",
                style = MaterialTheme.typography
                    .labelMedium,
                color = MaterialTheme.colorScheme
                    .onSurfaceVariant,
                modifier = Modifier.padding(
                    start = 4.dp,
                ),
            )

            Spacer(modifier = Modifier.height(12.dp))

            when {
                state.isLoading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize(),
                        contentAlignment =
                            Alignment.Center,
                    ) {
                        CircularProgressIndicator()
                    }
                }

                state.error != null -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize(),
                        contentAlignment =
                            Alignment.Center,
                    ) {
                        Card(
                            colors =
                                CardDefaults.cardColors(
                                    containerColor =
                                        MaterialTheme
                                            .colorScheme
                                            .errorContainer,
                                ),
                        ) {
                            Text(
                                text = state.error ?: "",
                                color = MaterialTheme
                                    .colorScheme
                                    .onErrorContainer,
                                modifier = Modifier
                                    .padding(16.dp),
                            )
                        }
                    }
                }

                state.results.isEmpty() -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize(),
                        contentAlignment =
                            Alignment.Center,
                    ) {
                        Column(
                            horizontalAlignment =
                                Alignment
                                    .CenterHorizontally,
                        ) {
                            Icon(
                                Icons.Default.People,
                                contentDescription =
                                    null,
                                modifier = Modifier
                                    .size(48.dp),
                                tint = MaterialTheme
                                    .colorScheme
                                    .outlineVariant,
                            )
                            Spacer(
                                modifier = Modifier
                                    .height(8.dp),
                            )
                            Text(
                                "Сотрудники не найдены",
                                style = MaterialTheme
                                    .typography
                                    .bodyLarge,
                                color = MaterialTheme
                                    .colorScheme
                                    .onSurfaceVariant,
                            )
                        }
                    }
                }

                else -> {
                    LazyColumn(
                        verticalArrangement =
                            Arrangement.spacedBy(
                                8.dp,
                            ),
                    ) {
                        items(
                            state.results,
                            key = { it.id },
                        ) { employee ->
                            EmployeeCard(employee)
                        }
                        item {
                            Spacer(
                                modifier = Modifier
                                    .height(16.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmployeeCard(employee: Employee) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme
                .surfaceContainerLow,
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment =
                Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(
                        MaterialTheme.colorScheme
                            .primaryContainer,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Default.Person,
                    contentDescription = null,
                    modifier = Modifier.size(22.dp),
                    tint = MaterialTheme.colorScheme
                        .onPrimaryContainer,
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = employee.fullName,
                    style = MaterialTheme.typography
                        .titleSmall,
                    fontWeight = FontWeight.Medium,
                )
                employee.personnelNumber?.let {
                    Row(
                        verticalAlignment =
                            Alignment.CenterVertically,
                    ) {
                        Icon(
                            Icons.Default.Badge,
                            contentDescription = null,
                            modifier =
                                Modifier.size(14.dp),
                            tint = MaterialTheme
                                .colorScheme.primary,
                        )
                        Spacer(
                            modifier =
                                Modifier.width(4.dp),
                        )
                        Text(
                            text = "Таб. №: $it",
                            style = MaterialTheme
                                .typography.bodySmall,
                            color = MaterialTheme
                                .colorScheme.primary,
                        )
                    }
                }
                employee.companyName?.let {
                    Text(
                        text = it,
                        style = MaterialTheme
                            .typography.bodySmall,
                        color = MaterialTheme
                            .colorScheme
                            .onSurfaceVariant,
                    )
                }
                employee.position?.let {
                    Text(
                        text = it,
                        style = MaterialTheme
                            .typography.bodySmall,
                        color = MaterialTheme
                            .colorScheme
                            .onSurfaceVariant,
                    )
                }
                employee.brigadeName?.let {
                    Text(
                        text = "Бригада: $it",
                        style = MaterialTheme
                            .typography.bodySmall,
                        color = MaterialTheme
                            .colorScheme
                            .onSurfaceVariant,
                    )
                }
            }
        }
    }
}
