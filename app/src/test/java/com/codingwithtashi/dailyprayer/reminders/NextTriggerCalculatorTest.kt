package com.codingwithtashi.dailyprayer.reminders

import com.google.common.truth.Truth.assertThat
import java.time.DayOfWeek
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import org.junit.Test

class NextTriggerCalculatorTest {
    private val utc = ZoneId.of("UTC")
    private fun at(y: Int, mo: Int, d: Int, h: Int, mi: Int, zone: ZoneId = utc) =
        ZonedDateTime.of(LocalDateTime.of(y, mo, d, h, mi), zone)

    // 2026-10-12 is a Monday
    private val mon = at(2026, 10, 12, 8, 0)

    @Test fun laterToday() {
        val r = NextTriggerCalculator.next(mon, 9 * 60, DaysMask.EVERY_DAY)
        assertThat(r).isEqualTo(at(2026, 10, 12, 9, 0))
    }

    @Test fun passedGoesToNextSelectedDay() {
        val r = NextTriggerCalculator.next(mon, 7 * 60, DaysMask.EVERY_DAY)
        assertThat(r).isEqualTo(at(2026, 10, 13, 7, 0))
    }

    @Test fun maskSkipsDays() {
        val mask = DaysMask.fromSet(setOf(DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY))
        assertThat(NextTriggerCalculator.next(mon, 7 * 60, mask)).isEqualTo(at(2026, 10, 14, 7, 0))
    }

    @Test fun zeroMaskIsNull() {
        assertThat(NextTriggerCalculator.next(mon, 7 * 60, 0)).isNull()
        assertThat(NextTriggerCalculator.next(mon, 7 * 60, 0x80)).isNull()
    }

    @Test fun sundayOnlyFromMonday() {
        val r = NextTriggerCalculator.next(mon, 9 * 60, DaysMask.bit(DayOfWeek.SUNDAY))
        assertThat(r).isEqualTo(at(2026, 10, 18, 9, 0))
    }

    @Test fun sameDayOnlyAfterPassedWrapsAWeek() {
        val mask = DaysMask.bit(DayOfWeek.MONDAY)
        assertThat(NextTriggerCalculator.next(mon, 7 * 60, mask)).isEqualTo(at(2026, 10, 19, 7, 0))
    }

    @Test fun dstGapResolvesForward() {
        val ny = ZoneId.of("America/New_York")
        val r = NextTriggerCalculator.next(at(2026, 3, 8, 0, 10, ny), 2 * 60 + 30, DaysMask.EVERY_DAY)
        assertThat(r).isEqualTo(at(2026, 3, 8, 3, 30, ny))
    }

    @Test fun nowEqualToTriggerMovesOn() {
        val now = at(2026, 10, 12, 9, 0)
        assertThat(NextTriggerCalculator.next(now, 9 * 60, DaysMask.EVERY_DAY))
            .isEqualTo(at(2026, 10, 13, 9, 0))
    }
}
