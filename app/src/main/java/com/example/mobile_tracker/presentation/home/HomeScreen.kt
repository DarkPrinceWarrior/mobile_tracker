package com.example.mobile_tracker.presentation.home

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
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
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.MoreHoriz
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
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.mobile_tracker.R
import org.koin.androidx.compose.koinViewModel

private data class BottomNavItem(
    val title: String,
    val icon: ImageVector,
)

private val bottomNavItems = listOf(
    BottomNavItem("Выдача", Icons.Default.Watch),
    BottomNavItem("Возврат", Icons.Default.Replay),
    BottomNavItem(
        "Выгрузка",
        Icons.Default.CloudUpload,
    ),
    BottomNavItem(
        "Журнал",
        Icons.AutoMirrored.Filled.List,
    ),
    BottomNavItem("Ещё", Icons.Default.MoreHoriz),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onLogout: () -> Unit,
    onNavigateToDevices: () -> Unit = {},
    onNavigateToEmployees: () -> Unit = {},
    onNavigateToIssue: () -> Unit = {},
    onNavigateToReturn: () -> Unit = {},
    onNavigateToJournal: () -> Unit = {},
    onNavigateToSummary: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
    viewModel: HomeViewModel = koinViewModel(),
) {
    val state by viewModel.state
        .collectAsStateWithLifecycle()
    var selectedTab by remember { mutableIntStateOf(0) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = state.siteName.ifBlank {
                                "Площадка"
                            },
                            style = MaterialTheme
                                .typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            text = "${state.shiftDate} · " +
                                if (state.shiftType ==
                                    "day"
                                ) {
                                    "Дневная"
                                } else {
                                    "Ночная"
                                },
                            style = MaterialTheme
                                .typography.bodySmall,
                            color = MaterialTheme
                                .colorScheme
                                .onSurfaceVariant,
                        )
                    }
                },
                colors =
                    TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme
                            .colorScheme.surface,
                    ),
                actions = {
                    if (state.pendingPacketsCount > 0) {
                        BadgedBox(
                            badge = {
                                Badge {
                                    Text(
                                        state
                                            .pendingPacketsCount
                                            .toString(),
                                    )
                                }
                            },
                        ) {
                            Icon(
                                Icons.Default
                                    .CloudUpload,
                                contentDescription =
                                    stringResource(
                                        R.string
                                            .offline_pending_count,
                                        state
                                            .pendingPacketsCount,
                                    ),
                                modifier = Modifier
                                    .size(24.dp),
                            )
                        }
                    }
                    IconButton(onClick = onLogout) {
                        Icon(
                            Icons.AutoMirrored
                                .Filled.Logout,
                            contentDescription = "Выход",
                        )
                    }
                },
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme
                    .colorScheme.surfaceContainer,
                tonalElevation = 0.dp,
            ) {
                bottomNavItems.forEachIndexed {
                        index,
                        item,
                    ->
                    NavigationBarItem(
                        selected =
                            selectedTab == index,
                        onClick = {
                            selectedTab = index
                        },
                        icon = {
                            Icon(
                                item.icon,
                                contentDescription =
                                    item.title,
                            )
                        },
                        label = { Text(item.title) },
                    )
                }
            }
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            AnimatedVisibility(
                visible = !state.isOnline,
                enter = expandVertically(),
                exit = shrinkVertically(),
            ) {
                OfflineBanner()
            }

            AnimatedContent(
                targetState = selectedTab,
                label = "tab_content",
                transitionSpec = {
                    fadeIn() togetherWith fadeOut()
                },
            ) { tab ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(
                            rememberScrollState(),
                        )
                        .padding(20.dp),
                    verticalArrangement =
                        Arrangement.spacedBy(16.dp),
                ) {
                    when (tab) {
                        0 -> IssueTabContent(
                            onNavigateToIssue,
                        )
                        1 -> ReturnTabContent(
                            onNavigateToReturn,
                        )
                        2 -> UploadTabContent(
                            onNavigateToDevices,
                        )
                        3 -> JournalTabContent(
                            onNavigateToJournal,
                        )
                        4 -> MoreTabContent(
                            onNavigateToDevices =
                                onNavigateToDevices,
                            onNavigateToEmployees =
                                onNavigateToEmployees,
                            onNavigateToSummary =
                                onNavigateToSummary,
                            onNavigateToSettings =
                                onNavigateToSettings,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TabHeroCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    buttonText: String,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme
                .primaryContainer.copy(alpha = 0.5f),
        ),
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment =
                Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(
                        MaterialTheme.colorScheme
                            .primaryContainer,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(36.dp),
                    tint = MaterialTheme.colorScheme
                        .onPrimaryContainer,
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = title,
                style = MaterialTheme.typography
                    .headlineSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = subtitle,
                style = MaterialTheme.typography
                    .bodyMedium,
                color = MaterialTheme.colorScheme
                    .onSurfaceVariant,
                textAlign = TextAlign.Center,
            )

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = onClick,
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.small,
                elevation =
                    ButtonDefaults.buttonElevation(
                        defaultElevation = 2.dp,
                    ),
            ) {
                Text(
                    text = buttonText,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(modifier = Modifier.width(8.dp))
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
private fun IssueTabContent(
    onNavigateToIssue: () -> Unit,
) {
    TabHeroCard(
        icon = Icons.Default.Watch,
        title = stringResource(R.string.issue_title),
        subtitle = "Выдайте часы сотруднику " +
            "по табельному номеру или ФИО",
        buttonText = stringResource(
            R.string.issue_navigate,
        ),
        onClick = onNavigateToIssue,
    )
}

@Composable
private fun ReturnTabContent(
    onNavigateToReturn: () -> Unit,
) {
    TabHeroCard(
        icon = Icons.Default.Replay,
        title = stringResource(R.string.return_title),
        subtitle = "Примите часы обратно " +
            "после окончания смены",
        buttonText = stringResource(
            R.string.return_navigate,
        ),
        onClick = onNavigateToReturn,
    )
}

@Composable
private fun UploadTabContent(
    onNavigateToDevices: () -> Unit,
) {
    TabHeroCard(
        icon = Icons.Default.CloudUpload,
        title = stringResource(R.string.tab_upload),
        subtitle = "Выберите часы " +
            "для выгрузки данных через BLE",
        buttonText = "Часы на площадке",
        onClick = onNavigateToDevices,
    )
}

@Composable
private fun JournalTabContent(
    onNavigateToJournal: () -> Unit,
) {
    TabHeroCard(
        icon = Icons.AutoMirrored.Filled.List,
        title = stringResource(R.string.journal_title),
        subtitle = "Просмотрите историю " +
            "всех операций за смену",
        buttonText = stringResource(
            R.string.journal_navigate,
        ),
        onClick = onNavigateToJournal,
    )
}

@Composable
private fun MoreTabContent(
    onNavigateToDevices: () -> Unit,
    onNavigateToEmployees: () -> Unit,
    onNavigateToSummary: () -> Unit,
    onNavigateToSettings: () -> Unit,
) {
    Text(
        text = stringResource(R.string.more_title),
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.Bold,
    )

    MoreMenuItem(
        icon = Icons.Default.Devices,
        title = stringResource(R.string.more_devices),
        subtitle = "Все часы, привязанные " +
            "к площадке",
        onClick = onNavigateToDevices,
    )
    MoreMenuItem(
        icon = Icons.Default.People,
        title = stringResource(R.string.more_employees),
        subtitle = "База сотрудников подрядчиков",
        onClick = onNavigateToEmployees,
    )
    MoreMenuItem(
        icon = Icons.Default.BarChart,
        title = stringResource(R.string.more_summary),
        subtitle = "Статистика текущей смены",
        onClick = onNavigateToSummary,
    )
    MoreMenuItem(
        icon = Icons.Default.Settings,
        title = stringResource(R.string.more_settings),
        subtitle = "Профиль, контекст, выход",
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
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme
                .surfaceContainerLow,
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment =
                Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(
                        MaterialTheme.colorScheme
                            .primaryContainer
                            .copy(alpha = 0.6f),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    modifier = Modifier.size(22.dp),
                    tint = MaterialTheme.colorScheme
                        .primary,
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography
                        .titleSmall,
                    fontWeight = FontWeight.Medium,
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography
                        .bodySmall,
                    color = MaterialTheme.colorScheme
                        .onSurfaceVariant,
                )
            }
            Icon(
                Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme
                    .onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun OfflineBanner() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                MaterialTheme.colorScheme.errorContainer,
            )
            .padding(
                horizontal = 16.dp,
                vertical = 10.dp,
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        Icon(
            Icons.Default.CloudOff,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
            tint = MaterialTheme.colorScheme
                .onErrorContainer,
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = stringResource(
                R.string.offline_banner,
            ),
            color = MaterialTheme.colorScheme
                .onErrorContainer,
            style = MaterialTheme.typography.labelLarge,
            textAlign = TextAlign.Center,
        )
    }
}
