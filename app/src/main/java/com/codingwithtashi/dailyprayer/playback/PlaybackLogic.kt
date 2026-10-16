package com.codingwithtashi.dailyprayer.playback

import com.codingwithtashi.dailyprayer.core.text.PrayerText
import kotlin.math.floor
import kotlin.math.max

/** Pure helpers behind [PlayerController]; unit-tested. */
object PlaybackLogic {
    const val RESTART_THRESHOLD_MS = 8_000L
    val SPEEDS = listOf(0.8f, 1f, 1.25f)

    fun nextSpeed(current: Float): Float {
        val i = SPEEDS.indexOfFirst { kotlin.math.abs(it - current) < 0.01f }
        return if (i < 0) 1f else SPEEDS[(i + 1) % SPEEDS.size]
    }

    /** True when "previous" should restart the current track instead of going back. */
    fun previousRestarts(positionMs: Long, currentIndex: Int): Boolean =
        positionMs > RESTART_THRESHOLD_MS || currentIndex <= 0

    fun verseIndex(
        text: PrayerText,
        verseCount: Int,
        positionMs: Long,
        durationMs: Long,
        estimatedDurationMs: Long = 0L,
    ): Int {
        if (verseCount <= 0) return 0
        text.verseStartMs?.let { ts ->
            return ts.indexOfLast { it <= positionMs }.coerceAtLeast(0)
        }
        val total = if (durationMs > 0) durationMs else estimatedDurationMs
        return floor(positionMs / max(1L, total).toDouble() * verseCount).toInt().coerceIn(0, verseCount - 1)
    }
}
