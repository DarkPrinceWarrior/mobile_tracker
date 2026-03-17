package com.example.mobile_tracker.presentation.qr_scan

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Videocam
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
import androidx.core.content.ContextCompat
import com.example.mobile_tracker.R
import com.example.mobile_tracker.presentation.common.AppScreenScaffold
import com.example.mobile_tracker.presentation.common.MTCard
import com.example.mobile_tracker.presentation.common.MTCompactTopBar
import com.example.mobile_tracker.presentation.common.MTStatusBadge
import com.example.mobile_tracker.presentation.common.MTStatusTone
import com.example.mobile_tracker.presentation.common.StateCard
import com.example.mobile_tracker.presentation.navigation.QrScanMode
import com.example.mobile_tracker.ui.theme.AppLayout
import com.example.mobile_tracker.ui.theme.AppRadius
import com.example.mobile_tracker.ui.theme.AppSpacing

@Composable
fun QrScanScreen(
    mode: QrScanMode,
    onBack: () -> Unit,
    onConfirmResult: (String) -> Unit,
) {
    val context = LocalContext.current
    var qrValue by rememberSaveable { mutableStateOf("") }
    var permissionDenied by rememberSaveable { mutableStateOf(false) }
    val cameraGranted =
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CAMERA,
        ) == PackageManager.PERMISSION_GRANTED

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        permissionDenied = !granted
    }

    AppScreenScaffold(
        topBar = {
            MTCompactTopBar(
                title = stringResource(R.string.qr_scan_title),
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
                            text = stringResource(R.string.qr_scan_hero_title),
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = modeInstruction(mode),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    MTStatusBadge(
                        label = if (cameraGranted) {
                            stringResource(R.string.qr_scan_badge_ready)
                        } else {
                            stringResource(R.string.qr_scan_badge_permission)
                        },
                        tone = if (cameraGranted) {
                            MTStatusTone.Success
                        } else {
                            MTStatusTone.Warning
                        },
                    )
                }

                QrPreviewFrame(
                    modifier = Modifier.fillMaxWidth(),
                    cameraGranted = cameraGranted,
                )

                if (!cameraGranted) {
                    Button(
                        onClick = {
                            permissionLauncher.launch(Manifest.permission.CAMERA)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(AppRadius.xl),
                    ) {
                        Icon(
                            imageVector = Icons.Default.Videocam,
                            contentDescription = null,
                        )
                        Spacer(modifier = Modifier.size(AppSpacing.xs))
                        Text(text = stringResource(R.string.qr_scan_permission_button))
                    }
                }
            }

            if (permissionDenied) {
                StateCard(
                    message = stringResource(R.string.qr_scan_permission_hint),
                    isError = false,
                )
                Button(
                    onClick = {
                        context.startActivity(
                            Intent(
                                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                Uri.fromParts("package", context.packageName, null),
                            ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(AppRadius.xl),
                ) {
                    Text(text = stringResource(R.string.permissions_open_app_settings))
                }
            }

            MTCard {
                Text(
                    text = stringResource(R.string.qr_scan_manual_title),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = stringResource(R.string.qr_scan_manual_subtitle),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                OutlinedTextField(
                    value = qrValue,
                    onValueChange = { qrValue = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = {
                        Text(text = stringResource(R.string.qr_scan_manual_label))
                    },
                    placeholder = {
                        Text(text = stringResource(R.string.qr_scan_manual_placeholder))
                    },
                    shape = RoundedCornerShape(AppRadius.lg),
                    singleLine = true,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(AppSpacing.xs),
                ) {
                    DemoValueButton(
                        modifier = Modifier.weight(1f),
                        text = stringResource(R.string.qr_scan_fill_demo),
                        onClick = { qrValue = demoValue(mode) },
                    )
                    Button(
                        onClick = { onConfirmResult(qrValue.trim()) },
                        modifier = Modifier.weight(1f),
                        enabled = qrValue.trim().isNotBlank(),
                        shape = RoundedCornerShape(AppRadius.xl),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                        ),
                    ) {
                        Text(text = stringResource(R.string.qr_scan_confirm))
                    }
                }
            }
        }
    }
}

@Composable
private fun QrPreviewFrame(
    modifier: Modifier,
    cameraGranted: Boolean,
) {
    Box(
        modifier = modifier
            .height(280.dp)
            .clip(RoundedCornerShape(AppRadius.xl))
            .background(Color(0xFF10241B)),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(210.dp)
                .clip(RoundedCornerShape(32.dp))
                .background(Color.White.copy(alpha = 0.06f)),
        ) {
            val cornerColor = if (cameraGranted) Color(0xFF18B270) else Color(0xFFE0C36A)
            ScannerCorner(
                modifier = Modifier.align(Alignment.TopStart),
                color = cornerColor,
            )
            ScannerCorner(
                modifier = Modifier.align(Alignment.TopEnd),
                color = cornerColor,
                mirroredHorizontally = true,
            )
            ScannerCorner(
                modifier = Modifier.align(Alignment.BottomStart),
                color = cornerColor,
                mirroredVertically = true,
            )
            ScannerCorner(
                modifier = Modifier.align(Alignment.BottomEnd),
                color = cornerColor,
                mirroredHorizontally = true,
                mirroredVertically = true,
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(AppSpacing.xs),
        ) {
            Icon(
                imageVector = Icons.Default.QrCodeScanner,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.92f),
                modifier = Modifier.size(40.dp),
            )
            Text(
                text = if (cameraGranted) {
                    stringResource(R.string.qr_scan_camera_ready)
                } else {
                    stringResource(R.string.qr_scan_camera_waiting)
                },
                style = MaterialTheme.typography.titleMedium,
                color = Color.White.copy(alpha = 0.96f),
            )
            Text(
                text = stringResource(R.string.qr_scan_frame_hint),
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.72f),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 32.dp),
            )
        }
    }
}

