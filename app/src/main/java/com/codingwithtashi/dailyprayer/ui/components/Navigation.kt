package com.codingwithtashi.dailyprayer.ui.components

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.codingwithtashi.dailyprayer.ui.theme.PrayerTheme
import com.codingwithtashi.dailyprayer.ui.theme.motionDuration

/** One main-tab destination: label string and icon drawable. */
data class TabSpec(@StringRes val label: Int, @DrawableRes val icon: Int)

@Composable
private fun TabItem(spec: TabSpec, selected: Boolean, onClick: () -> Unit, modifier: Modifier) {
    val c = PrayerTheme.colors
    val language = PrayerTheme.language
    val tint by animateColorAsState(if (selected) c.accent else c.textMuted, tween(motionDuration(200)), label = "tabTint")
    val pill by animateColorAsState(if (selected) c.surfaceWarm else Color.Transparent, tween(motionDuration(200)), label = "tabPill")
    Column(
        modifier
            .selectable(selected = selected, role = Role.Tab, onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Box(
            Modifier.size(56.dp, 30.dp).clip(RoundedCornerShape(15.dp)).background(pill),
            contentAlignment = Alignment.Center,
        ) {
            Icon(painterResource(spec.icon), contentDescription = null, tint = tint, modifier = Modifier.size(22.dp))
        }
        Text(
            stringResource(spec.label),
            style = PrayerTheme.type.tab(language, selected),
            color = tint,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
        )
    }
}

/** Compact-width bottom tab bar: 80dp plus the navigation-bar inset, surface fill, 1dp top border. */
@Composable
fun PrayerBottomBar(
    tabs: List<TabSpec>,
    selected: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = PrayerTheme.colors
    Box(modifier.fillMaxWidth().background(c.surface).topBorder(c.border).navigationBarsPadding()) {
        Row(
            Modifier.fillMaxWidth().height(80.dp).padding(start = 8.dp, top = 8.dp, end = 8.dp).selectableGroup(),
        ) {
            tabs.forEachIndexed { i, tab ->
                TabItem(tab, i == selected, { onSelect(i) }, Modifier.weight(1f).fillMaxHeight())
            }
        }
    }
}

/** Medium/expanded-width navigation rail: 80dp wide, surface fill, 1dp end border. */
@Composable
fun PrayerNavRail(
    tabs: List<TabSpec>,
    selected: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = PrayerTheme.colors
    Column(
        modifier
            .width(80.dp)
            .fillMaxHeight()
            .background(c.surface)
            .endBorder(c.border)
            .windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Start + WindowInsetsSides.Vertical))
            .padding(top = 12.dp)
            .selectableGroup(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        tabs.forEachIndexed { i, tab ->
            TabItem(tab, i == selected, { onSelect(i) }, Modifier.fillMaxWidth().heightIn(min = 56.dp))
        }
    }
}
