package com.example.mobile_tracker.presentation.home

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.PhonelinkSetup
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Watch
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.mobile_tracker.R
import com.example.mobile_tracker.presentation.common.AppScreenScaffold
import com.example.mobile_tracker.presentation.common.MTCard
import com.example.mobile_tracker.presentation.common.MTMetricCard
import com.example.mobile_tracker.presentation.common.MTSectionHeader
import com.example.mobile_tracker.presentation.common.MTStatusBadge
import com.example.mobile_tracker.presentation.common.MTStatusTone
import com.example.mobile_tracker.presentation.common.MTTopStatusBar
import com.example.mobile_tracker.presentation.common.StateCard
import com.example.mobile_tracker.presentation.common.rememberIsTablet
import com.example.mobile_tracker.ui.theme.AppLayout
import com.example.mobile_tracker.ui.theme.AppRadius
import com.example.mobile_tracker.ui.theme.AppSpacing
import com.example.mobile_tracker.ui.theme.danger
import com.example.mobile_tracker.ui.theme.warning
import org.koin.androidx.compose.koinViewModel

private data class BottomNavItem(
    val destination: HomeDestination,
    val titleRes: Int,
    val icon: ImageVector,
)

enum class HomeDestination {
    ISSUE,
    RETURN,
    JOURNAL,
    MORE,
}

private val bottomNavItems = listOf(
    BottomNavItem(HomeDestination.ISSUE, R.string.tab_issue, Icons.Default.Watch),
    BottomNavItem(HomeDestination.RETURN, R.string.tab_return, Icons.Default.Replay),
    BottomNavItem(
        HomeDestination.JOURNAL,
        R.string.tab_log,
        Icons.AutoMirrored.Filled.List,
    ),
    BottomNavItem(HomeDestination.MORE, R.string.tab_more, Icons.Default.MoreHoriz),
)

