package com.codingwithtashi.dailyprayer.core.text

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class TibetanScriptTest {
    @Test fun detectsTibetanLettersDigitsAndTsheg() {
        assertThat("བཀྲ".hasTibetan()).isTrue()
        assertThat("༡༢༣".hasTibetan()).isTrue()
        assertThat("་".hasTibetan()).isTrue()
        assertThat("abc 123".hasTibetan()).isFalse()
        assertThat("".hasTibetan()).isFalse()
    }

    @Test fun latinLetterOrDigitIgnoresTibetanAndPunctuation() {
        assertThat("༡༢ / ༡༠༨ · %".hasLatinLetterOrDigit()).isFalse()
        assertThat("༡༢ / 108".hasLatinLetterOrDigit()).isTrue()
        assertThat("Start བཀྲ".hasLatinLetterOrDigit()).isTrue()
    }

    @Test fun runsForMixedString() {
        val text = "Start · ཞོགས་པའི་ཉམས་ལེན།"
        val runs = tibetanRuns(text)
        assertThat(runs).hasSize(1)
        assertThat(text.substring(runs[0].first, runs[0].last + 1)).isEqualTo("ཞོགས་པའི་ཉམས་ལེན།")
    }

    @Test fun whitespaceBetweenTibetanIsJoined() {
        val text = "བཀྲ་ཤིས་ བདེ་ལེགས"
        assertThat(tibetanRuns(text)).containsExactly(0 until text.length)
    }

    @Test fun leadingAndTrailingLatinExcluded() {
        val text = "ab བཀྲ cd"
        val runs = tibetanRuns(text)
        assertThat(runs).containsExactly(3..5)
    }

    @Test fun digitsSeparatedBySlashAreSeparateRuns() {
        val text = "༡༢ / ༡༠༨"
        assertThat(tibetanRuns(text)).containsExactly(0..1, 5..7)
    }

    @Test fun noRunsForLatin() {
        assertThat(tibetanRuns("hello")).isEmpty()
    }
}
