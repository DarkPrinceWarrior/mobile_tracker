package com.example.mobile_tracker.presentation.employees

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.mobile_tracker.R
import com.example.mobile_tracker.domain.model.Employee
import com.example.mobile_tracker.presentation.common.AdaptiveListDetail
import com.example.mobile_tracker.presentation.common.AppScreenScaffold
import com.example.mobile_tracker.presentation.common.EmptyState
import com.example.mobile_tracker.presentation.common.LoadingState
import com.example.mobile_tracker.presentation.common.MTCard
import com.example.mobile_tracker.presentation.common.MTCompactTopBar
import com.example.mobile_tracker.presentation.common.MTStatusBadge
import com.example.mobile_tracker.presentation.common.MTStatusTone
import com.example.mobile_tracker.presentation.common.SearchField
import com.example.mobile_tracker.presentation.common.StateCard
import com.example.mobile_tracker.presentation.common.rememberIsTablet
import com.example.mobile_tracker.ui.theme.AppLayout
import com.example.mobile_tracker.ui.theme.AppRadius
import com.example.mobile_tracker.ui.theme.AppSpacing
import org.koin.androidx.compose.koinViewModel

@Composable
fun EmployeeSearchScreen(
    onBack: (() -> Unit)? = null,
    viewModel: EmployeeSearchViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val isTablet = rememberIsTablet()
    val selectedEmployee = state.results.firstOrNull { it.id == state.selectedEmployeeId }

    AppScreenScaffold(
        snackbarMessage = state.error,
        topBar = {
            MTCompactTopBar(
                title = stringResource(R.string.employees_title),
                subtitle = stringResource(R.string.employees_search_hint),
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
                actions = {
                    if (state.isSyncing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.tertiary,
                        )
                    } else {
                        IconButton(onClick = { viewModel.onIntent(EmployeeSearchIntent.SyncEmployees) }) {
                            Icon(
                                Icons.Default.Refresh,
                                contentDescription = stringResource(R.string.sync_action),
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
            EmployeeSearchSummaryCard(
                totalCount = state.totalCount,
                query = state.query,
                selectedEmployee = selectedEmployee,
            )

            SearchField(
                query = state.query,
                onQueryChange = { viewModel.onIntent(EmployeeSearchIntent.UpdateQuery(it)) },
                placeholder = stringResource(R.string.employees_search_hint),
            )

            Text(
                text = stringResource(R.string.employees_found, state.totalCount),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Box(modifier = Modifier.weight(1f)) {
                AdaptiveListDetail(
                    isTablet = isTablet,
                    listPane = { paneModifier ->
                        when {
                            state.isLoading -> LoadingState(modifier = paneModifier)
                            state.error != null -> Box(modifier = paneModifier) {
                                StateCard(message = state.error.orEmpty())
                            }
                            state.results.isEmpty() -> EmptyState(
                                title = stringResource(R.string.employees_empty),
                                icon = Icons.Default.People,
                                modifier = paneModifier,
                            )
                            else -> LazyColumn(
                                modifier = paneModifier,
                                verticalArrangement = Arrangement.spacedBy(AppSpacing.xs),
                            ) {
                                items(state.results, key = { it.id }) { employee ->
                                    EmployeeCard(
                                        employee = employee,
                                        isSelected = state.selectedEmployeeId == employee.id,
                                        onClick = {
                                            viewModel.onIntent(
                                                EmployeeSearchIntent.SelectEmployee(employee.id),
                                            )
                                        },
                                    )
                                }
                                item { Spacer(modifier = Modifier.height(AppSpacing.lg)) }
                            }
                        }
                    },
                    detailPane = { paneModifier ->
                        EmployeeDetailPane(
                            modifier = paneModifier.padding(start = AppSpacing.sm),
                            employee = selectedEmployee,
                        )
                    },
                )
            }
        }
    }
}

@Composable
private fun EmployeeSearchSummaryCard(
    totalCount: Int,
    query: String,
    selectedEmployee: Employee?,
) {
    MTCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(AppSpacing.xxs),
            ) {
                Text(
                    text = stringResource(R.string.employees_section_title),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = totalCount.toString(),
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = if (query.isBlank()) {
                        stringResource(R.string.employees_list_mode)
                    } else {
                        query
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            MTStatusBadge(
                label = if (query.isBlank()) {
                    stringResource(R.string.employees_list_mode)
                } else {
                    stringResource(R.string.action_search)
                },
                tone = if (query.isBlank()) MTStatusTone.Neutral else MTStatusTone.Success,
            )
        }

        if (selectedEmployee != null) {
            Spacer(modifier = Modifier.height(AppSpacing.xs))
            Surface(
                shape = RoundedCornerShape(AppRadius.pill),
                color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.45f),
            ) {
                Text(
                    text = stringResource(R.string.employees_selected, selectedEmployee.fullName),
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.tertiary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
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
            .semantics { selected = isSelected }
            .clickable(
                onClickLabel = stringResource(R.string.action_open_employee_details),
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
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(AppLayout.cardPadding),
            verticalArrangement = Arrangement.spacedBy(AppSpacing.sm),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm),
                verticalAlignment = Alignment.Top,
            ) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.55f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Default.Person,
                        contentDescription = null,
                        modifier = Modifier.size(22.dp),
                        tint = MaterialTheme.colorScheme.tertiary,
                    )
                }
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Text(
                        text = employee.fullName,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    val secondaryLine = listOfNotNull(employee.companyName, employee.position)
                        .joinToString(" · ")
                    if (secondaryLine.isNotBlank()) {
                        Text(
                            text = secondaryLine,
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

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(AppSpacing.xs),
            ) {
                employee.personnelNumber?.let {
                    EmployeeMetaPill(
                        icon = Icons.Default.Badge,
                        text = stringResource(R.string.employees_personnel_number, it),
                    )
                }
                employee.brigadeName?.let {
                    EmployeeMetaPill(
                        icon = Icons.Default.People,
                        text = stringResource(R.string.employees_brigade, it),
                    )
                }
            }
        }
    }
}

@Composable
private fun EmployeeMetaPill(
    icon: ImageVector,
    text: String,
) {
    Surface(
        shape = RoundedCornerShape(AppRadius.pill),
        color = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AppSpacing.xxs),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(14.dp),
                tint = MaterialTheme.colorScheme.tertiary,
            )
            Text(
                text = text,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun EmployeeDetailPane(
    modifier: Modifier,
    employee: Employee?,
) {
    if (employee == null) {
        EmptyState(
            title = stringResource(R.string.employees_detail_empty),
            icon = Icons.Default.People,
            modifier = modifier.fillMaxSize(),
        )
        return
    }

    Card(
        modifier = modifier.fillMaxSize(),
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
                horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.55f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Default.Person,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.tertiary,
                    )
                }
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Text(
                        text = employee.fullName,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    employee.companyName?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            employee.personnelNumber?.let {
                EmployeeDetailRow(
                    label = stringResource(R.string.issue_personnel_label),
                    value = it,
                )
            }
            employee.position?.let {
                EmployeeDetailRow(
                    label = stringResource(R.string.employees_detail_position),
                    value = it,
                )
            }
            employee.brigadeName?.let {
                EmployeeDetailRow(
                    label = stringResource(R.string.employees_detail_brigade),
                    value = it,
                )
            }
            employee.passNumber?.let {
                EmployeeDetailRow(
                    label = stringResource(R.string.employees_detail_pass),
                    value = it,
                )
            }
        }
    }
}

@Composable
private fun EmployeeDetailRow(
    label: String,
    value: String,
) {
    Surface(
        shape = RoundedCornerShape(AppRadius.lg),
        color = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}