@Composable
private fun ScannerCorner(
    modifier: Modifier,
    color: Color,
    mirroredHorizontally: Boolean = false,
    mirroredVertically: Boolean = false,
) {
    Box(modifier = modifier.padding(18.dp)) {
        val horizontalAlignment = if (mirroredHorizontally) Alignment.CenterEnd else Alignment.CenterStart
        val verticalAlignment = if (mirroredVertically) Alignment.BottomStart else Alignment.TopStart
        Box(
            modifier = Modifier
                .align(verticalAlignment)
                .size(width = 42.dp, height = 6.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(color),
        )
        Box(
            modifier = Modifier
                .align(horizontalAlignment)
                .size(width = 6.dp, height = 42.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(color),
        )
    }
}

@Composable
private fun DemoValueButton(
    modifier: Modifier = Modifier,
    text: String,
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
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@Composable
private fun modeSubtitle(mode: QrScanMode): String = when (mode) {
    QrScanMode.IssueDevice -> stringResource(R.string.qr_scan_subtitle_issue)
    QrScanMode.ReturnDevice -> stringResource(R.string.qr_scan_subtitle_return)
    QrScanMode.UploadDevice -> stringResource(R.string.qr_scan_subtitle_upload)
    QrScanMode.RegisterWatch -> stringResource(R.string.qr_scan_subtitle_register_watch)
}

@Composable
private fun modeInstruction(mode: QrScanMode): String = when (mode) {
    QrScanMode.IssueDevice -> stringResource(R.string.qr_scan_instruction_issue)
    QrScanMode.ReturnDevice -> stringResource(R.string.qr_scan_instruction_return)
    QrScanMode.UploadDevice -> stringResource(R.string.qr_scan_instruction_upload)
    QrScanMode.RegisterWatch -> stringResource(R.string.qr_scan_instruction_register_watch)
}

private fun demoValue(mode: QrScanMode): String = when (mode) {
    QrScanMode.IssueDevice -> "WT-0007"
    QrScanMode.ReturnDevice -> "WT-0003"
    QrScanMode.UploadDevice -> "WT-0005"
    QrScanMode.RegisterWatch -> "{\"device_id\":\"WT-0EC22895\",\"model\":\"sdk_gwear_x86_64\",\"firmware\":\"Wear OS 36\",\"app_version\":\"1.0.0\"}"
}
