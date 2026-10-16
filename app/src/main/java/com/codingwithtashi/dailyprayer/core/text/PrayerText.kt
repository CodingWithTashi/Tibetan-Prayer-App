package com.codingwithtashi.dailyprayer.core.text

/** Pre-split prayer text. [verseStartMs] is reserved for future per-verse audio timestamps. */
data class PrayerText(
    val verses: List<String>,
    val paragraphs: List<String>,
    val estDurationSec: Int,
    val estMinutes: Int,
    val verseStartMs: List<Long>? = null,
)
