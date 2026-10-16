package com.codingwithtashi.dailyprayer.playback

import com.codingwithtashi.dailyprayer.core.text.PrayerText
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class PlaybackLogicTest {
    private fun text(ts: List<Long>? = null) =
        PrayerText(listOf("a", "b", "c", "d"), emptyList(), 60, 1, ts)

    @Test fun proportionalVerseIndex() {
        assertThat(PlaybackLogic.verseIndex(text(), 4, 0, 100_000)).isEqualTo(0)
        assertThat(PlaybackLogic.verseIndex(text(), 4, 26_000, 100_000)).isEqualTo(1)
        assertThat(PlaybackLogic.verseIndex(text(), 4, 99_000, 100_000)).isEqualTo(3)
        assertThat(PlaybackLogic.verseIndex(text(), 4, 500_000, 100_000)).isEqualTo(3)
    }

    @Test fun usesEstimateWhenDurationUnknown() {
        assertThat(PlaybackLogic.verseIndex(text(), 4, 50_000, 0, estimatedDurationMs = 100_000)).isEqualTo(2)
    }

    @Test fun timestampVerseIndex() {
        val t = text(listOf(0, 1000, 5000, 9000))
        assertThat(PlaybackLogic.verseIndex(t, 4, 0, 0)).isEqualTo(0)
        assertThat(PlaybackLogic.verseIndex(t, 4, 5000, 0)).isEqualTo(2)
        assertThat(PlaybackLogic.verseIndex(t, 4, 8999, 0)).isEqualTo(2)
        assertThat(PlaybackLogic.verseIndex(t, 4, 20_000, 0)).isEqualTo(3)
    }

    @Test fun speedCyclesOrder() {
        assertThat(PlaybackLogic.nextSpeed(0.8f)).isEqualTo(1f)
        assertThat(PlaybackLogic.nextSpeed(1f)).isEqualTo(1.25f)
        assertThat(PlaybackLogic.nextSpeed(1.25f)).isEqualTo(0.8f)
        assertThat(PlaybackLogic.nextSpeed(2f)).isEqualTo(1f)
    }

    @Test fun previousRule() {
        assertThat(PlaybackLogic.previousRestarts(9_000, 2)).isTrue()
        assertThat(PlaybackLogic.previousRestarts(1_000, 0)).isTrue()
        assertThat(PlaybackLogic.previousRestarts(1_000, 2)).isFalse()
        assertThat(PlaybackLogic.previousRestarts(8_000, 1)).isFalse()
    }
}
