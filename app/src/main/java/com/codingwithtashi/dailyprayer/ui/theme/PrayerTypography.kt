package com.codingwithtashi.dailyprayer.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineBreak
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.codingwithtashi.dailyprayer.R
import com.codingwithtashi.dailyprayer.data.model.AppTextSize

/** Jomolhari, the Tibetan face (single weight). */
val Jomolhari = FontFamily(Font(R.font.jomolhari_regular, FontWeight.Normal))

/** Cormorant Garamond: 500, 600 and 500 italic. */
val CormorantGaramond = FontFamily(
    Font(R.font.cormorant_garamond_medium, FontWeight.Medium),
    Font(R.font.cormorant_garamond_semibold, FontWeight.SemiBold),
    Font(R.font.cormorant_garamond_medium_italic, FontWeight.Medium, FontStyle.Italic),
)

/** Instrument Sans: 400, 500 and 600. */
val InstrumentSans = FontFamily(
    Font(R.font.instrument_sans_regular, FontWeight.Normal),
    Font(R.font.instrument_sans_medium, FontWeight.Medium),
    Font(R.font.instrument_sans_semibold, FontWeight.SemiBold),
)

@Suppress("DEPRECATION")
private val NoFontPadding = PlatformTextStyle(includeFontPadding = false)
private val CenterTrimNone = LineHeightStyle(LineHeightStyle.Alignment.Center, LineHeightStyle.Trim.None)

private fun scaled(size: Int, scale: Float): TextUnit = (size * scale).sp

private val W4 = FontWeight.Normal
private val W5 = FontWeight.Medium
private val W6 = FontWeight.SemiBold

/**
 * Every named text style of the design system, named by role (Instrument Sans for UI, Cormorant
 * Garamond for display, Jomolhari for Tibetan). All sizes are in sp, so the system font scale still
 * applies; [textScale] is the app-wide Text size setting and multiplies both font size and line height.
 */
@Immutable
class PrayerTypography(val textScale: Float = 1f) {
    private fun style(
        family: FontFamily,
        size: Int,
        weight: FontWeight = FontWeight.Normal,
        line: Int? = null,
        letterSpacing: TextUnit = TextUnit.Unspecified,
        fontStyle: FontStyle? = null,
        paragraph: Boolean = false,
        lineExact: Float? = null,
        features: String? = null,
    ): TextStyle = TextStyle(
        fontFamily = family,
        fontSize = scaled(size, textScale),
        fontWeight = weight,
        fontStyle = fontStyle,
        lineHeight = when {
            lineExact != null -> TextUnit(lineExact * textScale, TextUnitType.Sp)
            line != null -> scaled(line, textScale)
            else -> TextUnit.Unspecified
        },
        letterSpacing = letterSpacing,
        fontFeatureSettings = features,
        platformStyle = NoFontPadding,
        lineHeightStyle = CenterTrimNone,
        lineBreak = if (paragraph) LineBreak.Paragraph else LineBreak.Simple,
    )

    // Cormorant Garamond: display and headings
    /** App wordmark. */
    val wordmark = style(CormorantGaramond, 40, W6, 44)
    val headline = style(CormorantGaramond, 34, W6, 38)
    /** Screen title (Library, Saved, Listen...). */
    val title = style(CormorantGaramond, 32, W6, 36)
    val greeting = style(CormorantGaramond, 32, W6, 36)
    val greetingCompact = style(CormorantGaramond, 28, W6, 32)
    val sheetTitle = style(CormorantGaramond, 24, W6, 30)
    val sectionTitle = style(CormorantGaramond, 24, W6, 30)

    /** Stat numbers use lining figures so "0" reads as zero. */
    val statNumber = style(CormorantGaramond, 28, W6, 30, features = "lnum")
    val readerSubtitle = style(CormorantGaramond, 20, W5, 26, fontStyle = FontStyle.Italic)
    val aaGlyph = style(CormorantGaramond, 20, W6)

    // Jomolhari (Tibetan); line heights are at least 1.5x the size
    val tibetanHero = style(Jomolhari, 32, line = 52, paragraph = true)
    val tibetanReaderTitle = style(Jomolhari, 34, line = 54, paragraph = true)
    val tibetanList = style(Jomolhari, 20, line = 32, paragraph = true)
    val tibetanListSmall = style(Jomolhari, 19, line = 30, paragraph = true)
    val tibetanCaption = style(Jomolhari, 17, line = 27, paragraph = true)
    val tibetanCaptionSmall = style(Jomolhari, 14, line = 22, paragraph = true)

