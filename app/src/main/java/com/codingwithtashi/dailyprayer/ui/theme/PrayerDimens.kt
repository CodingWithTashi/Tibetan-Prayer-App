package com.codingwithtashi.dailyprayer.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Radii, spacing, heights and tile sizes (plan Part H4); 1 CSS px of the prototype = 1dp. */
@Suppress("unused")
object PrayerDimens {
    // Radii
    val r7: Dp = 7.dp
    val r9: Dp = 9.dp
    val r10: Dp = 10.dp
    val r11: Dp = 11.dp
    val r12: Dp = 12.dp
    val r14: Dp = 14.dp
    val r16: Dp = 16.dp
    val r18: Dp = 18.dp
    val r20: Dp = 20.dp
    val r22: Dp = 22.dp
    val r24: Dp = 24.dp
    val r26: Dp = 26.dp
    val r27: Dp = 27.dp
    val r33: Dp = 33.dp
    val rPill: Dp = 100.dp

    // Spacing
    val s4: Dp = 4.dp
    val s6: Dp = 6.dp
    val s8: Dp = 8.dp
    val s10: Dp = 10.dp
    val s12: Dp = 12.dp
    val s14: Dp = 14.dp
    val s16: Dp = 16.dp
    val s18: Dp = 18.dp
    val s20: Dp = 20.dp
    val s22: Dp = 22.dp
    val s24: Dp = 24.dp
    val s28: Dp = 28.dp

    val screenHorizontal: Dp = 20.dp
    val onboardingHorizontal: Dp = 28.dp
    val readerHorizontal: Dp = 28.dp

    // Heights
    val button54: Dp = 54.dp
    val button52: Dp = 52.dp
    val button50: Dp = 50.dp
    val button48: Dp = 48.dp
    val button44: Dp = 44.dp
    val button40: Dp = 40.dp
    val button36: Dp = 36.dp
    val button34: Dp = 34.dp
    val topBar: Dp = 52.dp
    val bottomBar: Dp = 80.dp
    val navRailWidth: Dp = 80.dp
    val miniPlayer: Dp = 62.dp
    val counterBar: Dp = 66.dp
    val minTouchTarget: Dp = 48.dp

    // Tiles
    val tile40: Dp = 40.dp
    val tile42: Dp = 42.dp
    val tile44: Dp = 44.dp
    val tile48: Dp = 48.dp
    val tile84: Dp = 84.dp

    // Controls
    val squareCheck: Dp = 24.dp
    val checkCircle24: Dp = 24.dp
    val checkCircle26: Dp = 26.dp
    val tabPillWidth: Dp = 56.dp
    val tabPillHeight: Dp = 30.dp
    val toggleWidth: Dp = 48.dp
    val toggleHeight: Dp = 28.dp
    val toggleThumb: Dp = 22.dp
    val weekCircle: Dp = 26.dp
    val dayChip: Dp = 38.dp
    val avatarSmall: Dp = 40.dp
    val avatarLarge: Dp = 56.dp
    val iconButton: Dp = 48.dp
    val iconDefault: Dp = 22.dp

    // Content widths
    val contentMaxWidth: Dp = 640.dp
    val readerMaxWidth: Dp = 720.dp
    val onboardingMaxWidth: Dp = 480.dp
}

/** Shapes bridge (7/10/14/18/26) plus the bottom-sheet shape. */
object PrayerShapes {
    val material = Shapes(
        extraSmall = RoundedCornerShape(7.dp),
        small = RoundedCornerShape(10.dp),
        medium = RoundedCornerShape(14.dp),
        large = RoundedCornerShape(18.dp),
        extraLarge = RoundedCornerShape(26.dp),
    )
    val sheet = RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp)
}