@Composable
fun HomeScreen(
    onLogout: () -> Unit,
    onNavigateToDevices: () -> Unit = {},
    onNavigateToEmployees: () -> Unit = {},
    onNavigateToMonitoring: () -> Unit = {},
    onNavigateToMaps: () -> Unit = {},
    onNavigateToIssue: () -> Unit = {},
    onNavigateToReturn: () -> Unit = {},
    onNavigateToJournal: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
    onNavigateToAlerts: () -> Unit = {},
    onNavigateToRegisterWatch: () -> Unit = {},
    viewModel: HomeViewModel = koinViewModel(),
) {
    val state by viewModel.state
        .collectAsStateWithLifecycle()
    var selectedDestination by rememberSaveable {
        mutableStateOf(HomeDestination.ISSUE)
    }
    val isTablet = rememberIsTablet()
    val shiftTypeLabel = if (state.shiftType == "day") {
        stringResource(R.string.context_shift_day)
    } else {
        stringResource(R.string.context_shift_night)
    }
    val statusText = when {
        !state.isOnline -> stringResource(R.string.shell_offline)
        state.pendingPacketsCount > 0 -> stringResource(
            R.string.shell_pending_packets_short,
            state.pendingPacketsCount,
        )
        else -> null
    }
    val statusColor = when {
        !state.isOnline -> MaterialTheme.colorScheme.danger
        state.pendingPacketsCount > 0 -> MaterialTheme.colorScheme.warning
        else -> MaterialTheme.colorScheme.outlineVariant
    }
    val operatorLabel = state.operatorName.ifBlank {
        stringResource(R.string.shell_unknown_operator)
    }
    AppScreenScaffold(
        topBar = {
            MTTopStatusBar(
                leadingText = statusText,
                trailingText = operatorLabel,
                statusColor = statusColor,
            )
        },
        bottomBar = {
            if (!isTablet) {
                HomeBottomBar(
                    selectedDestination = selectedDestination,
                    pendingPacketsCount = state.pendingPacketsCount,
                    alertCount = state.totalAlertsCount,
                    onDestinationSelected = { selectedDestination = it },
                )
            }
        },
    ) { padding ->
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            if (isTablet) {
                HomeNavigationRail(
                    selectedDestination = selectedDestination,
                    pendingPacketsCount = state.pendingPacketsCount,
                    alertCount = state.totalAlertsCount,
                    onDestinationSelected = { selectedDestination = it },
                )
            }

            Column(
                modifier = Modifier.fillMaxSize(),
            ) {
                AnimatedContent<HomeDestination>(
                    targetState = selectedDestination,
                    label = "tab_content",
                    transitionSpec = {
                        fadeIn() togetherWith fadeOut()
                    },
                ) { destination ->
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(
                                rememberScrollState(),
                            )
                            .padding(horizontal = AppLayout.screenPadding)
                            .padding(top = AppSpacing.sm, bottom = AppSpacing.lg),
                        verticalArrangement =
                            Arrangement.spacedBy(16.dp),
                    ) {
                        if (destination == HomeDestination.ISSUE) {
                            HomeOverviewSection(
                                state = state,
                                shiftTypeLabel = shiftTypeLabel,
                            )
                        }
                        when (destination) {
                            HomeDestination.ISSUE -> IssueTabContent(
                                onNavigateToIssue = onNavigateToIssue,
                                onNavigateToEmployees = onNavigateToEmployees,
                                onNavigateToDevices = onNavigateToDevices,
                            )
                            HomeDestination.RETURN -> ReturnTabContent(
                                onNavigateToReturn = onNavigateToReturn,
                            )
                            HomeDestination.JOURNAL -> JournalTabContent(
                                onNavigateToJournal = onNavigateToJournal,
                            )
                            HomeDestination.MORE -> MoreTabContent(
                                onNavigateToMonitoring = onNavigateToMonitoring,
                                onNavigateToMaps = onNavigateToMaps,
                                onNavigateToSettings = onNavigateToSettings,
                                onNavigateToRegisterWatch = onNavigateToRegisterWatch,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeOverviewSection(
    state: HomeState,
    shiftTypeLabel: String,
) {
    MTSectionHeader(
        title = stringResource(R.string.home_overview_title),
    )

    MTCard {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(AppSpacing.sm),
        ) {
            Text(
                text = state.siteName.ifBlank { stringResource(R.string.context_site_label) },
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Clip,
            )
            Text(
                text = buildString {
                    if (state.shiftDate.isNotBlank()) {
                        append(state.shiftDate)
                        append(" · ")
                    }
                    append(shiftTypeLabel)
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            MTStatusBadge(
                label = if (state.isOnline) {
                    stringResource(R.string.home_network_online)
                } else {
                    stringResource(R.string.home_network_offline)
                },
                tone = if (state.isOnline) {
                    MTStatusTone.Success
                } else {
                    MTStatusTone.Danger
                },
            )
        }
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(AppSpacing.xs),
    ) {
        MTMetricCard(
            title = stringResource(R.string.summary_issued),
            value = state.issuedCount.toString(),
            subtitle = stringResource(
                R.string.home_metric_active_subtitle,
                state.uploadRequiredCount,
            ),
            tone = MTStatusTone.Info,
            modifier = Modifier.weight(1f),
        )
        MTMetricCard(
            title = stringResource(R.string.summary_returned),
            value = state.returnedCount.toString(),
            subtitle = stringResource(
                R.string.home_summary_returned_subtitle,
                state.activeBindingsCount,
            ),
            tone = MTStatusTone.Success,
            modifier = Modifier.weight(1f),
        )
    }
}


@Composable
private fun HomeBottomBar(
    selectedDestination: HomeDestination,
    pendingPacketsCount: Int,
    alertCount: Int,
    onDestinationSelected: (HomeDestination) -> Unit,
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        tonalElevation = 0.dp,
        shadowElevation = 8.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = AppSpacing.xs, vertical = AppSpacing.sm),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            bottomNavItems.forEach { item ->
                HomeNavItem(
                    modifier = Modifier.weight(1f),
                    title = stringResource(item.titleRes),
                    icon = item.icon,
                    selected = selectedDestination == item.destination,
                    badgeCount = if (
                        item.destination == HomeDestination.MORE &&
                            alertCount > 0
                    ) {
                        alertCount
                    } else {
                        null
                    },
                    vertical = false,
                    onClick = { onDestinationSelected(item.destination) },
                )
            }
        }
    }
}

@Composable
private fun HomeNavigationRail(
    selectedDestination: HomeDestination,
    pendingPacketsCount: Int,
    alertCount: Int,
    onDestinationSelected: (HomeDestination) -> Unit,
) {
    Surface(
        modifier = Modifier
            .padding(start = AppLayout.screenPadding, top = AppSpacing.sm, bottom = AppSpacing.sm)
            .fillMaxHeight(),
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        shape = RoundedCornerShape(AppRadius.xl),
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = AppSpacing.xs, vertical = AppSpacing.sm),
            verticalArrangement = Arrangement.spacedBy(AppSpacing.xs),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            bottomNavItems.forEach { item ->
                HomeNavItem(
                    title = stringResource(item.titleRes),
                    icon = item.icon,
                    selected = selectedDestination == item.destination,
                    badgeCount = if (
                        item.destination == HomeDestination.MORE &&
                            alertCount > 0
                    ) {
                        alertCount
                    } else {
                        null
                    },
                    vertical = true,
                    onClick = { onDestinationSelected(item.destination) },
                )
            }
        }
    }
}

@Composable
private fun HomeNavItem(
    modifier: Modifier = Modifier,
    title: String,
    icon: ImageVector,
    selected: Boolean,
    badgeCount: Int?,
    vertical: Boolean,
    onClick: () -> Unit,
) {
    val activeColor = MaterialTheme.colorScheme.tertiary
    val inactiveColor = MaterialTheme.colorScheme.onSurfaceVariant
    val contentColor = if (selected) activeColor else inactiveColor

    val interactionSource = remember { MutableInteractionSource() }
    val outerModifier = if (vertical) {
        Modifier
            .fillMaxWidth()
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
    } else {
        modifier
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
    }

    val arrangement = if (vertical) Arrangement.spacedBy(AppSpacing.xs) else Arrangement.spacedBy(6.dp)

    Box(
        modifier = outerModifier,
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            shape = RoundedCornerShape(if (vertical) AppRadius.lg else AppRadius.xl),
            color = Color.Transparent,
        ) {
            Column(
                modifier = Modifier.padding(
                    horizontal = if (vertical) AppSpacing.sm else 6.dp,
                    vertical = if (vertical) AppSpacing.sm else 10.dp,
                ),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = arrangement,
            ) {
                BadgedBox(
                    badge = {
                        if (badgeCount != null) {
                            Badge(
                                containerColor = MaterialTheme.colorScheme.danger,
                                contentColor = MaterialTheme.colorScheme.onError,
                            ) {
                                Text(badgeCount.toString())
                            }
                        }
                    },
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = contentColor,
                    )
                }
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelSmall,
                    color = contentColor,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun HomePrimaryActionCard(
    icon: ImageVector?,
    statusLabel: String?,
    statusTone: MTStatusTone = MTStatusTone.Neutral,
    title: String,
    subtitle: String,
    buttonText: String,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(AppRadius.xl),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primary,
        ),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = AppSpacing.lg, vertical = AppSpacing.md),
            verticalArrangement = Arrangement.spacedBy(AppSpacing.sm),
        ) {
            if (statusLabel != null || icon != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top,
                ) {
                    if (statusLabel != null) {
                        MTStatusBadge(
                            label = statusLabel,
                            tone = statusTone,
                        )
                    } else {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                    if (icon != null) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.1f)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                modifier = Modifier.size(24.dp),
                                tint = MaterialTheme.colorScheme.onPrimary,
                            )
                        }
                    }
                }
            }

            Text(
                text = title,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Normal,
                color = MaterialTheme.colorScheme.onPrimary,
            )

            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.74f),
            )

            Button(
                onClick = onClick,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(AppRadius.xl),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.onPrimary,
                    contentColor = MaterialTheme.colorScheme.primary,
                ),
                elevation = ButtonDefaults.buttonElevation(
                    defaultElevation = 2.dp,
                ),
            ) {
                Text(
                    text = buttonText,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(modifier = Modifier.width(AppSpacing.xs))
                Icon(
                    Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}

@Composable
private fun HomeQuickActionCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(AppRadius.lg),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
        ),
    ) {
        Column(
            modifier = Modifier.padding(AppLayout.cardPadding),
            verticalArrangement = Arrangement.spacedBy(AppSpacing.sm),
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.7f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.tertiary,
                )
            }
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun HomeQuickActionsRow(
    title: String? = null,
    first: @Composable () -> Unit,
    second: @Composable () -> Unit,
) {
    if (title != null) {
        MTSectionHeader(title = title)
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(AppSpacing.xs),
    ) {
        Box(modifier = Modifier.weight(1f)) { first() }
        Box(modifier = Modifier.weight(1f)) { second() }
    }
}

