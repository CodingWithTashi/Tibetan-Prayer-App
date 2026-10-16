package com.codingwithtashi.dailyprayer.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.window.core.layout.WindowSizeClass

/** Caps the width at [max] and fills up to it; use inside a horizontally centred column. */
fun Modifier.contentWidth(max: Dp = 640.dp): Modifier = this.widthIn(max = max).fillMaxWidth()

/** True when the window width is below the 600dp medium breakpoint (phone portrait). */
@Composable
fun rememberIsCompactWidth(): Boolean =
    !currentWindowAdaptiveInfo().windowSizeClass.isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_MEDIUM_LOWER_BOUND)

/** Draws a 1dp line at the bottom edge (list dividers). */
internal fun Modifier.bottomDivider(color: Color, show: Boolean = true): Modifier =
    if (!show) this else this.drawBehind {
        val h = 1.dp.toPx()
        drawRect(color, Offset(0f, size.height - h), Size(size.width, h))
    }

/** Draws a 1dp line at the top edge (bottom bar). */
internal fun Modifier.topBorder(color: Color): Modifier = this.drawBehind {
    drawRect(color, Offset.Zero, Size(size.width, 1.dp.toPx()))
}

/** Draws a 1dp line at the end edge (navigation rail, LTR/RTL aware). */
internal fun Modifier.endBorder(color: Color): Modifier = this.drawBehind {
    val h = 1.dp.toPx()
    val x = if (layoutDirection == androidx.compose.ui.unit.LayoutDirection.Ltr) size.width - h else 0f
    drawRect(color, Offset(x, 0f), Size(h, size.height))
}
