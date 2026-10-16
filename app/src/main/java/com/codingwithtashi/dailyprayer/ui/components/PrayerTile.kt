package com.codingwithtashi.dailyprayer.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.codingwithtashi.dailyprayer.core.text.hasTibetan
import com.codingwithtashi.dailyprayer.ui.theme.CategoryKey
import com.codingwithtashi.dailyprayer.ui.theme.PrayerTheme
import com.codingwithtashi.dailyprayer.ui.theme.categoryColors
import com.codingwithtashi.dailyprayer.ui.theme.category

/**
 * Rounded tile showing a Tibetan syllable on the category colour (decorative; hidden from
 * accessibility). A null [size] leaves sizing to [modifier] (e.g. `fillMaxWidth().aspectRatio(1f)`). [darkVariant] forces the dark category palette; the colour overrides win over both.
 */
@Composable
fun PrayerTile(
    syllable: String,
    category: CategoryKey,
    size: Dp?,
    radius: Dp,
    style: TextStyle,
    modifier: Modifier = Modifier,
    darkVariant: Boolean = false,
    containerColor: Color = Color.Unspecified,
    contentColor: Color = Color.Unspecified,
) {
    val glyphStyle = style.forContent(syllable)
    val palette = if (darkVariant) categoryColors(category, dark = true) else PrayerTheme.colors.category(category)
    Box(
        modifier = modifier
            .then(if (size != null) Modifier.size(size) else Modifier)
            .clip(RoundedCornerShape(radius))
            .background(if (containerColor.isSpecified()) containerColor else palette.tileBg)
            .clearAndSetSemantics { },
        contentAlignment = Alignment.Center,
    ) {
        BasicText(
            text = syllable,
            style = glyphStyle.copy(
                color = if (contentColor.isSpecified()) contentColor else palette.tileInk,
                textAlign = TextAlign.Center,
            ),
            maxLines = 1,
            softWrap = false,
            autoSize = TextAutoSize.StepBased(minFontSize = PrayerTheme.type.tileGlyphMinSize, maxFontSize = glyphStyle.fontSize),
        )
    }
}

private fun Color.isSpecified() = this != Color.Unspecified

/** First character uppercased, or "D" when the name is blank (Avatar initial). */
fun avatarInitial(name: String?): String =
    name?.trim()?.firstOrNull()?.uppercase() ?: "D"

/** Circular avatar with the user's [initial] (accentSolid background); decorative. */
@Composable
fun Avatar(
    size: Dp,
    initial: String,
    modifier: Modifier = Modifier,
    textStyle: TextStyle = if (size >= 56.dp) PrayerTheme.type.avatarLarge else PrayerTheme.type.avatarSmall,
) {
    val c = PrayerTheme.colors
    val initialStyle = textStyle.forContent(initial)
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(c.accentSolid)
            .clearAndSetSemantics { },
        contentAlignment = Alignment.Center,
    ) {
        BasicText(
            initial,
            style = initialStyle.copy(color = c.onAccentSolid),
            maxLines = 1,
            softWrap = false,
            autoSize = TextAutoSize.StepBased(minFontSize = PrayerTheme.type.tileGlyphMinSize, maxFontSize = initialStyle.fontSize),
        )
    }
}