@Composable
private fun IssueTabContent(
    onNavigateToIssue: () -> Unit,
    onNavigateToEmployees: () -> Unit,
    onNavigateToDevices: () -> Unit,
) {
    HomePrimaryActionCard(
        icon = null,
        statusLabel = null,
        title = stringResource(R.string.issue_title),
        subtitle = stringResource(R.string.home_issue_subtitle),
        buttonText = stringResource(R.string.issue_navigate),
        onClick = onNavigateToIssue,
    )

    HomeQuickActionsRow(
        first = {
            HomeQuickActionCard(
                icon = Icons.Default.People,
                title = stringResource(R.string.more_employees),
                subtitle = stringResource(R.string.home_shortcut_employees_desc),
                onClick = onNavigateToEmployees,
            )
        },
        second = {
            HomeQuickActionCard(
                icon = Icons.Default.Devices,
                title = stringResource(R.string.more_devices),
                subtitle = stringResource(R.string.home_shortcut_devices_desc),
                onClick = onNavigateToDevices,
            )
        },
    )
}

@Composable
private fun ReturnTabContent(
    onNavigateToReturn: () -> Unit,
) {
    HomePrimaryActionCard(
        icon = null,
        statusLabel = null,
        title = stringResource(R.string.return_title),
        subtitle = stringResource(R.string.home_return_subtitle),
        buttonText = stringResource(R.string.return_navigate),
        onClick = onNavigateToReturn,
    )
}


