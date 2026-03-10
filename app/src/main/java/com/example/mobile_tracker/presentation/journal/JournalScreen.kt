package com.example.mobile_tracker.presentation.journal

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Watch
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.mobile_tracker.R
import com.example.mobile_tracker.data.local.db.entity.OperationLogEntity
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
import com.example.mobile_tracker.ui.theme.danger
import com.example.mobile_tracker.ui.theme.success
import com.example.mobile_tracker.ui.theme.warning
import com.example.mobile_tracker.util.formatTimestamp
import org.koin.androidx.compose.koinViewModel

@androidx.compose.material3.ExperimentalMaterial3Api
@Composable
fun JournalScreen(
    onBack: (() -> Unit)? = null,
    viewModel: JournalViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val isTablet = rememberIsTablet()
    val selectedLog = state.filteredLogs.firstOrNull { it.id == state.selectedLogId }
    val allTypeLabel = stringResource(R.string.journal_filter_all)
    val typeLabels = state.availableTypes.associateWith { typeDisplayName(it) }
    val successLabel = stringResource(R.string.journal_status_success)
    val errorLabel = stringResource(R.string.journal_status_error)
    val pendingLabel = stringResource(R.string.journal_status_pending)

    AppScreenScaffold(
        snackbarMessage = state.error,
        topBar = {
            MTCompactTopBar(
                title = stringResource(R.string.journal_title),
                subtitle = stringResource(R.string.journal_summary_subtitle, state.filteredLogs.size),
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
            )
        },
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = state.isLoading,
            onRefresh = { viewModel.onIntent(JournalIntent.Refresh) },
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = AppLayout.screenPadding, vertical = AppSpacing.sm),
                verticalArrangement = Arrangement.spacedBy(AppSpacing.md),
            ) {
                JournalSummaryCard(
                    filteredCount = state.filteredLogs.size,
                    selectedType = state.typeFilter,
                    selectedStatus = state.statusFilter,
                )

                SearchField(
                    query = state.searchQuery,
                    onQueryChange = { viewModel.onIntent(JournalIntent.SetSearchQuery(it)) },
                    placeholder = stringResource(R.string.journal_search_hint),
                )

                JournalFilterRow(
                    items = listOf(allTypeLabel) + typeLabels.values.toList(),
                    selectedLabel = state.typeFilter?.let { typeLabels[it] } ?: allTypeLabel,
                    onSelected = { label ->
                        if (label == allTypeLabel) {
                            viewModel.onIntent(JournalIntent.SetTypeFilter(null))
                        } else {
                            val type = typeLabels.entries.firstOrNull { it.value == label }?.key
                            viewModel.onIntent(JournalIntent.SetTypeFilter(type))
                        }
                    },
                )

                JournalFilterRow(
                    items = listOf(successLabel, errorLabel, pendingLabel),
                    selectedLabel = state.statusFilter?.let { statusDisplayName(it) },
                    onSelected = { label ->
                        val next = when (label) {
                            successLabel -> "success"
                            errorLabel -> "error"
                            else -> "pending"
                        }
                        viewModel.onIntent(
                            JournalIntent.SetStatusFilter(
                                if (state.statusFilter == next) null else next,
                            ),
                        )
                    },
                )

                if (state.error != null) {
                    StateCard(message = state.error!!, isError = true)
                }

                Box(modifier = Modifier.weight(1f)) {
                    AdaptiveListDetail(
                        isTablet = isTablet,
                        listPane = { paneModifier ->
                            when {
                                state.isLoading && state.logs.isEmpty() -> LoadingState(modifier = paneModifier)
                                state.filteredLogs.isEmpty() -> EmptyState(
                                    title = stringResource(R.string.journal_empty),
                                    icon = Icons.Default.SwapHoriz,
                                    modifier = paneModifier,
                                )
                                else -> LazyColumn(
                                    modifier = paneModifier,
                                    verticalArrangement = Arrangement.spacedBy(AppSpacing.xs),
                                ) {
                                    items(state.filteredLogs, key = { it.id }) { log ->
                                        JournalLogCard(
                                            log = log,
                                            isSelected = state.selectedLogId == log.id,
                                            onClick = { viewModel.onIntent(JournalIntent.SelectLog(log.id)) },
                                        )
                                    }
                                    item { Spacer(modifier = Modifier.height(AppSpacing.lg)) }
                                }
                            }
                        },
                        detailPane = { paneModifier ->
                            JournalDetailPane(
                                modifier = paneModifier.padding(start = AppSpacing.sm),
                                log = selectedLog,
                            )
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun JournalSummaryCard(
    filteredCount: Int,
    selectedType: String?,
    selectedStatus: String?,
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
                    text = stringResource(R.string.journal_summary_title),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = filteredCount.toString(),
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = selectedType?.let { typeDisplayName(it) }
                        ?: stringResource(R.string.journal_filter_all),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            MTStatusBadge(
                label = selectedStatus?.let { statusDisplayName(it) }
                    ?: stringResource(R.string.journal_filter_all),
                tone = when (selectedStatus) {
                    "error" -> MTStatusTone.Danger
                    "pending" -> MTStatusTone.Warning
                    else -> MTStatusTone.Neutral
                },
            )
        }
    }
}

@Composable
private fun JournalFilterRow(
    items: List<String>,
    selectedLabel: String?,
    onSelected: (String) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(AppSpacing.xs),
    ) {
        items.forEach { label ->
            Surface(
                modifier = Modifier.clickable(onClick = { onSelected(label) }),
                shape = RoundedCornerShape(AppRadius.pill),
                color = if (selectedLabel == label) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.surfaceContainer
                },
            ) {
                Text(
                    text = label,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    style = MaterialTheme.typography.labelLarge,
                    color = if (selectedLabel == label) {
                        MaterialTheme.colorScheme.onPrimary
                    } else {
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                    },
                )
            }
        }
    }
}

