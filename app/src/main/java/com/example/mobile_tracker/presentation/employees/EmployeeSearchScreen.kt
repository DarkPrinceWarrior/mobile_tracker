package com.example.mobile_tracker.presentation.employees

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.mobile_tracker.R
import com.example.mobile_tracker.domain.model.Employee
import com.example.mobile_tracker.presentation.common.AppScreenScaffold
import com.example.mobile_tracker.presentation.common.EmptyState
import com.example.mobile_tracker.presentation.common.LoadingState
import com.example.mobile_tracker.presentation.common.SearchField
import com.example.mobile_tracker.presentation.common.StateCard
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmployeeSearchScreen(
    onBack: (() -> Unit)? = null,
    viewModel: EmployeeSearchViewModel =
        koinViewModel(),
) {
    val state by viewModel.state.collectAsState()

    AppScreenScaffold(
        snackbarMessage = state.error,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(R.string.employees_title),
                        fontWeight = FontWeight.SemiBold,
                    )
                },
                navigationIcon = {
                    if (onBack != null) {
                        IconButton(onClick = onBack) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.action_back),
                            )
                        }
                    }
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
                                    stringResource(R.string.sync_action),
                            )
                        }
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(horizontal = 16.dp),
        ) {
            Spacer(modifier = Modifier.height(4.dp))

            SearchField(
                query = state.query,
                onQueryChange = {
                    viewModel.onIntent(EmployeeSearchIntent.UpdateQuery(it))
                },
                placeholder = stringResource(R.string.employees_search_hint),
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = stringResource(
                    R.string.employees_found,
                    state.totalCount,
                ),
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
                    LoadingState()
                }

                state.error != null -> {
                    StateCard(message = state.error.orEmpty())
                }

                state.results.isEmpty() -> {
                    EmptyState(
                        title = stringResource(R.string.employees_empty),
                        icon = Icons.Default.People,
                    )
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
                            EmployeeCard(
                                employee = employee,
                                isSelected = state.selectedEmployeeId == employee.id,
                                onClick = {
                                    viewModel.onIntent(
                                        EmployeeSearchIntent.SelectEmployee(
                                            employee.id,
                                        ),
                                    )
                                },
                            )
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
private fun EmployeeCard(
    employee: Employee,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.secondaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceContainerLow
            },
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
