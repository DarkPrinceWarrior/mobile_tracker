package com.example.mobile_tracker.presentation.common

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

private val TABLET_BREAKPOINT = 600.dp

@Composable
fun AdaptiveListDetail(
    isTablet: Boolean,
    listPane: @Composable (Modifier) -> Unit,
    detailPane: (@Composable (Modifier) -> Unit)? = null,
) {
    if (isTablet && detailPane != null) {
        Row(modifier = Modifier.fillMaxSize()) {
            listPane(
                Modifier
                    .weight(0.4f)
                    .fillMaxHeight(),
            )
            VerticalDivider()
            detailPane(
                Modifier
                    .weight(0.6f)
                    .fillMaxHeight(),
            )
        }
    } else {
        listPane(Modifier.fillMaxSize())
    }
}

@Composable
fun rememberIsTablet(): Boolean {
    var isTablet = false
    BoxWithConstraints {
        isTablet = maxWidth >= TABLET_BREAKPOINT
    }
    return isTablet
}
