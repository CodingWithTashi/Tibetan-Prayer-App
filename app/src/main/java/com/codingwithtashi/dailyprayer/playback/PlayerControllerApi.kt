package com.codingwithtashi.dailyprayer.playback

import com.codingwithtashi.dailyprayer.core.text.PrayerText
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

/** What ViewModels see of [PlayerController]; lets tests substitute a fake. */
interface PlayerControllerApi {
    val state: StateFlow<PlayerState>
    val errors: SharedFlow<PlaybackError>

    /** Starts [prayerIds]; returns the blocking error (offline) or null when playback was requested. */
    suspend fun play(
        prayerIds: List<Int>,
        startIndex: Int = 0,
        contextLabel: String,
        routineId: Long? = null,
    ): PlaybackError?

    fun playOrToggle(prayerId: Int, contextLabel: String)
    fun togglePlayPause()
    fun seekTo(ms: Long)
    fun seekToFraction(fraction: Float)
    fun next()
    fun previous()
    fun cycleSpeed()
    fun cycleRepeat()
    fun setRepeat(mode: RepeatMode)
    fun skipToQueueIndex(index: Int)
    fun stopAndClear()

    fun currentVerseIndex(text: PrayerText, verseCount: Int, estimatedDurationMs: Long = 0L): Int {
        val s = state.value
        return PlaybackLogic.verseIndex(text, verseCount, s.positionMs, s.durationMs, estimatedDurationMs)
    }
}
