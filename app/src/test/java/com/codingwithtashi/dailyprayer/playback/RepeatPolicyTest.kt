package com.codingwithtashi.dailyprayer.playback

import androidx.media3.common.Player
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class RepeatPolicyTest {
    @Test fun offHasNoRepeats() {
        val p = RepeatPolicy()
        assertThat(p.start(RepeatMode.OFF)).isEqualTo(Player.REPEAT_MODE_OFF)
        assertThat(p.repeatsLeft).isEqualTo(0)
    }

    @Test fun threeRepeatsTwiceThenExhausts() {
        val p = RepeatPolicy()
        assertThat(p.start(RepeatMode.THREE)).isEqualTo(Player.REPEAT_MODE_ONE)
        assertThat(p.repeatsLeft).isEqualTo(2)
        assertThat(p.onRepeated()).isFalse()
        assertThat(p.repeatsLeft).isEqualTo(1)
        assertThat(p.onRepeated()).isTrue()
        assertThat(p.mode).isEqualTo(RepeatMode.OFF)
    }

    @Test fun sevenRepeatsSixTimes() {
        val p = RepeatPolicy()
        p.start(RepeatMode.SEVEN)
        assertThat(p.repeatsLeft).isEqualTo(6)
        repeat(5) { assertThat(p.onRepeated()).isFalse() }
        assertThat(p.onRepeated()).isTrue()
    }

    @Test fun infiniteNeverExhausts() {
        val p = RepeatPolicy()
        p.start(RepeatMode.INFINITE)
        assertThat(p.repeatsLeft).isEqualTo(RepeatPolicy.INFINITE_REPEATS)
        repeat(100) { assertThat(p.onRepeated()).isFalse() }
        assertThat(p.mode).isEqualTo(RepeatMode.INFINITE)
    }

    @Test fun resetClears() {
        val p = RepeatPolicy()
        p.start(RepeatMode.SEVEN)
        p.reset()
        assertThat(p.mode).isEqualTo(RepeatMode.OFF)
        assertThat(p.repeatsLeft).isEqualTo(0)
    }

    @Test fun modeCyclesInOrder() {
        assertThat(generateSequence(RepeatMode.OFF) { it.next() }.take(5).toList()).containsExactly(
            RepeatMode.OFF, RepeatMode.THREE, RepeatMode.SEVEN, RepeatMode.INFINITE, RepeatMode.OFF,
        ).inOrder()
    }
}
