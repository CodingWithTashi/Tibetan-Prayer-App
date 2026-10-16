package com.codingwithtashi.dailyprayer.playback

import android.content.ComponentName
import android.content.Context
import android.os.Bundle
import androidx.core.content.ContextCompat
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.codingwithtashi.dailyprayer.core.di.ApplicationScope
import com.codingwithtashi.dailyprayer.core.network.ConnectivityChecker
import com.codingwithtashi.dailyprayer.data.repository.PrayerRepository
import com.google.common.util.concurrent.ListenableFuture
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.guava.await
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * App-side handle to [PlaybackService]. Connected in `MainActivity.onStart`, released in `onStop`.
 * All MediaController access happens on the main thread.
 */
@Singleton
class PlayerController @Inject constructor(
    @ApplicationContext private val context: Context,
    private val prayers: PrayerRepository,
    private val connectivity: ConnectivityChecker,
    @ApplicationScope private val scope: CoroutineScope,
) : PlayerControllerApi {
    private val _state = MutableStateFlow(PlayerState())
    override val state: StateFlow<PlayerState> = _state.asStateFlow()

    private val _errors = MutableSharedFlow<PlaybackError>(extraBufferCapacity = 4)
    override val errors: SharedFlow<PlaybackError> = _errors.asSharedFlow()

    private var future: ListenableFuture<MediaController>? = null
    private var controller: MediaController? = null
    private var ticker: Job? = null
    private var localError: PlaybackError? = null
    private var lastServiceError: PlaybackError? = null

    private val listener = object : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) = sync()
    }

    private val controllerListener = object : MediaController.Listener {
        override fun onExtrasChanged(controller: MediaController, extras: Bundle) = sync()
        override fun onDisconnected(controller: MediaController) {
            this@PlayerController.controller = null
            future = null
            ticker?.cancel()
            _state.value = PlayerState()
        }
    }

    fun connect() {
        if (future != null) return
        val token = SessionToken(context, ComponentName(context, PlaybackService::class.java))
        val f = MediaController.Builder(context, token)
            .setListener(controllerListener)
            .buildAsync()
        future = f
        f.addListener({
            if (future !== f) return@addListener
            val c = runCatching { f.get() }.getOrNull() ?: run { future = null; return@addListener }
            controller = c
            c.addListener(listener)
            sync()
            startTicker()
        }, ContextCompat.getMainExecutor(context))
    }

    fun release() {
        ticker?.cancel()
        ticker = null
        controller?.removeListener(listener)
        future?.let { MediaController.releaseFuture(it) }
        future = null
        controller = null
        _state.value = PlayerState()
    }

    private fun startTicker() {
        ticker?.cancel()
        ticker = scope.launch(Dispatchers.Main.immediate) {
            while (isActive) {
                delay(TICK_MS)
                val c = controller ?: continue
                if (_state.value.isPlaying) {
                    val pos = c.currentPosition.coerceAtLeast(0L)
                    _state.update { it.copy(positionMs = pos) }
                }
            }
        }
    }

    private fun sync() {
        val c = controller ?: return
        val extrasOfItem = c.currentMediaItem?.mediaMetadata?.extras
        val session = c.sessionExtras
        val serviceError = session.getString(PlaybackCommands.KEY_ERROR)
            ?.let { n -> PlaybackError.entries.firstOrNull { it.name == n } }
        if (serviceError != null && serviceError != lastServiceError) _errors.tryEmit(serviceError)
        lastServiceError = serviceError
        val queue = (0 until c.mediaItemCount).map { c.getMediaItemAt(it).mediaId.toIntOrNull() ?: -1 }
        val duration = c.duration.takeIf { it != C.TIME_UNSET && it > 0 } ?: 0L
        _state.value = PlayerState(
            currentPrayerId = c.currentMediaItem?.mediaId?.toIntOrNull(),
            queue = queue,
            currentIndex = c.currentMediaItemIndex.coerceAtLeast(0),
            isPlaying = c.isPlaying || (c.playWhenReady && c.playbackState == Player.STATE_BUFFERING),
            isBuffering = c.playbackState == Player.STATE_BUFFERING,
            positionMs = c.currentPosition.coerceAtLeast(0L),
            durationMs = duration,
            speed = c.playbackParameters.speed,
            repeatMode = session.getString(PlaybackCommands.KEY_REPEAT_MODE)
                ?.let { n -> RepeatMode.entries.firstOrNull { it.name == n } } ?: RepeatMode.OFF,
            repeatsLeft = session.getInt(PlaybackCommands.KEY_REPEATS_LEFT, 0),
            contextLabel = extrasOfItem?.getString(PlaybackCommands.KEY_CONTEXT_LABEL),
            routineId = extrasOfItem?.getLong(PlaybackCommands.KEY_ROUTINE_ID, -1L)?.takeIf { it > 0 },
            source = extrasOfItem?.getString(PlaybackCommands.KEY_SOURCE)
                ?.let { n -> PlaybackSource.entries.firstOrNull { it.name == n } },
            error = localError ?: serviceError,
            connected = true,
        )
    }

    private suspend fun awaitController(): MediaController? {
        if (future == null) connect()
        return runCatching { future?.await() }.getOrNull()
    }

    override suspend fun play(
        prayerIds: List<Int>,
        startIndex: Int,
        contextLabel: String,
        routineId: Long?,
    ): PlaybackError? {
        if (prayerIds.isEmpty()) return null
        val found = prayers.getByIds(prayerIds)
        if (found.any { !it.isDownloaded } && !connectivity.isOnline()) {
            localError = PlaybackError.OFFLINE
            _errors.tryEmit(PlaybackError.OFFLINE)
            withContext(Dispatchers.Main.immediate) { sync() }
            _state.update { it.copy(error = PlaybackError.OFFLINE) }
            return PlaybackError.OFFLINE
        }
        withContext(Dispatchers.Main.immediate) {
            val c = awaitController() ?: return@withContext
            localError = null
            val requestExtras = Bundle().apply {
                putLong(PlaybackCommands.KEY_ROUTINE_ID, routineId ?: -1L)
                putString(PlaybackCommands.KEY_CONTEXT_LABEL, contextLabel)
            }
            val items = prayerIds.map { id ->
                MediaItem.Builder()
                    .setMediaId(id.toString())
                    .setRequestMetadata(MediaItem.RequestMetadata.Builder().setExtras(requestExtras).build())
                    .build()
            }
            c.setMediaItems(items, startIndex.coerceIn(0, items.size - 1), 0L)
            c.prepare()
            c.play()
        }
        return null
    }

    override fun playOrToggle(prayerId: Int, contextLabel: String) {
        val s = _state.value
        if (s.currentPrayerId == prayerId && s.queue.size == 1) {
            togglePlayPause()
        } else {
            scope.launch { play(listOf(prayerId), contextLabel = contextLabel) }
        }
    }

    override fun togglePlayPause() {
        val c = controller ?: return
        if (c.isPlaying || (c.playWhenReady && c.playbackState == Player.STATE_BUFFERING)) {
            c.pause()
        } else {
            localError = null
            if (c.playbackState == Player.STATE_IDLE) c.prepare()
            if (c.playbackState == Player.STATE_ENDED) c.seekTo(0)
            c.play()
        }
    }

    override fun seekTo(ms: Long) {
        controller?.seekTo(ms.coerceAtLeast(0L))
        sync()
    }

    override fun seekToFraction(fraction: Float) {
        val d = _state.value.durationMs
        if (d > 0) seekTo((fraction.coerceIn(0f, 1f) * d).toLong())
    }

    override fun next() {
        controller?.seekToNextMediaItem()
    }

    override fun previous() {
        val c = controller ?: return
        if (PlaybackLogic.previousRestarts(c.currentPosition, c.currentMediaItemIndex)) {
            c.seekTo(0)
        } else {
            c.seekToPreviousMediaItem()
        }
    }

    override fun cycleSpeed() {
        val c = controller ?: return
        c.setPlaybackSpeed(PlaybackLogic.nextSpeed(c.playbackParameters.speed))
    }

    override fun cycleRepeat() {
        controller?.sendCustomCommand(PlaybackCommands.cycleRepeat, Bundle.EMPTY)
    }

    override fun setRepeat(mode: RepeatMode) {
        val args = Bundle().apply { putString(PlaybackCommands.KEY_REPEAT_MODE, mode.name) }
        controller?.sendCustomCommand(PlaybackCommands.setRepeat, args)
    }

    override fun skipToQueueIndex(index: Int) {
        val c = controller ?: return
        if (index in 0 until c.mediaItemCount) {
            c.seekTo(index, 0L)
            c.play()
        }
    }

    override fun stopAndClear() {
        val c = controller ?: return
        localError = null
        c.stop()
        c.clearMediaItems()
        sync()
    }

    private companion object {
        const val TICK_MS = 250L
    }
}
