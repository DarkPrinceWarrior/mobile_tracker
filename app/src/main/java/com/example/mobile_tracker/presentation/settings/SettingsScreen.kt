package com.example.mobile_tracker.presentation.settings

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
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Cached
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.mobile_tracker.R
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateToLogin: () -> Unit = {},
    onNavigateToContextSelection: () -> Unit = {},
    viewModel: SettingsViewModel = koinViewModel(),
) {
    val state by viewModel.state
        .collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                SettingsEffect.NavigateToLogin ->
                    onNavigateToLogin()
                SettingsEffect.NavigateToContextSelection ->
                    onNavigateToContextSelection()
                is SettingsEffect.ShowMessage -> {}
            }
        }
    }

    if (state.showLogoutDialog) {
        ConfirmDialog(
            title = stringResource(R.string.settings_logout),
            text = stringResource(
                R.string.settings_logout_confirm,
            ),
            onConfirm = {
                viewModel.onIntent(
                    SettingsIntent.LogoutConfirmed,
                )
            },
            onDismiss = {
                viewModel.onIntent(
                    SettingsIntent.LogoutDismissed,
                )
            },
        )
    }

    if (state.showClearCacheDialog) {
        ConfirmDialog(
            title = stringResource(
                R.string.settings_clear_cache,
            ),
            text = stringResource(
                R.string.settings_clear_cache_confirm,
            ),
            onConfirm = {
                viewModel.onIntent(
                    SettingsIntent.ClearCacheConfirmed,
                )
            },
            onDismiss = {
                viewModel.onIntent(
                    SettingsIntent.ClearCacheDismissed,
                )
            },
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(
                            R.string.settings_title,
                        ),
                    )
                },
                colors = TopAppBarDefaults
                    .topAppBarColors(
                        containerColor =
                            MaterialTheme.colorScheme
                                .primaryContainer,
                    ),
            )
        },
    ) { padding ->
        if (state.isLoading) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                horizontalAlignment =
                    Alignment.CenterHorizontally,
                verticalArrangement =
                    Arrangement.Center,
            ) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement =
                Arrangement.spacedBy(16.dp),
        ) {
            OperatorCard(
                name = state.operatorName,
                email = state.operatorEmail,
            )

            ShiftContextCard(
                siteName = state.siteName,
                shiftDate = state.shiftDate,
                shiftType = state.shiftType,
            )

            SettingsActionItem(
                icon = Icons.Default.SwapHoriz,
                title = stringResource(
                    R.string.settings_change_context,
                ),
                subtitle = stringResource(
                    R.string
                        .settings_change_context_desc,
                ),
                onClick = {
                    viewModel.onIntent(
                        SettingsIntent
                            .ChangeContextClicked,
                    )
                },
            )

            SettingsActionItem(
                icon = Icons.Default.DeleteSweep,
                title = stringResource(
                    R.string.settings_clear_cache,
                ),
                subtitle = stringResource(
                    R.string
                        .settings_clear_cache_desc,
                ),
                onClick = {
                    viewModel.onIntent(
                        SettingsIntent.ClearCacheClicked,
                    )
                },
            )

            HorizontalDivider()

            SettingsActionItem(
                icon = Icons.AutoMirrored.Filled.Logout,
                title = stringResource(
                    R.string.settings_logout,
                ),
                subtitle = stringResource(
                    R.string.settings_logout_desc,
                ),
                onClick = {
                    viewModel.onIntent(
                        SettingsIntent.LogoutClicked,
                    )
                },
                isDestructive = true,
            )

            Spacer(modifier = Modifier.height(16.dp))

            AppInfoRow(version = state.appVersion)

            if (state.error != null) {
                Text(
                    text = state.error!!,
                    color = MaterialTheme.colorScheme.error,
                    style =
                        MaterialTheme.typography.bodySmall,
                    modifier = Modifier
                        .padding(top = 8.dp),
                )
            }
        }
    }
}

@Composable
private fun OperatorCard(
    name: String,
    email: String,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor =
                MaterialTheme.colorScheme
                    .surfaceVariant,
        ),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment =
                Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Default.Person,
                contentDescription = null,
                modifier = Modifier.size(40.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = name.ifBlank {
                        stringResource(
                            R.string
                                .settings_operator,
                        )
                    },
                    style = MaterialTheme.typography
                        .titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                if (email.isNotBlank()) {
                    Text(
                        text = email,
                        style = MaterialTheme.typography
                            .bodySmall,
                        color = MaterialTheme.colorScheme
                            .onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun ShiftContextCard(
    siteName: String,
    shiftDate: String,
    shiftType: String,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor =
                MaterialTheme.colorScheme
                    .secondaryContainer,
        ),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
        ) {
            Text(
                text = stringResource(
                    R.string.settings_current_context,
                ),
                style = MaterialTheme.typography
                    .labelMedium,
                color = MaterialTheme.colorScheme
                    .onSecondaryContainer,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = siteName.ifBlank { "—" },
                style = MaterialTheme.typography
                    .titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = "$shiftDate · " +
                    if (shiftType == "day") {
                        stringResource(
                            R.string.context_shift_day,
                        )
                    } else {
                        stringResource(
                            R.string.context_shift_night,
                        )
                    },
                style = MaterialTheme.typography
                    .bodySmall,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsActionItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    isDestructive: Boolean = false,
) {
    val tint = if (isDestructive) {
        MaterialTheme.colorScheme.error
    } else {
        MaterialTheme.colorScheme.onSurface
    }

    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment =
                Alignment.CenterVertically,
        ) {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = tint,
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography
                        .bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = tint,
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography
                        .bodySmall,
                    color = MaterialTheme.colorScheme
                        .onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun AppInfoRow(version: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment =
            Alignment.CenterVertically,
    ) {
        Icon(
            Icons.Default.Info,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = MaterialTheme.colorScheme
                .onSurfaceVariant,
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = stringResource(
                R.string.settings_version,
                version,
            ),
            style = MaterialTheme.typography
                .bodySmall,
            color = MaterialTheme.colorScheme
                .onSurfaceVariant,
        )
    }
}

@Composable
private fun ConfirmDialog(
    title: String,
    text: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(text) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(
                    stringResource(
                        R.string.settings_confirm,
                    ),
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    stringResource(
                        R.string.return_cancel,
                    ),
                )
            }
        },
    )
}
