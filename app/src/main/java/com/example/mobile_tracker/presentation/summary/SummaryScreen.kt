package com.example.mobile_tracker.presentation.summary

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.SyncProblem
import androidx.compose.material.icons.filled.Watch
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.mobile_tracker.R
import com.example.mobile_tracker.presentation.common.AppScreenScaffold
import com.example.mobile_tracker.presentation.common.LoadingState
import com.example.mobile_tracker.presentation.common.MTCard
import com.example.mobile_tracker.presentation.common.MTCompactTopBar
import com.example.mobile_tracker.presentation.common.MTStatusBadge
import com.example.mobile_tracker.presentation.common.MTStatusTone
import com.example.mobile_tracker.presentation.common.StateCard
import com.example.mobile_tracker.ui.theme.AppLayout
import com.example.mobile_tracker.ui.theme.AppRadius
import com.example.mobile_tracker.ui.theme.AppSpacing
import com.example.mobile_tracker.ui.theme.danger
import com.example.mobile_tracker.ui.theme.success
import com.example.mobile_tracker.ui.theme.warning
import org.koin.androidx.compose.koinViewModel

@androidx.compose.material3.ExperimentalMaterial3Api
@Composable
fun SummaryScreen(
    onBack: (() -> Unit)? = null,
    viewModel: SummaryViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    AppScreenScaffold(
        snackbarMessage = state.error,
        topBar = {
            MTCompactTopBar(
                title = stringResource(R.string.summary_title),
                subtitle = stringResource(R.string.summary_subtitle, state.siteName, state.shiftDate),
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
            onRefresh = { viewModel.onIntent(SummaryIntent.Refresh) },
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            if (state.isLoading && state.issuedCount == 0) {
                LoadingState()
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = AppLayout.screenPadding, vertical = AppSpacing.sm),
                    verticalArrangement = Arrangement.spacedBy(AppSpacing.md),
                ) {
                    SummaryHeroCard(
                        issuedCount = state.issuedCount,
                        returnedCount = state.returnedCount,
                        shiftType = state.shiftType,
                    )

                    if (state.error != null) {
                        StateCard(message = state.error!!, isError = true)
                    }

                    SummaryMetricGrid(
                        first = {
                            SummaryMetricCard(
                                icon = Icons.Default.Watch,
                                iconTint = MaterialTheme.colorScheme.primary,
                                label = stringResource(R.string.summary_issued),
                                value = state.issuedCount.toString(),
                            )
                        },
                        second = {
                            SummaryMetricCard(
                                icon = Icons.Default.CheckCircle,
                                iconTint = MaterialTheme.colorScheme.success,
                                label = stringResource(R.string.summary_returned),
                                value = state.returnedCount.toString(),
                            )
                        },
                    )

                    SummaryMetricGrid(
                        first = {
                            SummaryMetricCard(
                                icon = Icons.Default.HourglassTop,
                                iconTint = MaterialTheme.colorScheme.warning,
                                label = stringResource(R.string.summary_not_returned),
                                value = state.notReturnedCount.toString(),
                            )
                        },
                        second = {
                            SummaryMetricCard(
                                icon = Icons.Default.CloudDone,
                                iconTint = MaterialTheme.colorScheme.tertiary,
                                label = stringResource(R.string.summary_data_uploaded),
                                value = state.dataUploadedCount.toString(),
                            )
                        },
                    )

                    SummaryMetricGrid(
                        first = {
                            SummaryMetricCard(
                                icon = Icons.Default.CloudOff,
                                iconTint = MaterialTheme.colorScheme.warning,
                                label = stringResource(R.string.summary_pending_packets),
                                value = state.pendingPacketsCount.toString(),
                            )
                        },
                        second = {
                            SummaryMetricCard(
                                icon = Icons.Default.Error,
                                iconTint = MaterialTheme.colorScheme.danger,
                                label = stringResource(R.string.summary_error_packets),
                                value = state.errorPacketsCount.toString(),
                            )
                        },
                    )

                    SummaryMetricCard(
                        icon = Icons.Default.SyncProblem,
                        iconTint = MaterialTheme.colorScheme.secondary,
                        label = stringResource(R.string.summary_unsynced),
                        value = state.unsyncedBindingsCount.toString(),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}

@Composable
private fun SummaryHeroCard(
    issuedCount: Int,
    returnedCount: Int,
    shiftType: String,
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
                androidx.compose.material3.Text(
                    text = stringResource(R.string.summary_hero_title),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                androidx.compose.material3.Text(
                    text = issuedCount.toString(),
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
                androidx.compose.material3.Text(
                    text = if (shiftType == "day") {
                        stringResource(R.string.context_shift_day)
                    } else {
                        stringResource(R.string.context_shift_night)
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            MTStatusBadge(
                label = stringResource(R.string.summary_hero_status, returnedCount),
                tone = MTStatusTone.Success,
            )
        }
    }
}

@Composable
private fun SummaryMetricGrid(
    first: @Composable () -> Unit,
    second: @Composable () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(AppSpacing.xs),
    ) {
        Box(modifier = Modifier.weight(1f)) { first() }
        Box(modifier = Modifier.weight(1f)) { second() }
    }
}

@Composable
private fun SummaryMetricCard(
    icon: ImageVector,
    iconTint: Color,
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier,
        shape = androidx.compose.foundation.shape.RoundedCornerShape(AppRadius.lg),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
        ),
    ) {
        Column(
            modifier = Modifier.padding(AppLayout.cardPadding),
            verticalArrangement = Arrangement.spacedBy(AppSpacing.sm),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(AppSpacing.xs),
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(iconTint.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = label,
                        tint = iconTint,
                    )
                }
                androidx.compose.material3.Text(
                    text = label,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            androidx.compose.material3.Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium,
                color = iconTint,
            )
        }
    }
}
