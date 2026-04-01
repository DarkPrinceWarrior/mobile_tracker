package com.example.mobile_tracker.presentation.qr_scan

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import android.util.Size
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TextButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.mobile_tracker.R
import com.example.mobile_tracker.presentation.common.AppScreenScaffold
import com.example.mobile_tracker.presentation.common.MTCompactTopBar
import com.example.mobile_tracker.presentation.common.MTStatusBadge
import com.example.mobile_tracker.presentation.common.MTStatusTone
import com.example.mobile_tracker.presentation.navigation.QrScanMode
import com.example.mobile_tracker.ui.theme.AppLayout
import com.example.mobile_tracker.ui.theme.AppRadius
import com.example.mobile_tracker.ui.theme.AppSpacing
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.Executors

@Composable
fun QrScanScreen(
    mode: QrScanMode,
    onBack: () -> Unit,
    onConfirmResult: (String) -> Unit,
) {
    val context = LocalContext.current
    var autoConfirmed by rememberSaveable { mutableStateOf(false) }
    var permissionDenied by rememberSaveable { mutableStateOf(false) }
    var cameraGranted by rememberSaveable {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA,
            ) == PackageManager.PERMISSION_GRANTED,
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        cameraGranted = granted
        permissionDenied = !granted
    }

    AppScreenScaffold(
        topBar = {
            MTCompactTopBar(
                title = stringResource(R.string.qr_scan_title),
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
            // Camera preview
            QrPreviewFrame(
                modifier = Modifier.fillMaxWidth(),
                cameraGranted = cameraGranted,
                onBarcodeDetected = { value ->
                    if (!autoConfirmed) {
                        autoConfirmed = true
                        onConfirmResult(value)
                    }
                },
            )

            // Status badge
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
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

            // Instruction text
            Text(
                text = modeInstruction(mode),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
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

            if (permissionDenied) {
                PermissionInfoCard(
                    title = stringResource(R.string.qr_scan_badge_permission),
                    message = stringResource(R.string.qr_scan_permission_hint),
                    actionLabel = stringResource(R.string.permissions_open_app_settings),
                    onActionClick = {
                        context.startActivity(
                            Intent(
                                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                Uri.fromParts("package", context.packageName, null),
                            ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                        )
                    },
                )
            }
        }
    }
}

@Composable
private fun PermissionInfoCard(
    title: String,
    message: String,
    actionLabel: String,
    onActionClick: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(AppRadius.xl),
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
                    .size(42.dp)
                    .clip(RoundedCornerShape(AppRadius.lg))
                    .background(MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.6f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.tertiary,
                )
            }
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            TextButton(
                onClick = onActionClick,
                modifier = Modifier.align(Alignment.End),
            ) {
                Text(actionLabel)
            }
        }
    }
}

