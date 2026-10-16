package com.codingwithtashi.dailyprayer.ui

import com.codingwithtashi.dailyprayer.data.model.DownloadState
import com.codingwithtashi.dailyprayer.data.repository.DownloadRepository
import com.codingwithtashi.dailyprayer.playback.PlaybackError
import com.codingwithtashi.dailyprayer.playback.PlayerControllerApi
import com.codingwithtashi.dailyprayer.playback.PlayerState
import com.codingwithtashi.dailyprayer.playback.RepeatMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

data class PlayCall(val ids: List<Int>, val startIndex: Int, val contextLabel: String, val routineId: Long?)

/** [PlayerControllerApi] backed by a [MutableStateFlow]; records every call as a string in [calls]. */
class FakePlayerControllerApi : PlayerControllerApi {
    val mutableState = MutableStateFlow(PlayerState())
    override val state: StateFlow<PlayerState> = mutableState
    val mutableErrors = MutableSharedFlow<PlaybackError>(extraBufferCapacity = 8)
    override val errors: SharedFlow<PlaybackError> = mutableErrors

    val calls = mutableListOf<String>()
    var lastPlay: PlayCall? = null
    var playResult: PlaybackError? = null

    override suspend fun play(prayerIds: List<Int>, startIndex: Int, contextLabel: String, routineId: Long?): PlaybackError? {
        lastPlay = PlayCall(prayerIds, startIndex, contextLabel, routineId)
        calls += "play"
        return playResult
    }

    override fun playOrToggle(prayerId: Int, contextLabel: String) { calls += "playOrToggle($prayerId,$contextLabel)" }
    override fun togglePlayPause() { calls += "togglePlayPause" }
    override fun seekTo(ms: Long) { calls += "seekTo($ms)" }
    override fun seekToFraction(fraction: Float) { calls += "seekToFraction($fraction)" }
    override fun next() { calls += "next" }
    override fun previous() { calls += "previous" }
    override fun cycleSpeed() { calls += "cycleSpeed" }
    override fun cycleRepeat() { calls += "cycleRepeat" }
    override fun setRepeat(mode: RepeatMode) { calls += "setRepeat($mode)" }
    override fun skipToQueueIndex(index: Int) { calls += "skipToQueueIndex($index)" }
    override fun stopAndClear() { calls += "stopAndClear" }
}

class FakeDownloadRepository : DownloadRepository {
    val states = HashMap<Int, MutableStateFlow<DownloadState>>()
    val count = MutableStateFlow(0)
    val bytes = MutableStateFlow(0L)
    val calls = mutableListOf<String>()

    fun stateFlow(id: Int) = states.getOrPut(id) { MutableStateFlow(DownloadState.NotDownloaded) }
    override fun state(prayerId: Int): Flow<DownloadState> = stateFlow(prayerId)
    override fun download(prayerId: Int) { calls += "download($prayerId)" }
    override fun cancel(prayerId: Int) { calls += "cancel($prayerId)" }
    override suspend fun delete(prayerId: Int) {
        calls += "delete($prayerId)"
        stateFlow(prayerId).value = DownloadState.NotDownloaded
    }
    override fun downloadedCount(): Flow<Int> = count
    override fun totalBytes(): Flow<Long> = bytes
    override fun storageLabel(bytes: Long): String = "%.1f MB".format(bytes / 1_000_000.0)
}
