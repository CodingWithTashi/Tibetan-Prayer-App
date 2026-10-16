package com.codingwithtashi.dailyprayer.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.codingwithtashi.dailyprayer.R
import com.codingwithtashi.dailyprayer.ui.theme.PrayerTheme

/** Trailing content of a [SettingsRow]. */
sealed interface SettingsTrailing {
    data class Value(val text: String) : SettingsTrailing
    data class Switch(val checked: Boolean, val onCheckedChange: (Boolean) -> Unit) : SettingsTrailing
    data object Chevron : SettingsTrailing
    data object None : SettingsTrailing
}

/**
 * Settings list row (min 58): 20dp accent icon, label with optional [sub], and a [trailing] value,
 * switch or chevron. Switch rows toggle as a whole (Role.Switch); others are buttons.
 */
@Composable
fun SettingsRow(
    @DrawableRes icon: Int,
    label: String,
    modifier: Modifier = Modifier,
    sub: String? = null,
    trailing: SettingsTrailing = SettingsTrailing.None,
    onClick: (() -> Unit)? = null,
    minHeight: androidx.compose.ui.unit.Dp = 58.dp,
    tint: Color = PrayerTheme.colors.accent,
    labelColor: Color = PrayerTheme.colors.ink,
    showDivider: Boolean = true,
) {
    val c = PrayerTheme.colors
    val t = PrayerTheme.type
    val base = modifier.fillMaxWidth().bottomDivider(c.divider, showDivider)
    val interactive = when {
        trailing is SettingsTrailing.Switch ->
            base.toggleable(value = trailing.checked, role = Role.Switch, onValueChange = trailing.onCheckedChange)
        onClick != null -> base.clickable(role = Role.Button, onClick = onClick)
        else -> base
    }
    Row(
        interactive.heightIn(min = minHeight).padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Icon(painterResource(icon), null, Modifier.size(20.dp), tint = tint)
        Column(Modifier.weight(1f)) {
            Text(label, style = t.body15Semi, color = labelColor)
            if (sub != null) Text(sub, style = t.caption12, color = c.textMuted)
        }
        when (trailing) {
            is SettingsTrailing.Value -> Text(trailing.text, style = t.body14Medium, color = c.textMuted)
            is SettingsTrailing.Switch -> ToggleSwitch(trailing.checked, onCheckedChange = null)
            SettingsTrailing.Chevron -> Icon(painterResource(R.drawable.ic_chevron_right), null, Modifier.size(16.dp), tint = c.chevron)
            SettingsTrailing.None -> Unit
        }
    }
}