@Composable
@androidx.annotation.OptIn(androidx.camera.core.ExperimentalGetImage::class)
private fun QrPreviewFrame(
    modifier: Modifier,
    cameraGranted: Boolean,
    onBarcodeDetected: (String) -> Unit,
) {
    Box(
        modifier = modifier
            .height(280.dp)
            .clip(RoundedCornerShape(AppRadius.xl))
            .background(Color(0xFF0D1F17)),
        contentAlignment = Alignment.Center,
    ) {
        if (cameraGranted) {
            val context = LocalContext.current
            val lifecycleOwner = LocalLifecycleOwner.current
            val cameraExecutor = Executors.newSingleThreadExecutor()

            val scannerOptions = BarcodeScannerOptions.Builder()
                .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
                .build()
            val barcodeScanner = BarcodeScanning.getClient(scannerOptions)

            DisposableEffect(Unit) {
                onDispose {
                    cameraExecutor.shutdown()
                    barcodeScanner.close()
                }
            }

            AndroidView(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(AppRadius.xl)),
                factory = { ctx ->
                    val previewView = PreviewView(ctx).apply {
                        scaleType = PreviewView.ScaleType.FILL_CENTER
                    }
                    val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                    cameraProviderFuture.addListener(
                        {
                            val cameraProvider = cameraProviderFuture.get()
                            val preview = Preview.Builder()
                                .build()
                                .also { it.surfaceProvider = previewView.surfaceProvider }

                            val imageAnalysis = ImageAnalysis.Builder()
                                .setTargetResolution(Size(1280, 720))
                                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                                .build()
                                .also { analysis ->
                                    analysis.setAnalyzer(cameraExecutor) { imageProxy ->
                                        val mediaImage = imageProxy.image
                                        if (mediaImage != null) {
                                            val image = InputImage.fromMediaImage(
                                                mediaImage,
                                                imageProxy.imageInfo.rotationDegrees,
                                            )
                                            barcodeScanner.process(image)
                                                .addOnSuccessListener { barcodes ->
                                                    for (barcode in barcodes) {
                                                        barcode.rawValue?.let { value ->
                                                            onBarcodeDetected(value)
                                                        }
                                                    }
                                                }
                                                .addOnCompleteListener { imageProxy.close() }
                                        } else {
                                            imageProxy.close()
                                        }
                                    }
                                }

                            try {
                                cameraProvider.unbindAll()
                                cameraProvider.bindToLifecycle(
                                    lifecycleOwner,
                                    CameraSelector.DEFAULT_BACK_CAMERA,
                                    preview,
                                    imageAnalysis,
                                )
                            } catch (_: Exception) { }
                        },
                        ContextCompat.getMainExecutor(ctx),
                    )
                    previewView
                },
            )

            // Scanner frame overlay
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier
                        .size(200.dp)
                        .clip(RoundedCornerShape(28.dp))
                        .background(Color.White.copy(alpha = 0.05f)),
                ) {
                    val c = Color(0xFF18B270)
                    ScannerCorner(Modifier.align(Alignment.TopStart), c)
                    ScannerCorner(Modifier.align(Alignment.TopEnd), c, mirroredHorizontally = true)
                    ScannerCorner(Modifier.align(Alignment.BottomStart), c, mirroredVertically = true)
                    ScannerCorner(Modifier.align(Alignment.BottomEnd), c, mirroredHorizontally = true, mirroredVertically = true)
                }
            }
        } else {
            Box(
                modifier = Modifier
                    .size(200.dp)
                    .clip(RoundedCornerShape(28.dp))
                    .background(Color.White.copy(alpha = 0.05f)),
            ) {
                val c = Color(0xFFE0C36A)
                ScannerCorner(Modifier.align(Alignment.TopStart), c)
                ScannerCorner(Modifier.align(Alignment.TopEnd), c, mirroredHorizontally = true)
                ScannerCorner(Modifier.align(Alignment.BottomStart), c, mirroredVertically = true)
                ScannerCorner(Modifier.align(Alignment.BottomEnd), c, mirroredHorizontally = true, mirroredVertically = true)
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
                    text = stringResource(R.string.qr_scan_camera_waiting),
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White.copy(alpha = 0.96f),
                )
            }
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
    Box(modifier = modifier.padding(16.dp)) {
        Box(
            modifier = Modifier
                .align(if (mirroredVertically) Alignment.BottomStart else Alignment.TopStart)
                .size(width = 36.dp, height = 5.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(color),
        )
        Box(
            modifier = Modifier
                .align(if (mirroredHorizontally) Alignment.CenterEnd else Alignment.CenterStart)
                .size(width = 5.dp, height = 36.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(color),
        )
    }
}

@Composable
private fun modeInstruction(mode: QrScanMode): String = when (mode) {
    QrScanMode.IssueDevice -> stringResource(R.string.qr_scan_instruction_issue)
    QrScanMode.ReturnDevice -> stringResource(R.string.qr_scan_instruction_return)
    QrScanMode.UploadDevice -> stringResource(R.string.qr_scan_instruction_upload)
    QrScanMode.RegisterWatch -> stringResource(R.string.qr_scan_instruction_register_watch)
}
