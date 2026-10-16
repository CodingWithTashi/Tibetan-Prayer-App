package com.codingwithtashi.dailyprayer.reminders

import com.google.common.truth.Truth.assertThat
import java.time.DayOfWeek
import org.junit.Test

class DaysMaskTest {
    @Test fun bits() {
        assertThat(DaysMask.bit(DayOfWeek.MONDAY)).isEqualTo(1)
        assertThat(DaysMask.bit(DayOfWeek.SUNDAY)).isEqualTo(64)
    }

    @Test fun everyDay() {
        assertThat(DaysMask.EVERY_DAY).isEqualTo(127)
        assertThat(DaysMask.toSet(DaysMask.EVERY_DAY)).hasSize(7)
    }

    @Test fun roundTrip() {
        val s = setOf(DayOfWeek.TUESDAY, DayOfWeek.SATURDAY)
        val mask = DaysMask.fromSet(s)
        assertThat(mask).isEqualTo(2 or 32)
        assertThat(DaysMask.toSet(mask)).isEqualTo(s)
        assertThat(DaysMask.contains(mask, DayOfWeek.TUESDAY)).isTrue()
        assertThat(DaysMask.contains(mask, DayOfWeek.MONDAY)).isFalse()
    }

    @Test fun empty() {
        assertThat(DaysMask.fromSet(emptySet())).isEqualTo(0)
        assertThat(DaysMask.toSet(0)).isEmpty()
    }
}
