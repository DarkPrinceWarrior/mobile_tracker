package com.example.mobile_tracker.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

object AppSpacing {
    val xxs = 4.dp
    val xs = 8.dp
    val sm = 12.dp
    val md = 16.dp
    val lg = 24.dp
    val xl = 32.dp
}

object AppRadius {
    val sm = 8.dp
    val md = 12.dp
    val lg = 16.dp
    val xl = 24.dp
    val pill = 48.dp
}

object AppLayout {
    val screenPadding = 16.dp
    val cardPadding = 12.dp
    val sectionGap = 16.dp
    val blockGap = 8.dp
    val topStatusBarHeight = 34.dp
}

val ColorScheme.success: Color
    get() = Success

val ColorScheme.successSoft: Color
    get() = SuccessLight

val ColorScheme.warning: Color
    get() = Warning

val ColorScheme.warningSoft: Color
    get() = WarningLight

val ColorScheme.danger: Color
    get() = Danger

val ColorScheme.dangerSoft: Color
    get() = DangerLight

val ColorScheme.info: Color
    get() = Info

val ColorScheme.infoSoft: Color
    get() = InfoLight
