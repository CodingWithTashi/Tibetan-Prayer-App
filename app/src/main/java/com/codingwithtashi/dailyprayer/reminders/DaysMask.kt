package com.codingwithtashi.dailyprayer.reminders

import java.time.DayOfWeek

/** Weekday bitmask: bit0 = Monday ... bit6 = Sunday. */
object DaysMask {
    const val EVERY_DAY = 0x7F

    fun bit(day: DayOfWeek): Int = 1 shl (day.value - 1)

    fun contains(mask: Int, day: DayOfWeek): Boolean = mask and bit(day) != 0

    fun toSet(mask: Int): Set<DayOfWeek> = DayOfWeek.entries.filterTo(LinkedHashSet()) { contains(mask, it) }

    fun fromSet(days: Set<DayOfWeek>): Int = days.fold(0) { acc, d -> acc or bit(d) }
}
