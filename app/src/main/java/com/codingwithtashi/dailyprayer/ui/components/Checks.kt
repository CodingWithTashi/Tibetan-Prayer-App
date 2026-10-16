package com.codingwithtashi.dailyprayer.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.codingwithtashi.dailyprayer.R
import com.codingwithtashi.dailyprayer.ui.theme.PrayerTheme
import com.codingwithtashi.dailyprayer.ui.theme.motionDuration

/**
 * Round done indicator: filled accent with a check when [checked]; otherwise a 2dp ring that
 * shows [number] (routine step) when given. Decorative; the parent row carries the semantics.
 */
@Composable
fun CheckCircle(size: Dp, checked: Boolean, modifier: Modifier = Modifier, number: String? = null) {
    val c = PrayerTheme.colors
    val bg by animateColorAsState(if (checked) c.accent else androidx.compose.ui.graphics.Color.Transparent, tween(motionDuration(200)), label = "checkBg")
    val ring by animateColorAsState(if (checked) c.accent else c.borderStrong, tween(motionDuration(200)), label = "checkRing")
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(bg)
            .border(2.dp, ring, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        if (checked) {
            Icon(painterResource(R.drawable.ic_check), null, Modifier.size(if (size > 24.dp) 12.dp else 11.dp), tint = c.onAccent)
        } else if (number != null) {
            Text(number, style = PrayerTheme.type.caption12Semi, color = c.accent, maxLines = 1)
        }
    }
}

/** 24dp rounded-square checkbox visual (the row is the toggleable). */
@Composable
fun SquareCheck(checked: Boolean, modifier: Modifier = Modifier) {
    val c = PrayerTheme.colors
    val shape = RoundedCornerShape(7.dp)
    val bg by animateColorAsState(if (checked) c.accent else androidx.compose.ui.graphics.Color.Transparent, tween(motionDuration(150)), label = "sqBg")
    val edge by animateColorAsState(if (checked) c.accent else c.borderStrong, tween(motionDuration(150)), label = "sqEdge")
    Box(
        modifier = modifier.size(24.dp).clip(shape).background(bg).border(2.dp, edge, shape),
        contentAlignment = Alignment.Center,
    ) {
        if (checked) Icon(painterResource(R.drawable.ic_check), null, Modifier.size(14.dp), tint = c.onAccent)
    }
}

/**
 * Custom 48x28 switch. With [onCheckedChange] it is itself toggleable (Role.Switch, 48dp target);
 * pass null when the surrounding row owns the toggle.
 */
@Composable
fun ToggleSwitch(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val c = PrayerTheme.colors
    val track by animateColorAsState(if (checked) c.accent else c.borderMuted, tween(motionDuration(200)), label = "swTrack")
    val x by animateDpAsState(if (checked) 23.dp else 3.dp, tween(motionDuration(200)), label = "swThumb")
    val base = if (onCheckedChange != null) {
        modifier.minimumInteractiveComponentSize().toggleable(
            value = checked, enabled = enabled, role = Role.Switch, onValueChange = onCheckedChange,
        )
    } else modifier
    Box(base.width(48.dp).height(28.dp).clip(RoundedCornerShape(14.dp)).background(track)) {
        Box(
            Modifier
                .offset { IntOffset(x.roundToPx(), 3.dp.roundToPx()) }
                .size(22.dp)
                .shadow(1.dp, CircleShape)
                .background(c.surface, CircleShape),
        )
    }
}
