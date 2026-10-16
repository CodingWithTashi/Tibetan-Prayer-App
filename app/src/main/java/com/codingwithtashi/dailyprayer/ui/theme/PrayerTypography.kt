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
import com.codingwithtashi.dailyprayer.core.locale.AppLanguage

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

private fun style(
    family: FontFamily,
    size: Int,
    weight: FontWeight = FontWeight.Normal,
    line: Int? = null,
    letterSpacing: TextUnit = TextUnit.Unspecified,
    fontStyle: FontStyle? = null,
    paragraph: Boolean = false,
    lineExact: Float? = null,
): TextStyle = TextStyle(
    fontFamily = family,
    fontSize = size.sp,
    fontWeight = weight,
    fontStyle = fontStyle,
    lineHeight = when {
        lineExact != null -> TextUnit(lineExact, TextUnitType.Sp)
        line != null -> line.sp
        else -> TextUnit.Unspecified
    },
    letterSpacing = letterSpacing,
    platformStyle = NoFontPadding,
    lineHeightStyle = CenterTrimNone,
    lineBreak = if (paragraph) LineBreak.Paragraph else LineBreak.Simple,
)

private val W4 = FontWeight.Normal
private val W5 = FontWeight.Medium
private val W6 = FontWeight.SemiBold

/** Every named text style of the design system (plan Part H3). */
@Immutable
class PrayerTypography {
    // Cormorant Garamond
    val displayLarge = style(CormorantGaramond, 38, W6, 40)
    val displayMedium = style(CormorantGaramond, 32, W6, 35)
    val titleLarge = style(CormorantGaramond, 30, W6, 33)
    val greeting = style(CormorantGaramond, 28, W6, 32)
    val sheetTitle = style(CormorantGaramond, 22, W6, 26)
    val sectionTitle = style(CormorantGaramond, 21, W6, 25)
    val statNumber = style(CormorantGaramond, 20, W6, 20)
    val readerSubtitle = style(CormorantGaramond, 19, W5, 23, fontStyle = FontStyle.Italic)
    val aaGlyph = style(CormorantGaramond, 17, W6)

    // Jomolhari (Tibetan)
    val tibetanHero = style(Jomolhari, 30, line = 48, paragraph = true)
    val tibetanReaderTitle = style(Jomolhari, 32, line = 51, paragraph = true)
    val tibetanTitle30 = style(Jomolhari, 30, line = 40, paragraph = true)
    val tibetanList = style(Jomolhari, 17, line = 26, paragraph = true)
    val tibetanListSmall = style(Jomolhari, 16, line = 24, paragraph = true)
    val tibetanCaption = style(Jomolhari, 15, line = 22, paragraph = true)
    val tibetanCaptionSmall = style(Jomolhari, 11, line = 16, paragraph = true)
    val tibetanTile13 = style(Jomolhari, 13)
    val tibetanTile14 = style(Jomolhari, 14)
    val tibetanTile15 = style(Jomolhari, 15)
    val tibetanTile16 = style(Jomolhari, 16)
    val tibetanTile19 = style(Jomolhari, 19)
    val tibetanTile22 = style(Jomolhari, 22)
    val tibetanVerse = style(Jomolhari, 20, line = 38, paragraph = true)
    val tibetanEndMark = style(Jomolhari, 22)
    val tibUiTab = style(Jomolhari, 13)
    val tibUiPill = style(Jomolhari, 16)
    val tibUiDay = style(Jomolhari, 12)
    val tibUiLangOption = style(Jomolhari, 20)

    /** Jomolhari 10sp, used for the Today week-day labels in Tibetan. */
    val tibetanDay10 = style(Jomolhari, 10)

    /** Reader paragraph style: line height is size x 2.05. */
    fun tibetanReaderBody(sizeSp: Int): TextStyle =
        style(Jomolhari, sizeSp, paragraph = true, lineExact = sizeSp * 2.05f)

