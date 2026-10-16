package com.codingwithtashi.dailyprayer.domain

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class RoutineProgressCalculatorTest {
    @Test fun empty() {
        val p = RoutineProgressCalculator.compute(emptyList(), setOf(1))
        assertThat(p.total).isEqualTo(0)
        assertThat(p.isComplete).isFalse()
        assertThat(p.nextPrayerId).isNull()
        assertThat(p.fraction).isEqualTo(0f)
        assertThat(RoutineProgressCalculator.startLabel(p)).isEqualTo(StartLabel.START_READING)
    }

    @Test fun noneDone() {
        val p = RoutineProgressCalculator.compute(listOf(5, 6, 7), emptySet())
        assertThat(p.doneCount).isEqualTo(0)
        assertThat(p.nextPrayerId).isEqualTo(5)
        assertThat(RoutineProgressCalculator.startLabel(p)).isEqualTo(StartLabel.START_READING)
    }

    @Test fun partialNextIsFirstUndoneEvenIfLaterDone() {
        val p = RoutineProgressCalculator.compute(listOf(5, 6, 7), setOf(5, 7, 99))
        assertThat(p.doneIds).containsExactly(5, 7)
        assertThat(p.doneCount).isEqualTo(2)
        assertThat(p.nextPrayerId).isEqualTo(6)
        assertThat(p.fraction).isWithin(1e-6f).of(2f / 3f)
        assertThat(RoutineProgressCalculator.startLabel(p)).isEqualTo(StartLabel.CONTINUE)
    }

    @Test fun complete() {
        val p = RoutineProgressCalculator.compute(listOf(5, 6), setOf(6, 5))
        assertThat(p.isComplete).isTrue()
        assertThat(p.nextPrayerId).isEqualTo(5)
        assertThat(p.fraction).isEqualTo(1f)
        assertThat(RoutineProgressCalculator.startLabel(p)).isEqualTo(StartLabel.READ_AGAIN)
    }
}