    // Tibetan glyphs inside tiles, smallest to largest
    val tileGlyphMini = style(Jomolhari, 15)
    val tileGlyphSmall = style(Jomolhari, 16)
    val tileGlyphMedium = style(Jomolhari, 17)
    val tileGlyphLarge = style(Jomolhari, 18)
    val tileGlyphXL = style(Jomolhari, 21)
    val tileGlyphHero = style(Jomolhari, 24)

    /** Smallest size a tile glyph auto-shrinks to before it would overflow its tile. */
    val tileGlyphMinSize: TextUnit = 13.sp
    val tibetanVerse = style(Jomolhari, 24, line = 44, paragraph = true)
    val tibetanEndMark = style(Jomolhari, 26)

    /** Reader paragraph style: line height is size x 2.05. */
    fun tibetanReaderBody(sizeSp: Int): TextStyle =
        style(Jomolhari, sizeSp, paragraph = true, lineExact = sizeSp * 2.05f)

    // Instrument Sans: uppercase labels
    val overline = style(InstrumentSans, 13, W6, letterSpacing = 0.12.em)
    val label = style(InstrumentSans, 14, W6, letterSpacing = 0.10.em)

    // Captions
    val caption = style(InstrumentSans, 13, W5)
    val captionStrong = style(InstrumentSans, 13, W6)
    val bodySmall = style(InstrumentSans, 14, W4, 21)
    val bodySmallMedium = style(InstrumentSans, 14, W5)
    val bodySmallStrong = style(InstrumentSans, 14, W6)

    // Body
    val body = style(InstrumentSans, 16, W4, 24)
    val bodyMedium = style(InstrumentSans, 16, W5)
    val bodyStrong = style(InstrumentSans, 16, W6)
    /** Row titles and 48-52dp buttons. */
    val bodyLarge = style(InstrumentSans, 17, W4, 25)
    val bodyLargeStrong = style(InstrumentSans, 17, W6)
    val lead = style(InstrumentSans, 18, W4, 26)
    val leadStrong = style(InstrumentSans, 18, W6)

    // Inputs and misc
    val input = style(InstrumentSans, 17, W4)
    val inputLarge = style(InstrumentSans, 20, W4)
    val counter = style(InstrumentSans, 22, W6)
    val playerTitle = style(InstrumentSans, 22, W6, 28)
    val avatarSmall = style(InstrumentSans, 17, W6)
    val avatarLarge = style(InstrumentSans, 26, W6)

    /** "A" / "ཀ" glyph for the Text size selector, at 14/17/20/24sp for SMALL..LARGEST (Tibetan is handled by AppText). */
    fun textSizeSample(option: AppTextSize): TextStyle {
        val size = when (option) {
            AppTextSize.SMALL -> 14
            AppTextSize.DEFAULT -> 17
            AppTextSize.LARGE -> 20
            AppTextSize.LARGEST -> 24
        }
        return style(InstrumentSans, size, W6)
    }

    /** Every token by name, for the gallery preview and tests. */
    val all: Map<String, TextStyle> by lazy {
        mapOf(
            "wordmark" to wordmark, "headline" to headline, "title" to title, "greeting" to greeting,
            "greetingCompact" to greetingCompact, "sheetTitle" to sheetTitle, "sectionTitle" to sectionTitle,
            "statNumber" to statNumber, "readerSubtitle" to readerSubtitle, "aaGlyph" to aaGlyph,
            "tibetanHero" to tibetanHero, "tibetanReaderTitle" to tibetanReaderTitle,
            "tibetanList" to tibetanList, "tibetanListSmall" to tibetanListSmall, "tibetanCaption" to tibetanCaption,
            "tibetanCaptionSmall" to tibetanCaptionSmall, "tileGlyphMini" to tileGlyphMini,
            "tileGlyphSmall" to tileGlyphSmall, "tileGlyphMedium" to tileGlyphMedium, "tileGlyphLarge" to tileGlyphLarge,
            "tileGlyphXL" to tileGlyphXL, "tileGlyphHero" to tileGlyphHero, "tibetanVerse" to tibetanVerse,
            "tibetanEndMark" to tibetanEndMark,
            "overline" to overline, "label" to label, "caption" to caption, "captionStrong" to captionStrong,
            "bodySmall" to bodySmall, "bodySmallMedium" to bodySmallMedium, "bodySmallStrong" to bodySmallStrong,
            "body" to body, "bodyMedium" to bodyMedium, "bodyStrong" to bodyStrong, "bodyLarge" to bodyLarge,
            "bodyLargeStrong" to bodyLargeStrong, "lead" to lead, "leadStrong" to leadStrong, "input" to input,
            "inputLarge" to inputLarge, "counter" to counter, "playerTitle" to playerTitle,
            "avatarSmall" to avatarSmall, "avatarLarge" to avatarLarge,
        )
    }
}

/** Current typography. */
val LocalPrayerType = staticCompositionLocalOf { PrayerTypography() }
