package com.codingwithtashi.dailyprayer.playback

import androidx.media3.common.Player

/** Pure repeat bookkeeping for the current track. [repeatsLeft] is [INFINITE_REPEATS] for INFINITE mode. */
class RepeatPolicy {
    var mode: RepeatMode = RepeatMode.OFF
        private set
    var repeatsLeft: Int = 0
        private set

    /** Starts [newMode]; returns the Player repeat mode to apply (ONE when repeating, else OFF). */
    fun start(newMode: RepeatMode): Int {
        mode = newMode
        repeatsLeft = when (newMode) {
            RepeatMode.OFF -> 0
            RepeatMode.THREE -> 2
            RepeatMode.SEVEN -> 6
            RepeatMode.INFINITE -> INFINITE_REPEATS
        }
        return if (newMode == RepeatMode.OFF) Player.REPEAT_MODE_OFF else Player.REPEAT_MODE_ONE
    }

    /**
     * Call when the track auto-repeated once. Returns true when the repeats are exhausted
     * (caller must set the player repeat mode to OFF so the queue advances).
     */
    fun onRepeated(): Boolean {
        if (mode == RepeatMode.OFF) return true
        if (mode == RepeatMode.INFINITE) return false
        repeatsLeft = (repeatsLeft - 1).coerceAtLeast(0)
        if (repeatsLeft == 0) {
            mode = RepeatMode.OFF
            return true
        }
        return false
    }

    fun reset() {
        mode = RepeatMode.OFF
        repeatsLeft = 0
    }

    companion object {
        const val INFINITE_REPEATS = -1
    }
}
