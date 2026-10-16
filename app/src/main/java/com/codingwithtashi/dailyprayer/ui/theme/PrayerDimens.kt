package com.codingwithtashi.dailyprayer.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Component heights and sizes. Anything that wraps text is a *minimum* so larger text sizes and
 * font scales can grow it.
 */
object PrayerDimens {
    /** Minimum interactive size (accessibility). */
    val minTouchTarget: Dp = 48.dp

    // Bars
    val bottomBarMin: Dp = 80.dp
    val tabPillWidth: Dp = 60.dp
    val tabPillHeight: Dp = 32.dp
    val topBarMin: Dp = 56.dp
    val miniPlayerMin: Dp = 68.dp
    val miniPlayerTile: Dp = 44.dp
    val counterBarMin: Dp = 72.dp

    // Chips and pills
    val chipMin: Dp = 40.dp
    val dayChip: Dp = 44.dp
    val goalPillMin: Dp = 44.dp
    val smallPillMinWidth: Dp = 56.dp
    val smallPillMinHeight: Dp = 40.dp
    val langPillMin: Dp = 40.dp

    /** Added to every [com.codingwithtashi.dailyprayer.ui.components.PillButton] nominal height. */
    val pillButtonBoost: Dp = 4.dp

    // Inputs
    val inputCompact: Dp = 44.dp
    val inputDefault: Dp = 48.dp
    val inputLarge: Dp = 56.dp
    val inputXLarge: Dp = 60.dp
    val searchFieldMin: Dp = 52.dp

    // Rows and controls
    val segmentedControlMin: Dp = 48.dp
    val confirmBarMin: Dp = 48.dp
    val dashedBoxMin: Dp = 50.dp
    val remindRowMin: Dp = 56.dp
    val checkCircleSmallMin: Dp = 28.dp
    val checkCircleLargeMin: Dp = 30.dp
    val queueNumberMin: Dp = 28.dp
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
