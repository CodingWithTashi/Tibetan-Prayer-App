package com.codingwithtashi.dailyprayer.ui.components

import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.AnnotatedString.Range
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.codingwithtashi.dailyprayer.core.text.hasLatinLetterOrDigit
import com.codingwithtashi.dailyprayer.core.text.hasTibetan
import com.codingwithtashi.dailyprayer.core.text.tibetanRuns
import com.codingwithtashi.dailyprayer.ui.theme.CormorantGaramond
import com.codingwithtashi.dailyprayer.ui.theme.Jomolhari
import com.codingwithtashi.dailyprayer.ui.theme.PrayerTheme

/**
 * Size factor applied to Tibetan text styled with Instrument Sans (or no family). Tuned so Tibetan
 * labels are at least 16sp (13sp x 1.25), the smallest size where Jomolhari stays legible.
 */
internal const val TIBETAN_SCALE_SANS = 1.25f

/** Size factor for Tibetan text styled with Cormorant Garamond (matches the bo screen-title sizes). Tuned with [TIBETAN_SCALE_SANS]. */
internal const val TIBETAN_SCALE_SERIF = 1.0f

/** Minimum line height for Tibetan text, as a multiple of its font size. */
private const val TIBETAN_LINE_FACTOR = 1.5f

/** Result of [tibetanAware]: the text to draw and the style to draw it with. */
internal data class TibetanAwareText(val text: AnnotatedString, val style: TextStyle)

private fun TextStyle.tibetanScale(): Float = if (fontFamily === CormorantGaramond) TIBETAN_SCALE_SERIF else TIBETAN_SCALE_SANS

private fun TextUnit.timesOrSelf(k: Float): TextUnit = if (this == TextUnit.Unspecified) this else this * k

/** Raises [lineHeight] to at least 1.5x [size] when both are specified in the same unit; otherwise leaves it. */
private fun raiseLineHeight(lineHeight: TextUnit, size: TextUnit): TextUnit {
    if (lineHeight == TextUnit.Unspecified || size == TextUnit.Unspecified) return lineHeight
    if (lineHeight.type != size.type || lineHeight.type == TextUnitType.Unspecified) return lineHeight
    val need = size.value * TIBETAN_LINE_FACTOR
    return if (lineHeight.value >= need) lineHeight else TextUnit(need, size.type)
}

/**
 * Whole-text Jomolhari version of this style: Jomolhari family, normal weight (single-weight face, no
 * faux bold), size scaled by the family factor, no letter spacing and line height of at least 1.5x size.
 * A style that is already Jomolhari is returned unchanged.
 */
internal fun TextStyle.forTibetan(): TextStyle {
    if (fontFamily === Jomolhari) return this
    val newSize = fontSize.timesOrSelf(tibetanScale())
    return copy(
        fontFamily = Jomolhari,
        fontWeight = FontWeight.Normal,
        fontSize = newSize,
        letterSpacing = 0.sp,
        lineHeight = raiseLineHeight(lineHeight, newSize),
    )
}

/**
 * Whole-style Jomolhari when [text] contains Tibetan, otherwise this style unchanged. For places that
 * cannot take per-run spans: text fields and auto-sized `BasicText`.
 */
internal fun TextStyle.forContent(text: CharSequence): TextStyle = if (text.hasTibetan()) forTibetan() else this

private fun tibetanSpan(k: Float) = SpanStyle(
    fontFamily = Jomolhari,
    fontWeight = FontWeight.Normal,
    fontSize = k.em,
    letterSpacing = 0.sp,
)

/**
 * Applies the Tibetan font rules to [text] drawn with [style]:
 * no Tibetan, or an already-Jomolhari style, is unchanged; Tibetan without Latin letters or English
 * digits switches the whole style to Jomolhari ([forTibetan]); mixed text keeps the style and puts a
 * Jomolhari span on each Tibetan run (base line height raised so Tibetan lines do not clip).
 */
