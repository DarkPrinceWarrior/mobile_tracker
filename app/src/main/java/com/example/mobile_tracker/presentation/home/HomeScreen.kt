package com.example.mobile_tracker.presentation.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Watch
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
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
    BottomNavItem("Возврат", Icons.Default.Watch),
    BottomNavItem("Выгрузка", Icons.Default.CloudUpload),
    BottomNavItem("Журнал", Icons.AutoMirrored.Filled.List),
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
    val state by viewModel.state.collectAsStateWithLifecycle()
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
                            style =
                                MaterialTheme.typography
                                    .titleMedium,
                        )
                        Text(
                            text = "${state.shiftDate} · " +
                                if (state.shiftType == "day") {
                                    "Дневная"
                                } else {
                                    "Ночная"
                                },
                            style =
                                MaterialTheme.typography
                                    .bodySmall,
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor =
                        MaterialTheme.colorScheme
                            .primaryContainer,
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
                                Icons.Default.CloudUpload,
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
                            Icons.AutoMirrored.Filled.Logout,
                            contentDescription = "Выход",
                        )
                    }
                },
            )
        },
        bottomBar = {
            NavigationBar {
                bottomNavItems.forEachIndexed {
                        index,
                        item,
                    ->
                    NavigationBarItem(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
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

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment =
                    Alignment.CenterHorizontally,
                verticalArrangement =
                    Arrangement.Center,
            ) {
                when (selectedTab) {
                    0 -> {
                        Text(
                            text = stringResource(
                                R.string.issue_title,
                            ),
                            style = MaterialTheme.typography
                                .headlineMedium,
                        )
                        Spacer(
                            modifier =
                                Modifier.height(16.dp),
                        )
                        Button(
                            onClick = onNavigateToIssue,
                            modifier =
                                Modifier.fillMaxWidth(),
                        ) {
                            Text(
                                stringResource(
                                    R.string
                                        .issue_navigate,
                                ),
                            )
                        }
                    }
                    1 -> {
                        Text(
                            text = stringResource(
                                R.string.return_title,
                            ),
                            style = MaterialTheme.typography
                                .headlineMedium,
                        )
                        Spacer(
                            modifier =
                                Modifier.height(16.dp),
                        )
                        Button(
                            onClick = onNavigateToReturn,
                            modifier =
                                Modifier.fillMaxWidth(),
                        ) {
                            Text(
                                stringResource(
                                    R.string
                                        .return_navigate,
                                ),
                            )
                        }
                    }
                    2 -> {
                        Text(
                            text = stringResource(
                                R.string.tab_upload,
                            ),
                            style = MaterialTheme.typography
                                .headlineMedium,
                        )
                        Spacer(
                            modifier =
                                Modifier.height(8.dp),
                        )
                        Text(
                            text = "Выберите часы " +
                                "для выгрузки данных",
                            style = MaterialTheme
                                .typography.bodyLarge,
                            color = MaterialTheme
                                .colorScheme
                                .onSurfaceVariant,
                        )
                        Spacer(
                            modifier =
                                Modifier.height(16.dp),
                        )
                        Button(
                            onClick =
                                onNavigateToDevices,
                            modifier =
                                Modifier.fillMaxWidth(),
                        ) {
                            Text("Часы на площадке")
                        }
                    }
                    3 -> {
                        Text(
                            text = stringResource(
                                R.string.journal_title,
                            ),
                            style = MaterialTheme.typography
                                .headlineMedium,
                        )
                        Spacer(
                            modifier =
                                Modifier.height(16.dp),
                        )
                        Button(
                            onClick = onNavigateToJournal,
                            modifier =
                                Modifier.fillMaxWidth(),
                        ) {
                            Text(
                                stringResource(
                                    R.string
                                        .journal_navigate,
                                ),
                            )
                        }
                    }
                    4 -> {
                        Text(
                            text = stringResource(
                                R.string.more_title,
                            ),
                            style = MaterialTheme.typography
                                .headlineMedium,
                        )
                        Spacer(
                            modifier =
                                Modifier.height(16.dp),
                        )
                        Button(
                            onClick = onNavigateToDevices,
                            modifier =
                                Modifier.fillMaxWidth(),
                        ) {
                            Text(
                                stringResource(
                                    R.string
                                        .more_devices,
                                ),
                            )
                        }
                        Spacer(
                            modifier =
                                Modifier.height(8.dp),
                        )
                        Button(
                            onClick =
                                onNavigateToEmployees,
                            modifier =
                                Modifier.fillMaxWidth(),
                        ) {
                            Text(
                                stringResource(
                                    R.string
                                        .more_employees,
                                ),
                            )
                        }
                        Spacer(
                            modifier =
                                Modifier.height(8.dp),
                        )
                        Button(
                            onClick =
                                onNavigateToSummary,
                            modifier =
                                Modifier.fillMaxWidth(),
                        ) {
                            Text(
                                stringResource(
                                    R.string
                                        .more_summary,
                                ),
                            )
                        }
                        Spacer(
                            modifier =
                                Modifier.height(8.dp),
                        )
                        Button(
                            onClick =
                                onNavigateToSettings,
                            modifier =
                                Modifier.fillMaxWidth(),
                        ) {
                            Text(
                                stringResource(
                                    R.string
                                        .more_settings,
                                ),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun OfflineBanner() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFC62828))
            .padding(
                horizontal = 16.dp,
                vertical = 8.dp,
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        Text(
            text = stringResource(
                R.string.offline_banner,
            ),
            color = Color.White,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
        )
    }
}