@Composable
private fun JournalTabContent(
    onNavigateToJournal: () -> Unit,
) {
    HomePrimaryActionCard(
        icon = null,
        statusLabel = null,
        title = stringResource(R.string.journal_title),
        subtitle = stringResource(R.string.home_journal_subtitle),
        buttonText = stringResource(R.string.journal_navigate),
        onClick = onNavigateToJournal,
    )
}

@Composable
private fun MoreTabContent(
    onNavigateToMonitoring: () -> Unit,
    onNavigateToMaps: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToRegisterWatch: () -> Unit,
) {
    MoreMenuItem(
        icon = Icons.Default.Bolt,
        title = stringResource(R.string.more_monitoring),
        subtitle = stringResource(R.string.home_more_monitoring_subtitle),
        onClick = onNavigateToMonitoring,
    )
    MoreMenuItem(
        icon = Icons.Default.PhonelinkSetup,
        title = stringResource(R.string.more_register_watch),
        subtitle = stringResource(R.string.home_more_register_watch_subtitle),
        onClick = onNavigateToRegisterWatch,
    )
    MoreMenuItem(
        icon = Icons.Default.Map,
        title = stringResource(R.string.more_maps),
        subtitle = stringResource(R.string.home_more_maps_subtitle),
        onClick = onNavigateToMaps,
    )
    MoreMenuItem(
        icon = Icons.Default.Settings,
        title = stringResource(R.string.more_settings),
        subtitle = stringResource(R.string.home_more_settings_subtitle),
        onClick = onNavigateToSettings,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MoreMenuItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
) {
    Card(
        onClick = onClick,
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
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(
                        MaterialTheme.colorScheme
                            .tertiaryContainer
                            .copy(alpha = 0.7f),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    modifier = Modifier.size(22.dp),
                    tint = MaterialTheme.colorScheme.tertiary,
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium,
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Icon(
                Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
