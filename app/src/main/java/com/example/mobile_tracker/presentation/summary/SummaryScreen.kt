package com.example.mobile_tracker.presentation.summary

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.SyncProblem
import androidx.compose.material.icons.filled.Watch
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.mobile_tracker.R
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SummaryScreen(
    viewModel: SummaryViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(R.string.summary_title),
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor =
                        MaterialTheme.colorScheme.primaryContainer,
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
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
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
                        Text(
                            text = state.error!!,
                            color = MaterialTheme
                                .colorScheme.error,
                            style = MaterialTheme
                                .typography.bodyMedium,
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    MetricCard(
                        icon = Icons.Default.Watch,
                        iconTint = Color(0xFF1565C0),
                        label = stringResource(
                            R.string.summary_issued,
                        ),
                        value = state.issuedCount.toString(),
                    )
                    MetricCard(
                        icon = Icons.Default.CheckCircle,
                        iconTint = Color(0xFF2E7D32),
                        label = stringResource(
                            R.string.summary_returned,
                        ),
                        value = state.returnedCount.toString(),
                    )
                    MetricCard(
                        icon = Icons.Default.HourglassTop,
                        iconTint = Color(0xFFE65100),
                        label = stringResource(
                            R.string.summary_not_returned,
                        ),
                        value =
                            state.notReturnedCount.toString(),
                    )
                    MetricCard(
                        icon = Icons.Default.CloudDone,
                        iconTint = Color(0xFF6A1B9A),
                        label = stringResource(
                            R.string.summary_data_uploaded,
                        ),
                        value =
                            state.dataUploadedCount.toString(),
                    )
                    MetricCard(
                        icon = Icons.Default.CloudOff,
                        iconTint = Color(0xFFEF6C00),
                        label = stringResource(
                            R.string.summary_pending_packets,
                        ),
                        value =
                            state.pendingPacketsCount.toString(),
                    )
                    MetricCard(
                        icon = Icons.Default.Error,
                        iconTint = Color(0xFFC62828),
                        label = stringResource(
                            R.string.summary_error_packets,
                        ),
                        value =
                            state.errorPacketsCount.toString(),
                    )
                    MetricCard(
                        icon = Icons.Default.SyncProblem,
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
) {
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
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = iconTint,
                modifier = Modifier.size(36.dp),
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 16.dp),
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme
                        .onSurfaceVariant,
                )
            }
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = iconTint,
            )
        }
    }
}