    /** Tibetan section label (14sp) and overline (12sp): Jomolhari is illegible at the Latin label sizes. */
    val tibetanLabel14 = style(Jomolhari, 14, line = 20, paragraph = true)
    val tibetanOverline12 = style(Jomolhari, 12, line = 18, paragraph = true)

    // Instrument Sans: uppercase labels
    val overline10 = style(InstrumentSans, 10, W6, letterSpacing = 0.14.em)
    val label11 = style(InstrumentSans, 11, W6, letterSpacing = 0.12.em)
    val label11Wide = style(InstrumentSans, 11, W6, letterSpacing = 0.14.em)
    val label11Narrow = style(InstrumentSans, 11, W6, letterSpacing = 0.10.em)
    val label12 = style(InstrumentSans, 12, W6, letterSpacing = 0.10.em)

    // Captions
    val caption10 = style(InstrumentSans, 10, W5)
    val caption11 = style(InstrumentSans, 11, W5)
    val body11Semi = style(InstrumentSans, 11, W6)
    val caption12 = style(InstrumentSans, 12, W4, 16)
    val caption12Medium = style(InstrumentSans, 12, W5)
    val caption12Semi = style(InstrumentSans, 12, W6)

    // Body
    val body13 = style(InstrumentSans, 13, W4)
    val body13Medium = style(InstrumentSans, 13, W5)
    val body13Semi = style(InstrumentSans, 13, W6)
    val body14 = style(InstrumentSans, 14, W4, 21)
    val body14Medium = style(InstrumentSans, 14, W5)
    val body14Semi = style(InstrumentSans, 14, W6)
    val body15 = style(InstrumentSans, 15, W4, 22)
    val body15Semi = style(InstrumentSans, 15, W6)
    val body16 = style(InstrumentSans, 16, W4, 24)
    val body16Semi = style(InstrumentSans, 16, W6)

    // Inputs and misc
    val input15 = style(InstrumentSans, 15, W4)
    val input16 = style(InstrumentSans, 16, W4)
    val input18 = style(InstrumentSans, 18, W4)
    val counter18 = style(InstrumentSans, 18, W6)
    val playerTitle19 = style(InstrumentSans, 19, W6, 23)
    val avatar15 = style(InstrumentSans, 15, W6)
    val avatar22 = style(InstrumentSans, 22, W6)
    val about13 = style(InstrumentSans, 13, W4, 21)

    // ---- Language-dependent UI label helpers (Tibetan UI renders these in Jomolhari) ----

    /** Bottom bar / rail label: 600 when selected, 500 otherwise; Jomolhari 13sp in Tibetan. */
    fun tab(language: AppLanguage, selected: Boolean): TextStyle =
        if (language == AppLanguage.BO) tibUiTab else if (selected) body11Semi else caption11

    /** Language pill label. */
    fun pill(language: AppLanguage): TextStyle =
        if (language == AppLanguage.BO) tibUiPill else body13Semi

    /** Week-day chip label. */
    fun day(language: AppLanguage): TextStyle =
        if (language == AppLanguage.BO) tibUiDay else body13Semi

    /** Language option / segmented-control label. */
    fun langOption(language: AppLanguage): TextStyle =
        if (language == AppLanguage.BO) tibUiLangOption else body16Semi

    /** Week-day caption on Today: 10sp Sans, Jomolhari in Tibetan. */
    fun weekDay(language: AppLanguage): TextStyle =
        if (language == AppLanguage.BO) tibetanDay10 else caption10

    /** Screen title (Library, Saved, Listen...): Cormorant, Jomolhari 30sp in Tibetan. */
    fun screenTitle(language: AppLanguage): TextStyle =
        if (language == AppLanguage.BO) tibetanTitle30 else titleLarge
}

/** Current typography. */
val LocalPrayerType = staticCompositionLocalOf { PrayerTypography() }
