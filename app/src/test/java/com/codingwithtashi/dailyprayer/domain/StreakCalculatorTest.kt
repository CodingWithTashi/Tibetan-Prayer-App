package com.codingwithtashi.dailyprayer.domain

import com.google.common.truth.Truth.assertThat
import java.time.DayOfWeek
import java.time.LocalDate
import org.junit.Test

class StreakCalculatorTest {
    private val today = LocalDate.of(2026, 10, 14)
    private fun days(vararg back: Long) = back.map { today.minusDays(it) }.toSet()

    @Test fun empty() = assertThat(StreakCalculator.currentStreak(emptySet(), today)).isEqualTo(0)
    @Test fun todayOnly() = assertThat(StreakCalculator.currentStreak(days(0), today)).isEqualTo(1)
    @Test fun todayAndYesterday() = assertThat(StreakCalculator.currentStreak(days(0, 1), today)).isEqualTo(2)
    @Test fun gapCountsOnlyCurrentRun() =
        assertThat(StreakCalculator.currentStreak(days(0, 1, 3, 4, 5), today)).isEqualTo(2)
    @Test fun runEndingYesterday() =
        assertThat(StreakCalculator.currentStreak(days(1, 2, 3), today)).isEqualTo(3)
    @Test fun runEndingTwoDaysAgo() =
        assertThat(StreakCalculator.currentStreak(days(2, 3), today)).isEqualTo(0)

    @Test fun weekRowAcrossYearBoundary() {
        val thu = LocalDate.of(2026, 1, 1) // Thursday; week starts Mon 2025-12-29
        val row = StreakCalculator.weekRow(setOf(LocalDate.of(2025, 12, 30), thu), thu)
        assertThat(row).hasSize(7)
        assertThat(row.first().date).isEqualTo(LocalDate.of(2025, 12, 29))
        assertThat(row.first().dayOfWeek).isEqualTo(DayOfWeek.MONDAY)
        assertThat(row.last().date).isEqualTo(LocalDate.of(2026, 1, 4))
        assertThat(row.map { it.isToday }).containsExactly(false, false, false, true, false, false, false).inOrder()
        assertThat(row.map { it.isFuture }).containsExactly(false, false, false, false, true, true, true).inOrder()
        assertThat(row.map { it.done }).containsExactly(false, true, false, true, false, false, false).inOrder()
    }

    @Test fun weekRowOnSunday() {
        val sun = LocalDate.of(2026, 10, 18)
        val row = StreakCalculator.weekRow(emptySet(), sun)
        assertThat(row.first().date).isEqualTo(LocalDate.of(2026, 10, 12))
        assertThat(row.last().isToday).isTrue()
    }
}
