package com.codingwithtashi.dailyprayer.core.time

import com.codingwithtashi.dailyprayer.core.locale.AppLanguage
import com.google.common.truth.Truth.assertThat
import java.time.LocalDate
import org.junit.Test

class DateFormatsTest {
    @Test fun time12() {
        val en = AppLanguage.EN
        assertThat(DateFormats.time12(0, en)).isEqualTo("12:00 AM")
        assertThat(DateFormats.time12(390, en)).isEqualTo("6:30 AM")
        assertThat(DateFormats.time12(720, en)).isEqualTo("12:00 PM")
        assertThat(DateFormats.time12(1205, en)).isEqualTo("8:05 PM")
        assertThat(DateFormats.time12(1439, en)).isEqualTo("11:59 PM")
    }

    @Test fun time12KeepsEnglishDigitsInTibetan() {
        assertThat(DateFormats.time12(390, AppLanguage.BO)).isEqualTo("6:30 AM")
    }

    @Test fun mmss() {
        assertThat(DateFormats.mmss(0)).isEqualTo("0:00")
        assertThat(DateFormats.mmss(65_000)).isEqualTo("1:05")
        assertThat(DateFormats.mmss(222_000)).isEqualTo("3:42")
        assertThat(DateFormats.mmss(3_725_000)).isEqualTo("1:02:05")
    }

    private val weekdays = listOf("W1", "W2", "W3", "W4", "W5", "W6", "W7")
    private val months = (1..12).map { "M$it" }

    @Test fun dateLabelEnglish() {
        assertThat(DateFormats.dateLabel(LocalDate.of(2026, 10, 14), AppLanguage.EN, weekdays, months))
            .isEqualTo("Wednesday, 14 October")
    }

    @Test fun dateLabelTibetanUsesArraysAndDigits() {
        // 2026-10-14 is a Wednesday -> index 2
        assertThat(DateFormats.dateLabel(LocalDate.of(2026, 10, 14), AppLanguage.BO, weekdays, months))
            .isEqualTo("W3 M10 ཚེས་༡༤")
    }
}
