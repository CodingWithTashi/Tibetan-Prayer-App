package com.codingwithtashi.dailyprayer.playback

enum class PlaybackSource { LOCAL, STREAM }

enum class PlaybackError { OFFLINE, NETWORK, SOURCE, UNKNOWN }

data class PlayerState(
    val currentPrayerId: Int? = null,
    val queue: List<Int> = emptyList(),
    val currentIndex: Int = 0,
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = false,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val speed: Float = 1f,
    val repeatMode: RepeatMode = RepeatMode.OFF,
    val repeatsLeft: Int = 0,
    val contextLabel: String? = null,
    val routineId: Long? = null,
    val source: PlaybackSource? = null,
    val error: PlaybackError? = null,
    val connected: Boolean = false,
) {
    val hasMedia: Boolean get() = currentPrayerId != null
}
