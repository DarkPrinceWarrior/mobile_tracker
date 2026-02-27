package com.example.mobile_tracker.presentation.journal

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Watch
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.IconButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.mobile_tracker.R
import com.example.mobile_tracker.data.local.db.entity.OperationLogEntity
import com.example.mobile_tracker.presentation.common.AppScreenScaffold
import com.example.mobile_tracker.presentation.common.EmptyState
import com.example.mobile_tracker.presentation.common.LoadingState
import com.example.mobile_tracker.presentation.common.SearchField
import com.example.mobile_tracker.presentation.common.StateCard
import com.example.mobile_tracker.util.formatTimestamp
import org.koin.androidx.compose.koinViewModel

@OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalLayoutApi::class,
)
@Composable
fun JournalScreen(
    onBack: (() -> Unit)? = null,
    viewModel: JournalViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    AppScreenScaffold(
        snackbarMessage = state.error,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(R.string.journal_title),
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
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor =
                        MaterialTheme.colorScheme.surface,
                ),
            )
        },
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = state.isLoading,
            onRefresh = {
                viewModel.onIntent(JournalIntent.Refresh)
            },
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
            ) {
                Spacer(modifier = Modifier.height(8.dp))

                SearchField(
                    query = state.searchQuery,
                    onQueryChange = {
                        viewModel.onIntent(JournalIntent.SetSearchQuery(it))
                    },
                    placeholder = stringResource(R.string.journal_search_hint),
                )

                Spacer(modifier = Modifier.height(8.dp))

                TypeFilterRow(
                    availableTypes = state.availableTypes,
                    selectedType = state.typeFilter,
                    onTypeSelected = { type ->
                        viewModel.onIntent(
                            JournalIntent.SetTypeFilter(type),
                        )
                    },
                )

                Spacer(modifier = Modifier.height(4.dp))

                StatusFilterRow(
                    selectedStatus = state.statusFilter,
                    onStatusSelected = { status ->
                        viewModel.onIntent(
                            JournalIntent.SetStatusFilter(
                                status,
                            ),
                        )
                    },
                )

                Spacer(modifier = Modifier.height(8.dp))

                if (state.error != null) {
                    StateCard(
                        message = state.error!!,
                        modifier = Modifier.padding(bottom = 8.dp),
                    )
                }

                if (state.isLoading && state.logs.isEmpty()) {
                    LoadingState()
                } else if (state.filteredLogs.isEmpty()) {
                    EmptyState(
                        title = stringResource(R.string.journal_empty),
                        icon = Icons.Default.SwapHoriz,
                    )
                } else {
                    LazyColumn(
                        verticalArrangement =
                            Arrangement.spacedBy(8.dp),
                    ) {
                        items(
                            items = state.filteredLogs,
                            key = { it.id },
                        ) { log ->
                            LogEntryCard(
                                log = log,
                                isSelected = state.selectedLogId == log.id,
                                onClick = {
                                    viewModel.onIntent(
                                        JournalIntent.SelectLog(log.id),
                                    )
                                },
                            )
                        }
                        item {
                            Spacer(
                                modifier = Modifier.height(16.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TypeFilterRow(
    availableTypes: List<String>,
    selectedType: String?,
    onTypeSelected: (String?) -> Unit,
) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        FilterChip(
            selected = selectedType == null,
            onClick = { onTypeSelected(null) },
            label = {
                Text(stringResource(R.string.journal_filter_all))
            },
        )
        availableTypes.forEach { type ->
            FilterChip(
                selected = selectedType == type,
                onClick = {
                    onTypeSelected(
                        if (selectedType == type) null else type,
                    )
                },
                label = { Text(typeDisplayName(type)) },
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun StatusFilterRow(
    selectedStatus: String?,
    onStatusSelected: (String?) -> Unit,
) {
    val statuses = listOf("success", "error", "pending")
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        statuses.forEach { status ->
            FilterChip(
                selected = selectedStatus == status,
                onClick = {
                    onStatusSelected(
                        if (selectedStatus == status) {
                            null
                        } else {
                            status
                        },
                    )
                },
                label = { Text(statusDisplayName(status)) },
            )
        }
    }
}

@Composable
private fun LogEntryCard(
    log: OperationLogEntity,
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
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = typeIcon(log.type),
                contentDescription = log.type,
                tint = typeColor(log.type),
                modifier = Modifier.size(32.dp),
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = typeDisplayName(log.type),
                    style = MaterialTheme.typography.titleSmall,
                )
                if (log.employeeName != null) {
                    Text(
                        text = log.employeeName,
                        style = MaterialTheme.typography
                            .bodyMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                if (log.deviceId != null) {
                    Text(
                        text = log.deviceId,
                        style = MaterialTheme.typography
                            .bodySmall,
                        color = MaterialTheme.colorScheme
                            .onSurfaceVariant,
                    )
                }
                if (log.errorMessage != null) {
                    Text(
                        text = log.errorMessage,
                        style = MaterialTheme.typography
                            .bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column(
                horizontalAlignment = Alignment.End,
            ) {
                Text(
                    text = formatTimestamp(log.createdAt),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme
                        .onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(4.dp))
                StatusBadge(status = log.status)
            }
        }
    }
}

@Composable
private fun StatusBadge(status: String) {
    val (text, color) = when (status) {
        "success" -> "OK" to Color(0xFF2E7D32)
        "error" -> "Ошибка" to Color(0xFFC62828)
        "pending" -> "Ожидание" to Color(0xFFE65100)
        else -> status to MaterialTheme.colorScheme
            .onSurfaceVariant
    }
    Card(
        colors = CardDefaults.cardColors(
            containerColor = color.copy(alpha = 0.12f),
        ),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = color,
            modifier = Modifier.padding(
                horizontal = 8.dp,
                vertical = 2.dp,
            ),
        )
    }
}

private fun typeDisplayName(type: String): String = when (type) {
    "issue" -> "Выдача"
    "return" -> "Возврат"
    "upload" -> "Выгрузка"
    "upload_error" -> "Ошибка выгрузки"
    "sync" -> "Синхронизация"
    "status_change" -> "Смена статуса"
    else -> type
}

private fun statusDisplayName(status: String): String =
    when (status) {
        "success" -> "Успех"
        "error" -> "Ошибка"
        "pending" -> "Ожидание"
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
    "issue" -> Color(0xFF1565C0)
    "return" -> Color(0xFF2E7D32)
    "upload" -> Color(0xFF6A1B9A)
    "upload_error" -> MaterialTheme.colorScheme.error
    "sync" -> Color(0xFFE65100)
    else -> MaterialTheme.colorScheme.onSurfaceVariant
}
