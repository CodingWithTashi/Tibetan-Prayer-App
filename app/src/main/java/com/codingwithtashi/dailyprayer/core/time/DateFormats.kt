package com.codingwithtashi.dailyprayer.core.time

import com.codingwithtashi.dailyprayer.core.locale.AppLanguage
import com.codingwithtashi.dailyprayer.core.text.TibetanNumerals
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

object DateFormats {
    private val EN_DATE: DateTimeFormatter = DateTimeFormatter.ofPattern("EEEE, d MMMM", Locale.UK)

    /** "6:30 AM". Always English digits (decision 15), whatever the language. */
    @Suppress("UNUSED_PARAMETER")
    fun time12(minutesOfDay: Int, language: AppLanguage): String {
        val m = ((minutesOfDay % 1440) + 1440) % 1440
        val h24 = m / 60
        val h12 = if (h24 % 12 == 0) 12 else h24 % 12
        val suffix = if (h24 < 12) "AM" else "PM"
        return "$h12:${(m % 60).toString().padStart(2, '0')} $suffix"
    }

    /** "3:42", or "1:02:05" from one hour up. */
    fun mmss(ms: Long): String {
        val total = (maxOf(ms, 0L)) / 1000
        val h = total / 3600
        val m = (total % 3600) / 60
        val s = total % 60
        val ss = s.toString().padStart(2, '0')
        return if (h > 0) "$h:${m.toString().padStart(2, '0')}:$ss" else "$m:$ss"
    }

    /**
     * [weekdayNames] index 0 = Monday; [monthNames] index 0 = January. Only used for BO so we
     * never depend on ICU `bo` data.
     */
    fun dateLabel(
        date: LocalDate,
        language: AppLanguage,
        weekdayNames: List<String>,
        monthNames: List<String>,
    ): String = when (language) {
        AppLanguage.EN -> date.format(EN_DATE)
        AppLanguage.BO ->
            "${weekdayNames[date.dayOfWeek.value - 1]} ${monthNames[date.monthValue - 1]} " +
                "ཚེས་${TibetanNumerals.toTibetan(date.dayOfMonth)}"
    }
}
