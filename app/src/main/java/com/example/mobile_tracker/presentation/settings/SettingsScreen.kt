package com.example.mobile_tracker.presentation.settings

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.mobile_tracker.R
import com.example.mobile_tracker.presentation.common.AppScreenScaffold
import com.example.mobile_tracker.presentation.common.LoadingState
import com.example.mobile_tracker.presentation.common.MTCompactTopBar
import com.example.mobile_tracker.presentation.common.MTStatusBadge
import com.example.mobile_tracker.presentation.common.MTStatusTone
import com.example.mobile_tracker.presentation.common.StateCard
import com.example.mobile_tracker.ui.theme.AppLayout
import com.example.mobile_tracker.ui.theme.AppRadius
import com.example.mobile_tracker.ui.theme.AppSpacing
import com.example.mobile_tracker.ui.theme.danger
import kotlinx.coroutines.flow.collect
import org.koin.androidx.compose.koinViewModel

@Composable
fun SettingsScreen(
    onBack: (() -> Unit)? = null,
    onNavigateToLogin: () -> Unit = {},
    onNavigateToContextSelection: () -> Unit = {},
    viewModel: SettingsViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = androidx.compose.ui.platform.LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                SettingsEffect.NavigateToLogin -> onNavigateToLogin()
                SettingsEffect.NavigateToContextSelection -> onNavigateToContextSelection()
                is SettingsEffect.ShowMessage -> {}
            }
        }
    }

    if (state.showLogoutDialog) {
        ConfirmDialog(
            title = stringResource(R.string.settings_logout),
            text = stringResource(R.string.settings_logout_confirm),
            onConfirm = { viewModel.onIntent(SettingsIntent.LogoutConfirmed) },
            onDismiss = { viewModel.onIntent(SettingsIntent.LogoutDismissed) },
        )
    }

    if (state.showClearCacheDialog) {
        ConfirmDialog(
            title = stringResource(R.string.settings_clear_cache),
            text = stringResource(R.string.settings_clear_cache_confirm),
            onConfirm = { viewModel.onIntent(SettingsIntent.ClearCacheConfirmed) },
            onDismiss = { viewModel.onIntent(SettingsIntent.ClearCacheDismissed) },
        )
    }

    AppScreenScaffold(
        snackbarMessage = state.error,
        topBar = {
            MTCompactTopBar(
                title = stringResource(R.string.settings_title),
                subtitle = stringResource(R.string.settings_subtitle),
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
        if (state.isLoading) {
            LoadingState(modifier = Modifier.padding(padding))
            return@AppScreenScaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = AppLayout.screenPadding, vertical = AppSpacing.sm),
            verticalArrangement = Arrangement.spacedBy(AppSpacing.md),
        ) {
            SettingsHeroCard(
                name = state.operatorName,
                email = state.operatorEmail,
                siteName = state.siteName,
                shiftDate = state.shiftDate,
                shiftType = state.shiftType,
            )

            SettingsActionItem(
                icon = Icons.Default.SwapHoriz,
                title = stringResource(R.string.settings_change_context),
                subtitle = stringResource(R.string.settings_change_context_desc),
                onClick = { viewModel.onIntent(SettingsIntent.ChangeContextClicked) },
            )
            SettingsActionItem(
                icon = Icons.Default.DeleteSweep,
                title = stringResource(R.string.settings_clear_cache),
                subtitle = stringResource(R.string.settings_clear_cache_desc),
                onClick = { viewModel.onIntent(SettingsIntent.ClearCacheClicked) },
            )
            SettingsActionItem(
                icon = Icons.AutoMirrored.Filled.Logout,
                title = stringResource(R.string.settings_logout),
                subtitle = stringResource(R.string.settings_logout_desc),
                onClick = { viewModel.onIntent(SettingsIntent.LogoutClicked) },
                isDestructive = true,
            )

            Surface(
                shape = RoundedCornerShape(AppRadius.lg),
                color = MaterialTheme.colorScheme.surfaceContainer,
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(modifier = Modifier.size(AppSpacing.xxs))
                    Text(
                        text = stringResource(R.string.settings_version, state.appVersion),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            if (state.error != null) {
                StateCard(message = state.error!!, isError = true)
            }
        }
    }
}

@Composable
private fun SettingsHeroCard(
    name: String,
    email: String,
    siteName: String,
    shiftDate: String,
    shiftType: String,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(AppRadius.lg),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(AppLayout.cardPadding),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = name.ifBlank { stringResource(R.string.settings_operator) },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            MTStatusBadge(
                label = if (shiftType == "day") {
                    stringResource(R.string.context_shift_day)
                } else {
                    stringResource(R.string.context_shift_night)
                },
                tone = MTStatusTone.Neutral,
            )
        }
    }
}

@Composable
private fun SettingsActionItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    isDestructive: Boolean = false,
    trailing: (@Composable () -> Unit)? = null,
) {
    val tint = if (isDestructive) {
        MaterialTheme.colorScheme.danger
    } else {
        MaterialTheme.colorScheme.tertiary
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(AppRadius.lg),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(AppLayout.cardPadding),
            horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .background(
                        color = if (isDestructive) {
                            MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.75f)
                        } else {
                            MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.55f)
                        },
                        shape = CircleShape,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = tint,
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = if (isDestructive) tint else MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            trailing?.invoke()
        }
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
                Text(stringResource(R.string.settings_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.return_cancel))
            }
        },
    )
}
