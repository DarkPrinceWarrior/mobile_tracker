package com.example.mobile_tracker.presentation.summary

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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.mobile_tracker.R
import com.example.mobile_tracker.presentation.common.AppScreenScaffold
import com.example.mobile_tracker.presentation.common.LoadingState
import com.example.mobile_tracker.presentation.common.StateCard
import com.example.mobile_tracker.ui.theme.Danger
import com.example.mobile_tracker.ui.theme.Success
import com.example.mobile_tracker.ui.theme.Warning
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SummaryScreen(
    onBack: (() -> Unit)? = null,
    viewModel: SummaryViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    AppScreenScaffold(
        snackbarMessage = state.error,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(R.string.summary_title),
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
                viewModel.onIntent(SummaryIntent.Refresh)
            },
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
                        .padding(16.dp),
                    verticalArrangement =
                        Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        text = stringResource(
                            R.string.summary_subtitle,
                            state.siteName,
                            state.shiftDate,
                        ),
                        style = MaterialTheme.typography
                            .titleSmall,
                        color = MaterialTheme.colorScheme
                            .onSurfaceVariant,
                    )

                    if (state.error != null) {
                        StateCard(message = state.error!!)
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        horizontalArrangement =
                            Arrangement.spacedBy(
                                12.dp,
                            ),
                    ) {
                        MetricCard(
                            modifier = Modifier
                                .weight(1f),
                            icon = Icons.Default.Watch,
                            iconTint =
                                MaterialTheme.colorScheme
                                    .primary,
                            label = stringResource(
                                R.string.summary_issued,
                            ),
                            value = state.issuedCount
                                .toString(),
                        )
                        MetricCard(
                            modifier = Modifier
                                .weight(1f),
                            icon = Icons.Default
                                .CheckCircle,
                            iconTint = Success,
                            label = stringResource(
                                R.string
                                    .summary_returned,
                            ),
                            value = state.returnedCount
                                .toString(),
                        )
                    }

                    Row(
                        horizontalArrangement =
                            Arrangement.spacedBy(
                                12.dp,
                            ),
                    ) {
                        MetricCard(
                            modifier = Modifier
                                .weight(1f),
                            icon = Icons.Default
                                .HourglassTop,
                            iconTint = Warning,
                            label = stringResource(
                                R.string
                                    .summary_not_returned,
                            ),
                            value =
                                state.notReturnedCount
                                    .toString(),
                        )
                        MetricCard(
                            modifier = Modifier
                                .weight(1f),
                            icon = Icons.Default
                                .CloudDone,
                            iconTint = MaterialTheme
                                .colorScheme.tertiary,
                            label = stringResource(
                                R.string
                                    .summary_data_uploaded,
                            ),
                            value =
                                state.dataUploadedCount
                                    .toString(),
                        )
                    }

                    Row(
                        horizontalArrangement =
                            Arrangement.spacedBy(
                                12.dp,
                            ),
                    ) {
                        MetricCard(
                            modifier = Modifier
                                .weight(1f),
                            icon = Icons.Default
                                .CloudOff,
                            iconTint = Warning,
                            label = stringResource(
                                R.string
                                    .summary_pending_packets,
                            ),
                            value =
                                state.pendingPacketsCount
                                    .toString(),
                        )
                        MetricCard(
                            modifier = Modifier
                                .weight(1f),
                            icon = Icons.Default.Error,
                            iconTint = Danger,
                            label = stringResource(
                                R.string
                                    .summary_error_packets,
                            ),
                            value =
                                state.errorPacketsCount
                                    .toString(),
                        )
                    }

                    MetricCard(
                        modifier = Modifier
                            .fillMaxWidth(),
                        icon = Icons.Default
                            .SyncProblem,
                        iconTint = Color(0xFF795548),
                        label = stringResource(
                            R.string.summary_unsynced,
                        ),
                        value =
                            state.unsyncedBindingsCount
                                .toString(),
                    )
                }
            }
        }
    }
}

@Composable
private fun MetricCard(
    icon: ImageVector,
    iconTint: Color,
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme
                .surfaceContainerLow,
        ),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
        ) {
            Row(
                verticalAlignment =
                    Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(
                            iconTint.copy(alpha = 0.12f),
                        ),
                    contentAlignment =
                        Alignment.Center,
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = label,
                        tint = iconTint,
                        modifier = Modifier
                            .size(20.dp),
                    )
                }
                Spacer(
                    modifier = Modifier.width(10.dp),
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography
                        .bodySmall,
                    color = MaterialTheme.colorScheme
                        .onSurfaceVariant,
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = value,
                style = MaterialTheme.typography
                    .headlineMedium,
                fontWeight = FontWeight.Bold,
                color = iconTint,
            )
        }
    }
}
