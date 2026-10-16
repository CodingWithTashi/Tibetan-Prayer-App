package com.codingwithtashi.dailyprayer.core.text

import kotlin.math.roundToInt

object VerseSplitter {
    const val MIN_VERSE_LENGTH = 46
    const val VERSES_PER_PARAGRAPH = 3

    // A phrase ends at a shad (།) or a tertsek (༔, used by terma texts such as བར་ཆད་ལམ་སེལ།).
    private val PHRASE_SPLIT = Regex("(?<=[།༔])\\s+|\\n+")

    fun split(text: String): PrayerText {
        val phrases = text.replace("\r", "")
            .split(PHRASE_SPLIT)
            .map { it.trim() }
            .filter { it.isNotEmpty() }

        val verses = ArrayList<String>()
        var current = ""
        for (phrase in phrases) {
            current = if (current.isEmpty()) phrase else "$current $phrase"
            if (current.length >= MIN_VERSE_LENGTH) {
                verses += current
                current = ""
            }
        }
        if (current.isNotEmpty()) verses += current

        val paragraphs = verses.chunked(VERSES_PER_PARAGRAPH).map { it.joinToString(" ") }
        val len = text.length
        return PrayerText(
            verses = verses,
            paragraphs = paragraphs,
            estDurationSec = maxOf(120, (len / 8.0).roundToInt()),
            estMinutes = maxOf(1, (len / 320.0).roundToInt()),
        )
    }

    /** First syllable of a Tibetan title without whitespace, shad, head marks and Tibetan digits. */
    fun syllable(title: String): String =
        title.trim().split('་').first().filter {
            !it.isWhitespace() && it != '།' && it != '༄' && it != '༅' && it !in '༠'..'༩'
        }
}
