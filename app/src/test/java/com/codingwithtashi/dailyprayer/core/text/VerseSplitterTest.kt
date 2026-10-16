package com.codingwithtashi.dailyprayer.core.text

import com.codingwithtashi.dailyprayer.DataSourceContent
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class VerseSplitterTest {
    private val text = VerseSplitter.split(DataSourceContent.GYUN_CHAK_SUM_CONTENT)

    @Test fun versesAreAtLeastMinLengthExceptLast() {
        assertThat(text.verses).isNotEmpty()
        text.verses.dropLast(1).forEach { assertThat(it.length).isAtLeast(46) }
    }

    @Test fun paragraphsAreVersesChunkedByThree() {
        assertThat(text.paragraphs).isEqualTo(text.verses.chunked(3).map { it.joinToString(" ") })
    }

    @Test fun estimatesHaveFloors() {
        assertThat(text.estDurationSec).isAtLeast(120)
        assertThat(text.estMinutes).isAtLeast(1)
        assertThat(text.verseStartMs).isNull()
    }

    @Test fun syllableStripsMarks() {
        assertThat(VerseSplitter.syllable("༧རྒྱལ་བའི་ཞབས་བརྟན།")).isEqualTo("རྒྱལ")
        assertThat(VerseSplitter.syllable("བསྔོ་བ་དང་སྨོན་ལམ། ༡་")).isEqualTo("བསྔོ")
        assertThat(VerseSplitter.syllable("སྐྱབས་འགྲོ་སེམས་བསྐྱེད།།")).isEqualTo("སྐྱབས")
    }

    @Test fun splitsTertsekTerminatedTexts() {
        // བར་ཆད་ལམ་སེལ། ends its phrases with ༔ instead of །; it must not collapse into one verse.
        val terma = VerseSplitter.split(DataSourceContent.BARCHAT_LAMSEL_CONTENT)
        assertThat(terma.verses.size).isGreaterThan(20)
        assertThat(terma.verses.dropLast(1).all { it.length >= VerseSplitter.MIN_VERSE_LENGTH }).isTrue()
        assertThat(terma.paragraphs.size).isGreaterThan(5)
    }
}
