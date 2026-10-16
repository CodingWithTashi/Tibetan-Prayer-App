package com.codingwithtashi.dailyprayer.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.codingwithtashi.dailyprayer.R
import com.codingwithtashi.dailyprayer.ui.theme.PrayerTheme
import androidx.compose.foundation.layout.size

/**
 * Reader counter pill (h66, r33): minus, count/goal with a thin progress bar, reset and a large
 * plus button. [countText]/[goalText] are already localised; the count is a polite live region.
 */
@Composable
fun CounterBar(
    countText: String,
    goalText: String,
    progress: Float,
    onDec: () -> Unit,
    onInc: () -> Unit,
    onReset: () -> Unit,
    resetEnabled: Boolean,
    modifier: Modifier = Modifier,
) {
    val c = PrayerTheme.colors
    val t = PrayerTheme.type
    val shape = RoundedCornerShape(33.dp)
    val haptic = LocalHapticFeedback.current
    Row(
        modifier
            .fillMaxWidth()
            .shadow(12.dp, shape, ambientColor = c.ink, spotColor = c.ink)
            .background(c.darkCard, shape)
            .border(1.dp, c.darkCardBorder, shape)
            .height(66.dp)
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        CircleButton(
            size = 46.dp,
            contentDescription = stringResource(R.string.cd_decrement),
            onClick = onDec,
            border = BorderStroke(1.dp, c.darkCardOutline),
        ) { Icon(painterResource(R.drawable.ic_minus), null, Modifier.size(18.dp), tint = c.darkCardText) }
        Column(
            Modifier.weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            Row(
                Modifier.semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite },
                verticalAlignment = Alignment.Bottom,
            ) {
                Text(countText, style = t.counter18, color = c.darkCardText, maxLines = 1)
                Text(" / $goalText", style = t.body13, color = c.darkCardSubtle, maxLines = 1)
            }
            ProgressBarThin(
                progress = progress,
                modifier = Modifier.width(110.dp),
                height = 3.dp,
                trackColor = c.darkCardTrack,
                fillColor = c.gold,
                radius = 2.dp,
                announceProgress = false,
            )
        }
        IconActionButton(
            icon = R.drawable.ic_reset,
            contentDescription = stringResource(R.string.cd_reset_count),
            onClick = onReset,
            modifier = Modifier.alpha(if (resetEnabled) 1f else 0.4f),
            tint = c.darkCardMuted,
            iconSize = 19.dp,
            enabled = resetEnabled,
        )
        CircleButton(
            size = 54.dp,
            contentDescription = stringResource(R.string.cd_increment),
            onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.ContextClick)
                onInc()
            },
            containerColor = c.accentSolid,
        ) { Icon(painterResource(R.drawable.ic_plus), null, Modifier.size(22.dp), tint = c.onAccentSolid) }
    }
}
