package com.codingwithtashi.dailyprayer.ui.components

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import com.codingwithtashi.dailyprayer.core.locale.AppLanguage
import com.codingwithtashi.dailyprayer.ui.theme.Jomolhari
import com.codingwithtashi.dailyprayer.ui.theme.PrayerTheme

@Composable
private fun UppercaseLabel(text: String, style: TextStyle, tibetanStyle: TextStyle, color: Color, modifier: Modifier) {
    val bo = PrayerTheme.language == AppLanguage.BO
    Text(
        text = if (bo) text else text.uppercase(),
        style = if (bo) tibetanStyle else style,
        color = color,
        modifier = modifier,
    )
}

/**
 * Section heading label: uppercase and letter-spaced, except in Tibetan where it renders as
 * Jomolhari 14sp (no casing/tracking) because the Latin label sizes are illegible in Tibetan script.
 */
@Composable
fun SectionLabel(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = PrayerTheme.type.label12,
    color: Color = PrayerTheme.colors.textMuted,
) = UppercaseLabel(text, style, PrayerTheme.type.tibetanLabel14, color, modifier.semantics { heading() })

/** Small overline label (10sp by default; Jomolhari 12sp in Tibetan); same casing rules as [SectionLabel]. */
@Composable
fun Overline(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = PrayerTheme.type.overline10,
    color: Color = PrayerTheme.colors.textSubtle,
) = UppercaseLabel(text, style, PrayerTheme.type.tibetanOverline12, color, modifier)

/** Large screen title (Cormorant, or Jomolhari 30sp in Tibetan), exposed as a heading. */
@Composable
fun ScreenTitle(text: String, modifier: Modifier = Modifier, color: Color = PrayerTheme.colors.ink) {
    Text(
        text = text,
        style = PrayerTheme.type.screenTitle(PrayerTheme.language),
        color = color,
        modifier = modifier.semantics { heading() },
    )
}

/** Text that always renders in Jomolhari (Tibetan) regardless of the style's family. */
@Composable
fun TibetanText(
    text: String,
    style: TextStyle,
    modifier: Modifier = Modifier,
    color: Color = PrayerTheme.colors.ink,
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Clip,
    textAlign: TextAlign? = null,
) {
    Text(
        text = text,
        style = style.copy(fontFamily = Jomolhari),
        color = color,
        modifier = modifier,
        maxLines = maxLines,
        overflow = overflow,
        textAlign = textAlign,
    )
}

/** Plain text for pre-formatted count strings (digits already localised by the caller). */
@Composable
fun NumberText(
    text: String,
    style: TextStyle,
    modifier: Modifier = Modifier,
    color: Color = PrayerTheme.colors.ink,
    maxLines: Int = 1,
) {
    Text(text = text, style = style, color = color, modifier = modifier, maxLines = maxLines)
}

