package com.codingwithtashi.dailyprayer.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.progressSemantics
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.codingwithtashi.dailyprayer.R
import com.codingwithtashi.dailyprayer.ui.theme.PrayerDimens
import com.codingwithtashi.dailyprayer.ui.theme.PrayerTheme
import com.codingwithtashi.dailyprayer.ui.theme.motionDuration

/**
 * Screen top bar: at least 56dp plus the status-bar inset. [onBack] null leaves a 48dp spacer where the
 * back button would be; [center] fills the middle; [actions] sit at the end.
 */
@Composable
fun TopBar(
    onBack: (() -> Unit)?,
    modifier: Modifier = Modifier,
    tint: Color = PrayerTheme.colors.ink,
    backContentDescription: String = stringResource(R.string.cd_back),
    center: @Composable () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .heightIn(min = PrayerDimens.topBarMin)
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (onBack != null) {
            IconActionButton(R.drawable.ic_back, backContentDescription, onBack, tint = tint)
        } else {
            Spacer(Modifier.size(48.dp))
        }
        Box(Modifier.weight(1f), contentAlignment = Alignment.Center) { center() }
        actions()
    }
}

/** Thin rounded progress bar; [animate] eases value changes. Width comes from [modifier]. */
@Composable
fun ProgressBarThin(
    progress: Float,
    modifier: Modifier = Modifier,
    height: Dp = 4.dp,
    trackColor: Color = PrayerTheme.colors.border,
    fillColor: Color = PrayerTheme.colors.accent,
    radius: Dp = height / 2,
    animate: Boolean = true,
    announceProgress: Boolean = true,
) {
    val target = progress.coerceIn(0f, 1f)
    val shown = if (animate) {
        animateFloatAsState(target, tween(motionDuration(300)), label = "progress").value
    } else target
    Box(
        modifier
            .fillMaxWidth()
            .height(height)
            .then(if (announceProgress) Modifier.progressSemantics(target) else Modifier.clearAndSetSemantics { })
            .clip(RoundedCornerShape(radius))
            .background(trackColor)
            .drawBehind { drawRect(fillColor, Offset.Zero, Size(size.width * shown, size.height)) },
    )
}

/** Circular determinate ring starting at 12 o'clock (download progress). */
@Composable
fun ConicProgressRing(
    progress: Float,
    modifier: Modifier = Modifier,
    size: Dp = 28.dp,
    stroke: Dp = 3.dp,
    trackColor: Color = PrayerTheme.colors.border,
    color: Color = PrayerTheme.colors.accent,
) {
    Canvas(modifier.size(size).clearAndSetSemantics { }) {
        val w = stroke.toPx()
        val inset = w / 2
        val arcSize = Size(this.size.width - w, this.size.height - w)
        drawArc(trackColor, 0f, 360f, false, Offset(inset, inset), arcSize, style = Stroke(w))
        drawArc(color, -90f, 360f * progress.coerceIn(0f, 1f), false, Offset(inset, inset), arcSize, style = Stroke(w, cap = StrokeCap.Round))
    }
}

/** 40x4 bottom-sheet drag handle. */
@Composable
fun SheetHandle(modifier: Modifier = Modifier, color: Color = PrayerTheme.colors.handle) {
    Box(modifier.padding(vertical = 10.dp).width(40.dp).height(4.dp).clip(RoundedCornerShape(2.dp)).background(color))
}

