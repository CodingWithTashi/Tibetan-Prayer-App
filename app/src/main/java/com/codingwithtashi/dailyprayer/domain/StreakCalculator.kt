package com.codingwithtashi.dailyprayer.domain

import java.time.DayOfWeek
import java.time.LocalDate

data class DayCell(
    val date: LocalDate,
    val dayOfWeek: DayOfWeek,
    val done: Boolean,
    val isToday: Boolean,
    val isFuture: Boolean,
)

object StreakCalculator {
    /** Consecutive practice days ending today, or yesterday if today has no practice yet. */
    fun currentStreak(practiceDates: Set<LocalDate>, today: LocalDate): Int {
        var day = when {
            today in practiceDates -> today
            today.minusDays(1) in practiceDates -> today.minusDays(1)
            else -> return 0
        }
        var count = 0
        while (day in practiceDates) {
            count++
            day = day.minusDays(1)
        }
        return count
    }

    /** Monday..Sunday of today's ISO week. */
    fun weekRow(practiceDates: Set<LocalDate>, today: LocalDate): List<DayCell> {
        val monday = today.minusDays((today.dayOfWeek.value - 1).toLong())
        return (0L..6L).map { i ->
            val d = monday.plusDays(i)
            DayCell(d, d.dayOfWeek, d in practiceDates, d == today, d.isAfter(today))
        }
    }
}
