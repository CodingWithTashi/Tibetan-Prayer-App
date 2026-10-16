package com.codingwithtashi.dailyprayer.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/** Prayer categories used for tile colours and filter dots. */
enum class CategoryKey { DAILY, SUTRA, LIFE, PRAISE, ASPIRE, OTHER }

/** Dot, tile background and tile ink colours for one [CategoryKey]. */
@Immutable
data class CategoryColors(val dot: Color, val tileBg: Color, val tileInk: Color)

/** Every colour token of the design system; two instances: [Light] and [Dark]. */
@Immutable
data class PrayerColors(
    val isDark: Boolean,
    val background: Color,
    val surface: Color,
    val surfaceWarm: Color,
    val segmentBg: Color,
    val border: Color,
    val divider: Color,
    val borderStrong: Color,
    val borderMuted: Color,
    val handle: Color,
    val ink: Color,
    val textMuted: Color,
    val textSubtle: Color,
    val chevron: Color,
    val accent: Color,
    val onAccent: Color,
    val accentSolid: Color,
    val onAccentSolid: Color,
    val darkCard: Color,
    val darkCardText: Color,
    /** 1dp border of dark cards: visible only in the dark theme. */
    val darkCardBorder: Color,
    /** Secondary text on [darkCard] (mini-player sub, counter reset icon). */
    val darkCardMuted: Color,
    /** Goal text on the counter bar. */
    val darkCardSubtle: Color,
    /** Outline of the counter bar's circle buttons. */
    val darkCardOutline: Color,
    /** Track of the counter bar's progress bar. */
    val darkCardTrack: Color,
    val gold: Color,
    val danger: Color,
    val success: Color,
    val scrim: Color,
    val appDictBg: Color,
    val appDictTile: Color,
    val appDictSub: Color,
    val appCalBg: Color,
    val appCalTile: Color,
    val appCalSub: Color,
    val flagBlue: Color = Color(0xFF2E5A8C),
    val flagWhite: Color = Color(0xFFFBF8F1),
    val flagRed: Color = Color(0xFFA8322D),
    val flagGreen: Color = Color(0xFF2F6B4F),
    val flagYellow: Color = Color(0xFFD9A21E),
) {
    companion object {
        val Light = PrayerColors(
            isDark = false,
            background = Color(0xFFF5EFE3),
            surface = Color(0xFFFBF8F1),
            surfaceWarm = Color(0xFFEFE2CF),
            segmentBg = Color(0xFFEDE3D2),
            border = Color(0xFFE4D9C6),
            divider = Color(0xFFE9DFCE),
            borderStrong = Color(0xFFCDBDA2),
            borderMuted = Color(0xFFD8CCB8),
            handle = Color(0xFFDCCFB9),
            ink = Color(0xFF2A1714),
            textMuted = Color(0xFF6B5A50),
            textSubtle = Color(0xFF8A7766),
            chevron = Color(0xFFB8A890),
            accent = Color(0xFF7A1F24),
            onAccent = Color(0xFFF5EFE3),
            accentSolid = Color(0xFF7A1F24),
            onAccentSolid = Color(0xFFF5EFE3),
            darkCard = Color(0xFF2A1714),
            darkCardText = Color(0xFFF5EFE3),
            darkCardBorder = Color.Transparent,
            darkCardMuted = Color(0xFFC9B8A6),
            darkCardSubtle = Color(0xFFA8968A),
            darkCardOutline = Color(0xFF5A4440),
            darkCardTrack = Color(0xFF4A3530),
            gold = Color(0xFFE9B95A),
            danger = Color(0xFFA8322D),
            success = Color(0xFF2F6B4F),
            scrim = Color(0x522A1714),
            appDictBg = Color(0xFFE4EAF2),
            appDictTile = Color(0xFF2E5A8C),
            appDictSub = Color(0xFF4A5A6E),
            appCalBg = Color(0xFFE1ECE5),
            appCalTile = Color(0xFF2F6B4F),
            appCalSub = Color(0xFF4A5E52),
        )

        val Dark = PrayerColors(
            isDark = true,
            background = Color(0xFF160E0C),
            surface = Color(0xFF221614),
            surfaceWarm = Color(0xFF2D1D1A),
            segmentBg = Color(0xFF2A1714),
            border = Color(0xFF3A2724),
            divider = Color(0xFF2D1D1A),
            borderStrong = Color(0xFF4A3530),
            borderMuted = Color(0xFF4A3530),
            handle = Color(0xFF4A3530),
            ink = Color(0xFFF3E6D3),
            textMuted = Color(0xFFC2AE98),
            textSubtle = Color(0xFFB39F8A),
            chevron = Color(0xFF7E6C5E),
            accent = Color(0xFFE9B95A),
            onAccent = Color(0xFF1A0F0E),
            accentSolid = Color(0xFF7A1F24),
            onAccentSolid = Color(0xFFF5EFE3),
            darkCard = Color(0xFF2A1714),
            darkCardText = Color(0xFFF5EFE3),
            darkCardBorder = Color(0xFF4A3530),
            darkCardMuted = Color(0xFFC9B8A6),
            darkCardSubtle = Color(0xFFA8968A),
            darkCardOutline = Color(0xFF5A4440),
            darkCardTrack = Color(0xFF4A3530),
            gold = Color(0xFFE9B95A),
            danger = Color(0xFFE08A86),
            success = Color(0xFF8FB89C),
            scrim = Color(0x73000000),
            appDictBg = Color(0xFF1F2A38),
            appDictTile = Color(0xFF2E5A8C),
            appDictSub = Color(0xFF9BB8DE),
            appCalBg = Color(0xFF1B2A22),
            appCalTile = Color(0xFF2F6B4F),
            appCalSub = Color(0xFF8FB89C),
        )
    }
}

