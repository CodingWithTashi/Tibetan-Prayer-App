package com.codingwithtashi.dailyprayer.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.codingwithtashi.dailyprayer.ui.theme.PrayerTheme

@Composable
private fun ChipPill(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    dot: Color?,
    modifier: Modifier,
) {
    val c = PrayerTheme.colors
    val shape = RoundedCornerShape(18.dp)
    Box(modifier.minimumInteractiveComponentSize()) {
        Row(
            Modifier
                .height(36.dp)
                .clip(shape)
                .background(if (selected) c.accent else c.surface)
                .border(1.dp, if (selected) c.accent else c.border, shape)
                .selectable(selected = selected, role = Role.Tab, onClick = onClick)
                .padding(start = 12.dp, end = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (dot != null) Box(Modifier.size(8.dp).background(dot, CircleShape))
            Text(label, style = PrayerTheme.type.body13Semi, color = if (selected) c.onAccent else c.ink, maxLines = 1)
        }
    }
}

/** Library category filter chip with a coloured dot (pass null [dot] for "All"). */
@Composable
fun CategoryChip(label: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier, dot: Color? = null) =
    ChipPill(label, selected, onClick, dot, modifier)

/** Filter chip without a dot (Listen: All / Downloaded). */
@Composable
fun FilterChipPill(label: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) =
    ChipPill(label, selected, onClick, null, modifier)

/** 38dp circular week-day toggle (44x48 touch area so seven fit on a 360dp phone). */
@Composable
fun DayChip(
    label: String,
    selected: Boolean,
    onToggle: () -> Unit,
    contentDescription: String,
    modifier: Modifier = Modifier,
) {
    val c = PrayerTheme.colors
    Box(
        modifier
            .width(44.dp)
            .heightIn(min = 48.dp)
            .toggleable(value = selected, role = Role.Checkbox, onValueChange = { onToggle() })
            .semantics { this.contentDescription = contentDescription },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(if (selected) c.accent else Color.Transparent)
                .then(if (selected) Modifier else Modifier.border(1.dp, c.borderMuted, CircleShape)),
            contentAlignment = Alignment.Center,
        ) {
            Text(label, style = PrayerTheme.type.day(PrayerTheme.language), color = if (selected) c.onAccent else c.ink, maxLines = 1)
        }
    }
}

/** Recitation goal pill (h40, r20); give it a weight/width via [modifier]. */
@Composable
fun GoalPill(label: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val c = PrayerTheme.colors
    val shape = RoundedCornerShape(20.dp)
    Box(modifier.minimumInteractiveComponentSize(), contentAlignment = Alignment.Center) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(40.dp)
                .clip(shape)
                .background(if (selected) c.accent else Color.Transparent)
                .border(1.dp, if (selected) c.accent else c.border, shape)
                .selectable(selected = selected, role = Role.RadioButton, onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            Text(label, style = PrayerTheme.type.body14Semi, color = if (selected) c.onAccent else c.ink, maxLines = 1)
        }
    }
}

/** 52x34 pill used by the player (speed, repeat); content is laid out in a centred row. */
@Composable
fun SmallPill(
    onClick: () -> Unit,
    contentDescription: String,
    modifier: Modifier = Modifier,
    borderColor: Color = Color.Unspecified,
    containerColor: Color = Color.Transparent,
    content: @Composable RowScope.() -> Unit,
) {
    val shape = RoundedCornerShape(17.dp)
    Box(modifier.minimumInteractiveComponentSize(), contentAlignment = Alignment.Center) {
        Row(
            Modifier
                .width(52.dp)
                .height(34.dp)
                .clip(shape)
                .background(containerColor)
                .border(1.dp, if (borderColor == Color.Unspecified) PrayerTheme.colors.border else borderColor, shape)
                .semantics { this.contentDescription = contentDescription }
                .clickable(role = Role.Button, onClick = onClick),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            content = content,
        )
    }
}

/** Reminder time chip (surfaceWarm, r10, h36); opens the time picker. */
@Composable
fun TimeChip(timeLabel: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val c = PrayerTheme.colors
    Box(modifier.minimumInteractiveComponentSize(), contentAlignment = Alignment.Center) {
        Box(
            Modifier
                .height(36.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(c.surfaceWarm)
                .clickable(role = Role.Button, onClick = onClick)
                .padding(horizontal = 10.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(timeLabel, style = PrayerTheme.type.body15Semi, color = c.accent, maxLines = 1, textAlign = TextAlign.Center)
        }
    }
}

