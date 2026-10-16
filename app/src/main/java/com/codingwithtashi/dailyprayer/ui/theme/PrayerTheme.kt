package com.codingwithtashi.dailyprayer.ui.theme

import android.provider.Settings
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalRippleConfiguration
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RippleConfiguration
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.isSpecified
import androidx.core.os.ConfigurationCompat
import com.codingwithtashi.dailyprayer.core.locale.AppLanguage

/** Current UI language (en/bo). */
val LocalAppLanguage = staticCompositionLocalOf { AppLanguage.EN }

/** True when the system animator scale is 0 (durations collapse to 0). */
val LocalReduceMotion = staticCompositionLocalOf { false }

/** Reads `Settings.Global.ANIMATOR_DURATION_SCALE == 0f` once per composition. */
@Composable
fun rememberReduceMotion(): Boolean {
    val context = LocalContext.current
    return remember(context) {
        runCatching {
            Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
        }.getOrDefault(false)
    }
}

/** Returns [ms], or 0 when reduce-motion is on. */
@Composable
@ReadOnlyComposable
fun motionDuration(ms: Int): Int = if (LocalReduceMotion.current) 0 else ms

private fun lightScheme() = lightColorScheme(
    primary = Color(0xFF7A1F24), onPrimary = Color(0xFFF5EFE3),
    primaryContainer = Color(0xFFEFE2CF), onPrimaryContainer = Color(0xFF7A1F24),
    secondary = Color(0xFF6B5A50),
    background = Color(0xFFF5EFE3), onBackground = Color(0xFF2A1714),
    surface = Color(0xFFFBF8F1), onSurface = Color(0xFF2A1714),
    surfaceVariant = Color(0xFFEFE2CF), onSurfaceVariant = Color(0xFF6B5A50),
    surfaceContainerLowest = Color(0xFFFBF8F1), surfaceContainerLow = Color(0xFFFBF8F1),
    surfaceContainer = Color(0xFFFBF8F1), surfaceContainerHigh = Color(0xFFFBF8F1),
    surfaceContainerHighest = Color(0xFFFBF8F1),
    outline = Color(0xFFE4D9C6), outlineVariant = Color(0xFFE9DFCE),
    error = Color(0xFFA8322D), scrim = Color(0xFF2A1714),
    inverseSurface = Color(0xFF2A1714), inverseOnSurface = Color(0xFFF5EFE3),
    inversePrimary = Color(0xFFE9B95A),
)

private fun darkScheme() = darkColorScheme(
    primary = Color(0xFFE9B95A), onPrimary = Color(0xFF1A0F0E),
    primaryContainer = Color(0xFF2D1D1A), onPrimaryContainer = Color(0xFFE9B95A),
    secondary = Color(0xFFC2AE98),
    background = Color(0xFF160E0C), onBackground = Color(0xFFF3E6D3),
    surface = Color(0xFF221614), onSurface = Color(0xFFF3E6D3),
    surfaceVariant = Color(0xFF2D1D1A), onSurfaceVariant = Color(0xFFC2AE98),
    surfaceContainerLowest = Color(0xFF221614), surfaceContainerLow = Color(0xFF221614),
    surfaceContainer = Color(0xFF221614), surfaceContainerHigh = Color(0xFF221614),
    surfaceContainerHighest = Color(0xFF221614),
    outline = Color(0xFF3A2724), outlineVariant = Color(0xFF2D1D1A),
    error = Color(0xFFE08A86), scrim = Color(0xFF000000),
    inverseSurface = Color(0xFFF3E6D3), inverseOnSurface = Color(0xFF2A1714),
    inversePrimary = Color(0xFF7A1F24),
)

private fun TextStyle.scaled(scale: Float, family: FontFamily): TextStyle = copy(
    fontFamily = family,
    fontSize = if (fontSize.isSpecified) fontSize * scale else fontSize,
    lineHeight = if (lineHeight.isSpecified) lineHeight * scale else lineHeight,
)

private fun materialTypography(textScale: Float): Typography {
    val d = Typography()
    val f = InstrumentSans
    fun TextStyle.s() = scaled(textScale, f)
    return Typography(
        displayLarge = d.displayLarge.s(), displayMedium = d.displayMedium.s(),
        displaySmall = d.displaySmall.s(), headlineLarge = d.headlineLarge.s(),
        headlineMedium = d.headlineMedium.s(), headlineSmall = d.headlineSmall.s(),
        titleLarge = d.titleLarge.s(), titleMedium = d.titleMedium.s(),
        titleSmall = d.titleSmall.s(), bodyLarge = d.bodyLarge.s(),
        bodyMedium = d.bodyMedium.s(), bodySmall = d.bodySmall.s(),
        labelLarge = d.labelLarge.s(), labelMedium = d.labelMedium.s(),
        labelSmall = d.labelSmall.s(),
    )
}

/**
 * Root theme: provides the colour/type/reader/language/motion locals, the ripple colour and the
 * Material 3 bridge (colour scheme, shapes, typography). [textScale] is the app-wide Text size setting.
 */
@Composable
fun PrayerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    language: AppLanguage = AppLanguage.fromTag(ConfigurationCompat.getLocales(LocalConfiguration.current)[0]?.language),
    reduceMotion: Boolean = rememberReduceMotion(),
    textScale: Float = 1f,
    content: @Composable () -> Unit,
) {
    val colors = if (darkTheme) PrayerColors.Dark else PrayerColors.Light
    val reader = if (darkTheme) ReaderPalette.Dark else ReaderPalette.Light
    val type = remember(textScale) { PrayerTypography(textScale) }
    val scheme = remember(darkTheme) { if (darkTheme) darkScheme() else lightScheme() }
    val m3Typography = remember(textScale) { materialTypography(textScale) }
    CompositionLocalProvider(
        LocalPrayerColors provides colors,
        LocalPrayerType provides type,
        LocalAppLanguage provides language,
        LocalReaderPalette provides reader,
        LocalReduceMotion provides reduceMotion,
        LocalContentColor provides colors.ink,
        LocalRippleConfiguration provides remember(colors.accent) { RippleConfiguration(color = colors.accent) },
    ) {
        MaterialTheme(colorScheme = scheme, shapes = PrayerShapes.material, typography = m3Typography, content = content)
    }
}

/** Accessors for the current theme locals. */
object PrayerTheme {
    val colors: PrayerColors @Composable @ReadOnlyComposable get() = LocalPrayerColors.current
    val type: PrayerTypography @Composable @ReadOnlyComposable get() = LocalPrayerType.current
    val language: AppLanguage @Composable @ReadOnlyComposable get() = LocalAppLanguage.current
    val reader: ReaderPalette @Composable @ReadOnlyComposable get() = LocalReaderPalette.current
    val reduceMotion: Boolean @Composable @ReadOnlyComposable get() = LocalReduceMotion.current
}
