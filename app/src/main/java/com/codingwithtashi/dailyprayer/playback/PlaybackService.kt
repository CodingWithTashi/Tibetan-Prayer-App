package com.codingwithtashi.dailyprayer.playback

import androidx.media3.common.util.UnstableApi
import android.app.PendingIntent
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.datasource.HttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.codingwithtashi.dailyprayer.MainActivity
import com.codingwithtashi.dailyprayer.core.di.ApplicationScope
import com.codingwithtashi.dailyprayer.core.notification.DeepLinks
import com.codingwithtashi.dailyprayer.data.repository.PrayerRepository
import com.codingwithtashi.dailyprayer.data.repository.RoutineRepository
import dagger.hilt.android.AndroidEntryPoint
import java.io.FileNotFoundException
import java.net.UnknownHostException
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@androidx.annotation.OptIn(markerClass = [UnstableApi::class])
@AndroidEntryPoint
class PlaybackService : MediaSessionService() {

    @Inject lateinit var prayerRepository: PrayerRepository
    @Inject lateinit var routineRepository: RoutineRepository
    @Inject @ApplicationScope lateinit var appScope: CoroutineScope

    private var session: MediaSession? = null
    private lateinit var player: ExoPlayer
    private val repeat = RepeatPolicy()
    private val handler = Handler(Looper.getMainLooper())
    private val capturedDurations = HashSet<Int>()
    private var error: PlaybackError? = null

    private val idleStop = Runnable {
        val s = session ?: return@Runnable
        val hasClients = s.connectedControllers.any { !s.isMediaNotificationController(it) }
        if (!player.isPlaying && !hasClients) stopSelf()
    }

    override fun onCreate() {
        super.onCreate()
        player = ExoPlayer.Builder(this)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_SPEECH)
                    .build(),
                true,
            )
            .setHandleAudioBecomingNoisy(true)
            .setWakeMode(C.WAKE_MODE_NETWORK)
            .build()
        player.addListener(PlayerListener())

        val activityIntent = Intent(this, MainActivity::class.java)
            .putExtra(DeepLinks.EXTRA_DESTINATION, DeepLinks.DEST_PLAYER)
            .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
        val sessionActivity = PendingIntent.getActivity(
            this,
            0,
            activityIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        session = MediaSession.Builder(this, player)
            .setCallback(
                PlaybackSessionCallback(
                    prayers = prayerRepository,
                    repeat = repeat,
                    scope = appScope,
                    onRepeatChanged = ::publishExtras,
                    onControllerDisconnected = ::scheduleIdleStop,
                ),
            )
            .setSessionActivity(sessionActivity)
            .build()
        publishExtras()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = session

    override fun onTaskRemoved(rootIntent: Intent?) {
        if (!player.playWhenReady || player.mediaItemCount == 0) stopSelf()
    }

    override fun onDestroy() {
        handler.removeCallbacks(idleStop)
        session?.run {
            release()
            session = null
        }
        player.release()
        super.onDestroy()
    }

    private fun scheduleIdleStop() {
        handler.removeCallbacks(idleStop)
        handler.postDelayed(idleStop, IDLE_STOP_MS)
    }

    private fun publishExtras() {
        val b = Bundle().apply {
            putString(PlaybackCommands.KEY_REPEAT_MODE, repeat.mode.name)
            putInt(PlaybackCommands.KEY_REPEATS_LEFT, repeat.repeatsLeft)
            error?.let { putString(PlaybackCommands.KEY_ERROR, it.name) }
        }
        session?.setSessionExtras(b)
    }

    private fun setError(e: PlaybackError?) {
        if (error == e) return
        error = e
        publishExtras()
    }

    private fun markDone(item: MediaItem?) {
        item ?: return
        val extras = item.mediaMetadata.extras ?: return
        val routineId = extras.getLong(PlaybackCommands.KEY_ROUTINE_ID, -1L)
        val prayerId = item.mediaId.toIntOrNull() ?: return
        if (routineId > 0) appScope.launch { routineRepository.markDone(routineId, prayerId) }
    }

    private fun captureDuration(item: MediaItem?) {
        val id = item?.mediaId?.toIntOrNull() ?: return
        val duration = player.duration
        if (duration == C.TIME_UNSET || duration <= 0 || !capturedDurations.add(id)) return
        appScope.launch { prayerRepository.updateAudioDuration(id, duration) }
    }

    private fun mapError(e: PlaybackException): PlaybackError {
        var t: Throwable? = e
        while (t != null) {
            when (t) {
                is HttpDataSource.HttpDataSourceException, is UnknownHostException -> return PlaybackError.NETWORK
                is FileNotFoundException -> return PlaybackError.SOURCE
            }
            t = t.cause
        }
        return when (e.errorCode) {
            PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED,
            PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT,
            -> PlaybackError.NETWORK
            PlaybackException.ERROR_CODE_IO_FILE_NOT_FOUND,
            PlaybackException.ERROR_CODE_PARSING_CONTAINER_MALFORMED,
            PlaybackException.ERROR_CODE_PARSING_CONTAINER_UNSUPPORTED,
            PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS,
            -> PlaybackError.SOURCE
            else -> PlaybackError.UNKNOWN
        }
    }

    private inner class PlayerListener : Player.Listener {
        override fun onPositionDiscontinuity(
            oldPosition: Player.PositionInfo,
            newPosition: Player.PositionInfo,
            reason: Int,
        ) {
            if (reason != Player.DISCONTINUITY_REASON_AUTO_TRANSITION) return
            if (oldPosition.mediaItemIndex == newPosition.mediaItemIndex) {
                if (repeat.onRepeated()) player.repeatMode = Player.REPEAT_MODE_OFF
                publishExtras()
            } else {
                markDone(oldPosition.mediaItem)
            }
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            if (reason == Player.MEDIA_ITEM_TRANSITION_REASON_REPEAT) return
            repeat.reset()
            player.repeatMode = Player.REPEAT_MODE_OFF
            error = null
            publishExtras()
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            when (playbackState) {
                Player.STATE_READY -> {
                    captureDuration(player.currentMediaItem)
                    setError(null)
                }
                Player.STATE_ENDED -> {
                    markDone(player.currentMediaItem)
                    scheduleIdleStop()
                }
            }
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            if (isPlaying) handler.removeCallbacks(idleStop) else scheduleIdleStop()
        }

        override fun onPlayerError(e: PlaybackException) {
            setError(mapError(e))
        }
    }

    private companion object {
        const val IDLE_STOP_MS = 60_000L
    }
}