internal fun tibetanAware(text: AnnotatedString, style: TextStyle): TibetanAwareText {
    val plain = text.text
    if (!plain.hasTibetan() || style.fontFamily === Jomolhari) return TibetanAwareText(text, style)
    if (!plain.hasLatinLetterOrDigit()) return TibetanAwareText(text, style.forTibetan())
    val k = style.tibetanScale()
    val span = tibetanSpan(k)
    val extra = tibetanRuns(plain).map { Range(span, it.first, it.last + 1) }
    val merged = AnnotatedString(plain, text.spanStyles + extra, text.paragraphStyles)
    val lineHeight = raiseLineHeight(style.lineHeight, style.fontSize.timesOrSelf(k))
    return TibetanAwareText(merged, style.copy(lineHeight = lineHeight))
}

/** String overload of [tibetanAware]. */
internal fun tibetanAware(text: String, style: TextStyle): TibetanAwareText =
    tibetanAware(AnnotatedString(text), style)

/**
 * The app's text composable, replacing Material `Text` everywhere in ui. Any Tibetan in [text] is drawn
 * in Jomolhari (a Compose font family does not fall back per script), with size, weight, letter spacing
 * and line height adjusted; see [tibetanAware]. Text without Tibetan renders exactly like Material `Text`.
 */
@Composable
fun AppText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    textAlign: TextAlign? = null,
    overflow: TextOverflow = TextOverflow.Clip,
    softWrap: Boolean = true,
    maxLines: Int = Int.MAX_VALUE,
    minLines: Int = 1,
    onTextLayout: (TextLayoutResult) -> Unit = {},
    style: TextStyle = LocalTextStyle.current,
) {
    val aware = remember(text, style) { tibetanAware(text, style) }
    Text(
        text = aware.text,
        modifier = modifier,
        color = color,
        textAlign = textAlign,
        overflow = overflow,
        softWrap = softWrap,
        maxLines = maxLines,
        minLines = minLines,
        onTextLayout = onTextLayout,
        style = aware.style,
    )
}

/** [AppText] for styled text; existing spans are preserved and Jomolhari spans are added on Tibetan runs. */
@Composable
fun AppText(
    text: AnnotatedString,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    textAlign: TextAlign? = null,
    overflow: TextOverflow = TextOverflow.Clip,
    softWrap: Boolean = true,
    maxLines: Int = Int.MAX_VALUE,
    minLines: Int = 1,
    onTextLayout: (TextLayoutResult) -> Unit = {},
    style: TextStyle = LocalTextStyle.current,
) {
    val aware = remember(text, style) { tibetanAware(text, style) }
    Text(
        text = aware.text,
        modifier = modifier,
        color = color,
        textAlign = textAlign,
        overflow = overflow,
        softWrap = softWrap,
        maxLines = maxLines,
        minLines = minLines,
        onTextLayout = onTextLayout,
        style = aware.style,
    )
}

/**
 * Section heading label: uppercase and letter-spaced. Tibetan is unaffected by uppercasing and
 * [AppText] drops the letter spacing and draws it in Jomolhari, so no per-language handling is needed.
 */
@Composable
fun SectionLabel(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = PrayerTheme.type.label,
    color: Color = PrayerTheme.colors.textMuted,
) = AppText(text.uppercase(), style = style, color = color, modifier = modifier.semantics { heading() })

/** Small overline label (13sp by default); same casing and Tibetan rules as [SectionLabel]. */
@Composable
fun Overline(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = PrayerTheme.type.overline,
    color: Color = PrayerTheme.colors.textSubtle,
) = AppText(text.uppercase(), style = style, color = color, modifier = modifier)

/** Large screen title (Cormorant; Tibetan text switches to Jomolhari via [AppText]), exposed as a heading. */
@Composable
fun ScreenTitle(text: String, modifier: Modifier = Modifier, color: Color = PrayerTheme.colors.ink) {
    AppText(
        text = text,
        style = PrayerTheme.type.title,
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
    AppText(
        text = text,
        style = style.copy(fontFamily = Jomolhari),
        color = color,
        modifier = modifier,
        maxLines = maxLines,
        overflow = overflow,
        textAlign = textAlign,
    )
}
