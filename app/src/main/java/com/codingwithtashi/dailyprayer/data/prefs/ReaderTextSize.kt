package com.codingwithtashi.dailyprayer.data.prefs

/** Single home of the Reader prayer-text size range (sp). */
object ReaderTextSize {
    const val MIN = 18
    const val MAX = 36
    const val STEP = 2
    const val DEFAULT = 26

    fun clamp(size: Int): Int = size.coerceIn(MIN, MAX)
}