private val CategoryLight = mapOf(
    CategoryKey.DAILY to CategoryColors(Color(0xFF2E5A8C), Color(0xFFE4EAF2), Color(0xFF2E5A8C)),
    CategoryKey.SUTRA to CategoryColors(Color(0xFFB8A890), Color(0xFFECE6DB), Color(0xFF5A4A40)),
    CategoryKey.LIFE to CategoryColors(Color(0xFFA8322D), Color(0xFFF3E2DF), Color(0xFFA8322D)),
    CategoryKey.PRAISE to CategoryColors(Color(0xFF2F6B4F), Color(0xFFE1ECE5), Color(0xFF2F6B4F)),
    CategoryKey.ASPIRE to CategoryColors(Color(0xFFD9A21E), Color(0xFFF5EBCF), Color(0xFF8A600A)),
    CategoryKey.OTHER to CategoryColors(Color(0xFF8A7766), Color(0xFFEDE6DA), Color(0xFF6B5A50)),
)

private val CategoryDark = mapOf(
    CategoryKey.DAILY to CategoryColors(Color(0xFF2E5A8C), Color(0xFF2E5A8C).copy(alpha = 0.18f), Color(0xFF9BB8DE)),
    CategoryKey.SUTRA to CategoryColors(Color(0xFFB8A890), Color(0xFFB8A890).copy(alpha = 0.18f), Color(0xFFD8CCB8)),
    CategoryKey.LIFE to CategoryColors(Color(0xFFA8322D), Color(0xFFA8322D).copy(alpha = 0.18f), Color(0xFFE08A86)),
    CategoryKey.PRAISE to CategoryColors(Color(0xFF2F6B4F), Color(0xFF2F6B4F).copy(alpha = 0.18f), Color(0xFF8FB89C)),
    CategoryKey.ASPIRE to CategoryColors(Color(0xFFD9A21E), Color(0xFFD9A21E).copy(alpha = 0.18f), Color(0xFFE9B95A)),
    CategoryKey.OTHER to CategoryColors(Color(0xFF8A7766), Color(0xFF8A7766).copy(alpha = 0.18f), Color(0xFFB39F8A)),
)

/** Category colours for the current theme. */
fun PrayerColors.category(key: CategoryKey): CategoryColors =
    (if (isDark) CategoryDark else CategoryLight).getValue(key)

/** Category colours for an explicit light/dark choice (used by dark-only surfaces). */
fun categoryColors(key: CategoryKey, dark: Boolean): CategoryColors =
    (if (dark) CategoryDark else CategoryLight).getValue(key)

/** Reader palette; follows the app-wide appearance setting. */
@Immutable
data class ReaderPalette(
    val bg: Color,
    val fg: Color,
    val muted: Color,
    val border: Color,
    val accent: Color,
    /** Next button fill: always the solid maroon. */
    val accentSolid: Color = Color(0xFF7A1F24),
) {
    /** Bottom fade over the reader text: transparent bg to opaque bg (130dp tall overlay). */
    fun fadeBrush(): Brush = Brush.verticalGradient(
        0f to bg.copy(alpha = 0f),
        0.7f to bg,
        1f to bg,
    )

    companion object {
        val Light = ReaderPalette(
            bg = Color(0xFFF5EFE3), fg = Color(0xFF2A1714), muted = Color(0xFF6B5A50),
            border = Color(0xFFE4D9C6), accent = Color(0xFF7A1F24),
        )
        val Dark = ReaderPalette(
            bg = Color(0xFF160E0C), fg = Color(0xFFEADCC8), muted = Color(0xFFA8968A),
            border = Color(0xFF2D1D1A), accent = Color(0xFFE9B95A),
        )
    }
}

/** Fixed, theme-independent colours of the Now Playing screen and mini player. */
object NowPlayingPalette {
    val bg = Color(0xFF160E0C)
    val sheet = Color(0xFF221614)
    val text = Color(0xFFF3E6D3)
    val overline = Color(0xFFB39F8A)
    val time = Color(0xFFB39F8A)
    val tib = Color(0xFFC2AE98)
    val miniSub = Color(0xFFC9B8A6)
    val close = Color(0xFFC9B8A6)
    val verseCurrent = Color(0xFFE9B95A)
    val versePast = Color(0xFF7E6C5E)
    val verseFuture = Color(0xFFCDBBA5)
    val control = Color(0xFFE2B456)
    val playIcon = Color(0xFF1A0F0E)
    val pillBorder = Color(0xFF4A3530)
    val queueDivider = Color(0xFF3A2724)
    val seekTrack = Color(0x2EF3E6D3)
    val artInnerBorder = Color(0x80E9B95A)
    val artBg = Color(0xFF7A1F24)
    val artSyllable = Color(0xFFF2D49A)
    val miniSyllable = Color(0xFFEBCB91)
    val miniTrack = Color(0x26F5EFE3)
    val downloaded = Color(0xFF8FB89C)

    /** Scrim behind the queue bottom sheet (black at 45% opacity). */
    val queueScrim = Color(0x73000000)
}

/** Current colour set. */
val LocalPrayerColors = staticCompositionLocalOf { PrayerColors.Light }

/** Current reader palette. */
val LocalReaderPalette = staticCompositionLocalOf { ReaderPalette.Light }
