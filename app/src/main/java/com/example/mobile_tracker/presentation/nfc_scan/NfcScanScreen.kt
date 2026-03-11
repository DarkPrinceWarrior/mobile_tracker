package com.example.mobile_tracker.presentation.nfc_scan

import android.content.Intent
import android.content.Context
import android.nfc.NfcAdapter
import android.provider.Settings
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.mobile_tracker.R
import com.example.mobile_tracker.presentation.common.AppScreenScaffold
import com.example.mobile_tracker.presentation.common.MTCard
import com.example.mobile_tracker.presentation.common.MTCompactTopBar
import com.example.mobile_tracker.presentation.common.MTStatusBadge
import com.example.mobile_tracker.presentation.common.MTStatusTone
import com.example.mobile_tracker.presentation.common.StateCard
import com.example.mobile_tracker.presentation.navigation.NfcScanMode
import com.example.mobile_tracker.ui.theme.AppLayout
import com.example.mobile_tracker.ui.theme.AppRadius
import com.example.mobile_tracker.ui.theme.AppSpacing

@Composable
fun NfcScanScreen(
    mode: NfcScanMode,
    onBack: () -> Unit,
    onConfirmResult: (String) -> Unit,
) {
    val context = LocalContext.current
    val nfcAdapter = remember(context) { NfcAdapter.getDefaultAdapter(context) }
    val nfcSupported = nfcAdapter != null
    val nfcEnabled = nfcAdapter?.isEnabled == true
    var passValue by rememberSaveable { mutableStateOf("") }

    AppScreenScaffold(
        topBar = {
            MTCompactTopBar(
                title = stringResource(R.string.nfc_scan_title),
                subtitle = modeSubtitle(mode),
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.action_back),
                            tint = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(padding)
                .padding(horizontal = AppLayout.screenPadding, vertical = AppSpacing.sm),
            verticalArrangement = Arrangement.spacedBy(AppSpacing.md),
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
                            text = stringResource(R.string.nfc_scan_hero_title),
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = stringResource(R.string.nfc_scan_instruction),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    MTStatusBadge(
                        label = when {
                            nfcEnabled -> stringResource(R.string.nfc_scan_badge_ready)
                            nfcSupported -> stringResource(R.string.nfc_scan_badge_disabled)
                            else -> stringResource(R.string.nfc_scan_badge_unsupported)
                        },
                        tone = when {
                            nfcEnabled -> MTStatusTone.Success
                            nfcSupported -> MTStatusTone.Warning
                            else -> MTStatusTone.Neutral
                        },
                    )
                }

                NfcPreviewFrame(
                    modifier = Modifier.fillMaxWidth(),
                    enabled = nfcEnabled,
                )
            }

            when {
                !nfcSupported -> StateCard(
                    message = stringResource(R.string.nfc_scan_not_supported_hint),
                    isError = false,
                )

                !nfcEnabled -> StateCard(
                    message = stringResource(R.string.nfc_scan_disabled_hint),
                    isError = false,
                )
            }

            if (nfcSupported && !nfcEnabled) {
                Button(
                    onClick = {
                        context.startActivity(
                            Intent(Settings.ACTION_NFC_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(AppRadius.xl),
                ) {
                    Text(text = stringResource(R.string.permissions_open_nfc_settings))
                }
            }

            MTCard {
                Text(
                    text = stringResource(R.string.nfc_scan_manual_title),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = stringResource(R.string.nfc_scan_manual_subtitle),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                OutlinedTextField(
                    value = passValue,
                    onValueChange = { passValue = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = {
                        Text(text = stringResource(R.string.nfc_scan_manual_label))
                    },
                    placeholder = {
                        Text(text = stringResource(R.string.nfc_scan_manual_placeholder))
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(AppRadius.lg),
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(AppSpacing.xs),
                ) {
                    DemoNfcButton(
                        modifier = Modifier.weight(1f),
                        onClick = { passValue = demoValue(mode, context) },
                    )
                    Button(
                        onClick = { onConfirmResult(passValue.trim()) },
                        modifier = Modifier.weight(1f),
                        enabled = passValue.trim().isNotBlank(),
                        shape = RoundedCornerShape(AppRadius.xl),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                        ),
                    ) {
                        Text(text = stringResource(R.string.nfc_scan_confirm))
                    }
                }
            }
        }
    }
}

@Composable
private fun NfcPreviewFrame(
    modifier: Modifier,
    enabled: Boolean,
) {
    Box(
        modifier = modifier
            .height(280.dp)
            .clip(RoundedCornerShape(AppRadius.xl))
            .background(Color(0xFF0F201A)),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(220.dp)
                .clip(RoundedCornerShape(36.dp))
                .background(Color.White.copy(alpha = 0.05f)),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .size(124.dp)
                    .clip(CircleShape)
                    .background(
                        if (enabled) Color(0xFF18B270).copy(alpha = 0.14f)
                        else Color.White.copy(alpha = 0.08f),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Default.Badge,
                    contentDescription = null,
                    tint = if (enabled) Color(0xFF18B270) else Color.White.copy(alpha = 0.82f),
                    modifier = Modifier.size(52.dp),
                )
            }
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(AppSpacing.xs),
        ) {
            Text(
                text = if (enabled) {
                    stringResource(R.string.nfc_scan_ready)
                } else {
                    stringResource(R.string.nfc_scan_waiting)
                },
                style = MaterialTheme.typography.titleMedium,
                color = Color.White.copy(alpha = 0.96f),
            )
            Text(
                text = stringResource(R.string.nfc_scan_frame_hint),
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.72f),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 28.dp),
            )
        }
    }
}

@Composable
private fun DemoNfcButton(
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(AppRadius.xl),
        color = MaterialTheme.colorScheme.surfaceContainer,
        onClick = onClick,
    ) {
        Box(
            modifier = Modifier.padding(vertical = 14.dp),
            contentAlignment = Alignment.Center,
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(AppSpacing.xs),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Default.Badge,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.tertiary,
                )
                Text(
                    text = stringResource(R.string.nfc_scan_fill_demo),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    }
}

@Composable
private fun modeSubtitle(mode: NfcScanMode): String = when (mode) {
    NfcScanMode.IdentifyEmployee -> stringResource(R.string.nfc_scan_subtitle_issue)
}

private fun demoValue(
    mode: NfcScanMode,
    context: Context,
): String = when (mode) {
    NfcScanMode.IdentifyEmployee -> {
        val adapter = NfcAdapter.getDefaultAdapter(context)
        if (adapter != null) "A-1001" else "B-2001"
    }
}
