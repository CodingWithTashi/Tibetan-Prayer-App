package com.codingwithtashi.dailyprayer.ui.theme

import androidx.compose.ui.unit.isSpecified
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PrayerTypographyTest {
    private val base = PrayerTypography(1f)

    @Test fun noTokenBelow13spAtDefaultScale() {
        base.all.forEach { (name, style) ->
            assertThat(style.fontSize.value).isAtLeast(13f)
            assertThat(name).isNotEmpty()
        }
    }

    @Test fun scaleMultipliesSizeAndLineHeight() {
        val big = PrayerTypography(1.3f)
        listOf("body", "bodySmall", "title", "tibetanList", "tibetanVerse", "lead").forEach { name ->
            val a = base.all.getValue(name)
            val b = big.all.getValue(name)
            assertThat(b.fontSize.value).isWithin(0.01f).of(a.fontSize.value * 1.3f)
            if (a.lineHeight.isSpecified) {
                assertThat(b.lineHeight.value).isWithin(0.01f).of(a.lineHeight.value * 1.3f)
            }
        }
        val a = base.tibetanReaderBody(26)
        val b = big.tibetanReaderBody(26)
        assertThat(b.fontSize.value).isWithin(0.01f).of(26f * 1.3f)
        assertThat(b.lineHeight.value).isWithin(0.01f).of(a.lineHeight.value * 1.3f)
        assertThat(a.lineHeight.value).isWithin(0.01f).of(26f * 2.05f)
    }

    @Test fun jomolhariLineHeightsAreAtLeastOnePointFiveTimesSize() {
        val styles = base.all.values + base.tibetanReaderBody(18) + base.tibetanReaderBody(36)
        styles.filter { it.fontFamily === Jomolhari && it.lineHeight.isSpecified }.forEach {
            assertThat(it.lineHeight.value).isAtLeast(it.fontSize.value * 1.5f)
        }
    }
}
