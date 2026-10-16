package com.codingwithtashi.dailyprayer.core.text

import com.codingwithtashi.dailyprayer.core.locale.AppLanguage
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class TibetanNumeralsTest {
    @Test fun zero() = assertThat(TibetanNumerals.toTibetan(0)).isEqualTo("༠")
    @Test fun seven() = assertThat(TibetanNumerals.toTibetan(7)).isEqualTo("༧")
    @Test fun oneOhEight() = assertThat(TibetanNumerals.toTibetan(108)).isEqualTo("༡༠༨")
    @Test fun tenThousandLong() = assertThat(TibetanNumerals.toTibetan(10000L)).isEqualTo("༡༠༠༠༠")
    @Test fun negativeKeepsSign() = assertThat(TibetanNumerals.toTibetan(-42)).isEqualTo("-༤༢")
    @Test fun stringPassesNonDigits() = assertThat(TibetanNumerals.toTibetan("1:30")).isEqualTo("༡:༣༠")

    @Test fun formatCountPerLanguage() {
        assertThat(TibetanNumerals.formatCount(10000, AppLanguage.EN)).isEqualTo("10000")
        assertThat(TibetanNumerals.formatCount(108, AppLanguage.BO)).isEqualTo("༡༠༨")
    }

    @Test fun percent() {
        assertThat(TibetanNumerals.formatPercent(0.42f, AppLanguage.EN)).isEqualTo("42%")
        assertThat(TibetanNumerals.formatPercent(0.42f, AppLanguage.BO)).isEqualTo("༤༢%")
        assertThat(TibetanNumerals.formatPercent(1f, AppLanguage.EN)).isEqualTo("100%")
    }

    @Test fun minutes() {
        assertThat(TibetanNumerals.formatMinutes(5, AppLanguage.EN)).isEqualTo("5")
        assertThat(TibetanNumerals.formatMinutes(5, AppLanguage.BO)).isEqualTo("༥")
    }
}
