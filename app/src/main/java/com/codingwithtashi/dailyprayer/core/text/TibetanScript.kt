package com.codingwithtashi.dailyprayer.core.text

/** True for any character in the Tibetan Unicode block (U+0F00..U+0FFF): letters, Tibetan digits, tsheg, shad and marks. */
fun Char.isTibetan(): Boolean = this in 'ༀ'..'࿿'

/** True when this text contains at least one Tibetan-block character (including Tibetan digits). */
fun CharSequence.hasTibetan(): Boolean = any { it.isTibetan() }

/** True when this text contains a letter or digit that is not Tibetan (for example Latin letters or English digits). */
fun CharSequence.hasLatinLetterOrDigit(): Boolean = any { !it.isTibetan() && it.isLetterOrDigit() }

/**
 * Maximal index ranges of Tibetan text in [text]. Whitespace sitting between two Tibetan characters is
 * joined into the run, so "བཀྲ་ཤིས་ བདེ་ལེགས" is a single run. Leading and trailing whitespace is never part of a run.
 */
fun tibetanRuns(text: CharSequence): List<IntRange> {
    val runs = mutableListOf<IntRange>()
    var start = -1
    var lastTibetan = -1
    for (i in text.indices) {
        val ch = text[i]
        when {
            ch.isTibetan() -> {
                if (start < 0) start = i
                lastTibetan = i
            }
            ch.isWhitespace() && start >= 0 -> Unit
            else -> if (start >= 0) {
                runs += start..lastTibetan
                start = -1
            }
        }
    }
    if (start >= 0) runs += start..lastTibetan
    return runs
}
