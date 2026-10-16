package com.codingwithtashi.dailyprayer.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.codingwithtashi.dailyprayer.ui.theme.PrayerColors
import com.codingwithtashi.dailyprayer.ui.theme.PrayerTheme

/** Segmented control (track segmentBg r16, options h44 r12; selected gets a surface fill and 2dp shadow). */
@Composable
fun SegmentedControl(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    textStyleFor: (index: Int) -> TextStyle,
    modifier: Modifier = Modifier,
) {
    val c = PrayerTheme.colors
    Row(
        modifier
            .fillMaxWidth()
            .background(c.segmentBg, RoundedCornerShape(16.dp))
            .padding(4.dp)
            .selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        options.forEachIndexed { i, label ->
            val selected = i == selectedIndex
            val shape = RoundedCornerShape(12.dp)
            Box(
                Modifier
                    .weight(1f)
                    .height(48.dp)
                    .then(if (selected) Modifier.shadow(2.dp, shape) else Modifier)
                    .clip(shape)
                    .background(if (selected) c.surface else Color.Transparent)
                    .selectable(selected = selected, role = Role.RadioButton, onClick = { onSelect(i) }),
                contentAlignment = Alignment.Center,
            ) {
                Text(label, style = textStyleFor(i), color = if (selected) c.ink else c.textMuted, maxLines = 1)
            }
        }
    }
}

/** Box with a dashed 1.5dp borderStrong outline (add-prayers, empty saved). */
@Composable
fun DashedBox(
    modifier: Modifier = Modifier,
    radius: Dp = 16.dp,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    contentAlignment: Alignment = Alignment.Center,
    content: @Composable BoxScope.() -> Unit,
) {
    val color = PrayerTheme.colors.borderStrong
    Box(
        modifier
            .drawBehind {
                val w = 1.5.dp.toPx()
                drawRoundRect(
                    color = color,
                    topLeft = Offset(w / 2, w / 2),
                    size = Size(size.width - w, size.height - w),
                    cornerRadius = CornerRadius(radius.toPx()),
                    style = Stroke(w, pathEffect = PathEffect.dashPathEffect(floatArrayOf(8.dp.toPx(), 6.dp.toPx()))),
                )
            }
            .padding(contentPadding),
        contentAlignment = contentAlignment,
        content = content,
    )
}

/** Centred empty state: 64dp icon circle, message and an optional filled button (label to onClick). */
@Composable
fun EmptyState(
    @DrawableRes icon: Int,
    text: String,
    modifier: Modifier = Modifier,
    button: Pair<String, () -> Unit>? = null,
) {
    val c = PrayerTheme.colors
    Column(
        modifier.fillMaxWidth().padding(vertical = 40.dp, horizontal = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Box(Modifier.size(64.dp).background(c.surfaceWarm, CircleShape), contentAlignment = Alignment.Center) {
            Icon(painterResource(icon), contentDescription = null, tint = c.accent, modifier = Modifier.size(28.dp))
        }
        Text(text, style = PrayerTheme.type.body14, color = c.textMuted, textAlign = TextAlign.Center)
        if (button != null) {
            PillButton(button.first, button.second, height = 44.dp, horizontalPadding = 20.dp)
        }
    }
}

/** Language card (h56, r16): 2dp accent border when selected, 1dp border otherwise. */
@Composable
fun LanguageOption(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    textStyle: TextStyle,
    modifier: Modifier = Modifier,
) {
    val c = PrayerTheme.colors
    val shape = RoundedCornerShape(16.dp)
    Box(
        modifier
            .heightIn(min = 56.dp)
            .clip(shape)
            .background(if (selected) c.surface else Color.Transparent)
            .border(if (selected) 2.dp else 1.dp, if (selected) c.accent else c.border, shape)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, style = textStyle, color = c.ink, maxLines = 1)
    }
}

/** Appearance choice. */
enum class AppearanceKind { System, Light, Dark }

/** Appearance card (h52, r14) drawn in the colours of the option it represents, regardless of the active theme. */
@Composable
fun AppearanceOptionCard(
    kind: AppearanceKind,
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = PrayerTheme.colors
    val (bg, fg, selectedBorder) = when (kind) {
        AppearanceKind.System -> Triple(c.surface, c.ink, c.accent)
        AppearanceKind.Light -> PrayerColors.Light.let { Triple(it.background, it.ink, it.accent) }
        AppearanceKind.Dark -> PrayerColors.Dark.let { Triple(it.background, it.ink, it.accent) }
    }
    val shape = RoundedCornerShape(14.dp)
    Box(
        modifier
            .heightIn(min = 52.dp)
            .clip(shape)
            .background(bg)
            .border(if (selected) 2.dp else 1.dp, if (selected) selectedBorder else c.border, shape)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, style = PrayerTheme.type.body14Semi, color = fg, maxLines = 1)
    }
}

/** "Our apps" card: tile with Tibetan [glyph], title and subtitle on a tinted background. */
@Composable
fun AppCard(
    glyph: String,
    title: String,
    sub: String,
    backgroundColor: Color,
    tileColor: Color,
    subColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = PrayerTheme.colors
    Column(
        modifier
            .clip(RoundedCornerShape(18.dp))
            .background(backgroundColor)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        PrayerTile(glyph, com.codingwithtashi.dailyprayer.ui.theme.CategoryKey.OTHER, 40.dp, 11.dp, PrayerTheme.type.tibetanTile16, containerColor = tileColor, contentColor = c.onAccentSolid)
        Column {
            Text(title, style = PrayerTheme.type.body14Semi, color = c.ink, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(sub, style = PrayerTheme.type.caption12, color = subColor, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
        Spacer(Modifier.size(0.dp))
    }
}