@Composable
private fun JournalLogCard(
    log: OperationLogEntity,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .semantics { selected = isSelected }
            .clickable(
                onClickLabel = stringResource(R.string.action_open_journal_details),
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
            verticalAlignment = Alignment.Top,
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(typeColor(log.type).copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = typeIcon(log.type),
                    contentDescription = null,
                    tint = typeColor(log.type),
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = typeDisplayName(log.type),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                log.employeeName?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                log.deviceId?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                log.errorMessage?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.danger,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(AppSpacing.xxs),
            ) {
                Text(
                    text = formatTimestamp(log.createdAt),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                MTStatusBadge(
                    label = statusDisplayName(log.status),
                    tone = when (log.status) {
                        "success" -> MTStatusTone.Success
                        "error" -> MTStatusTone.Danger
                        "pending" -> MTStatusTone.Warning
                        else -> MTStatusTone.Neutral
                    },
                )
            }
        }
    }
}

@Composable
private fun JournalDetailPane(
    modifier: Modifier,
    log: OperationLogEntity?,
) {
    if (log == null) {
        EmptyState(
            title = stringResource(R.string.journal_detail_empty),
            icon = Icons.Default.SwapHoriz,
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
                        .background(typeColor(log.type).copy(alpha = 0.14f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = typeIcon(log.type),
                        contentDescription = null,
                        tint = typeColor(log.type),
                    )
                }
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Text(
                        text = typeDisplayName(log.type),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = formatTimestamp(log.createdAt),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                MTStatusBadge(
                    label = statusDisplayName(log.status),
                    tone = when (log.status) {
                        "success" -> MTStatusTone.Success
                        "error" -> MTStatusTone.Danger
                        "pending" -> MTStatusTone.Warning
                        else -> MTStatusTone.Neutral
                    },
                )
            }

            log.employeeName?.let {
                JournalDetailRow(
                    label = stringResource(R.string.journal_detail_employee),
                    value = it,
                )
            }
            log.deviceId?.let {
                JournalDetailRow(
                    label = stringResource(R.string.journal_detail_device),
                    value = it,
                )
            }
            log.details?.let {
                JournalDetailRow(
                    label = stringResource(R.string.journal_detail_details),
                    value = it,
                )
            }
            log.errorMessage?.let {
                StateCard(message = it, isError = true)
            }
        }
    }
}

@Composable
private fun JournalDetailRow(
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

@Composable
private fun typeDisplayName(type: String): String = when (type) {
    "issue" -> stringResource(R.string.journal_type_issue)
    "return" -> stringResource(R.string.journal_type_return)
    "upload" -> stringResource(R.string.journal_type_upload)
    "upload_error" -> stringResource(R.string.journal_type_upload_error)
    "sync" -> stringResource(R.string.journal_type_sync)
    "status_change" -> stringResource(R.string.journal_type_status_change)
    else -> type
}

@Composable
private fun statusDisplayName(status: String): String = when (status) {
    "success" -> stringResource(R.string.journal_status_success)
    "error" -> stringResource(R.string.journal_status_error)
    "pending" -> stringResource(R.string.journal_status_pending)
    else -> status
}

private fun typeIcon(type: String): ImageVector = when (type) {
    "issue" -> Icons.Default.Watch
    "return" -> Icons.Default.SwapHoriz
    "upload" -> Icons.Default.CloudUpload
    "upload_error" -> Icons.Default.Error
    "sync" -> Icons.Default.Sync
    else -> Icons.Default.SwapHoriz
}

@Composable
private fun typeColor(type: String): Color = when (type) {
    "issue" -> MaterialTheme.colorScheme.primary
    "return" -> MaterialTheme.colorScheme.success
    "upload" -> MaterialTheme.colorScheme.tertiary
    "upload_error" -> MaterialTheme.colorScheme.danger
    "sync" -> MaterialTheme.colorScheme.warning
    else -> MaterialTheme.colorScheme.onSurfaceVariant
}
