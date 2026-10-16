package com.codingwithtashi.dailyprayer.reminders

import java.time.ZonedDateTime

object NextTriggerCalculator {
    /** First selected-weekday occurrence of the time strictly after [now], or null if no days. */
    fun next(now: ZonedDateTime, timeMinutesOfDay: Int, daysMask: Int): ZonedDateTime? {
        if (daysMask and DaysMask.EVERY_DAY == 0) return null
        val h = timeMinutesOfDay / 60
        val m = timeMinutesOfDay % 60
        val today = now.toLocalDate()
        for (offset in 0L..7L) {
            val date = today.plusDays(offset)
            if (!DaysMask.contains(daysMask, date.dayOfWeek)) continue
            val candidate = date.atTime(h, m).atZone(now.zone)
            if (candidate.isAfter(now)) return candidate
        }
        return null
    }
}
