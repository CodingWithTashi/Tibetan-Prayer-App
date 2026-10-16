package com.codingwithtashi.dailyprayer.playback

import androidx.media3.common.util.UnstableApi
import android.net.Uri
import android.os.Bundle
import androidx.core.net.toUri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.session.MediaSession
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionCommands
import androidx.media3.session.SessionError
import androidx.media3.session.SessionResult
import com.codingwithtashi.dailyprayer.data.model.Prayer
import com.codingwithtashi.dailyprayer.data.repository.PrayerRepository
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.guava.future

/** Resolves controller-sent media ids into playable items and handles the custom repeat commands. */
@androidx.annotation.OptIn(markerClass = [UnstableApi::class])
class PlaybackSessionCallback(
    private val prayers: PrayerRepository,
    private val repeat: RepeatPolicy,
    private val scope: CoroutineScope,
    private val onRepeatChanged: () -> Unit,
    private val onControllerDisconnected: () -> Unit,
) : MediaSession.Callback {

    override fun onConnect(
        session: MediaSession,
        controller: MediaSession.ControllerInfo,
    ): MediaSession.ConnectionResult {
        val commands: SessionCommands = MediaSession.ConnectionResult.DEFAULT_SESSION_COMMANDS.buildUpon()
            .add(PlaybackCommands.cycleRepeat)
            .add(PlaybackCommands.setRepeat)
            .build()
        return MediaSession.ConnectionResult.AcceptedResultBuilder(session)
            .setAvailableSessionCommands(commands)
            .build()
    }

    override fun onDisconnected(session: MediaSession, controller: MediaSession.ControllerInfo) {
        onControllerDisconnected()
    }

    override fun onAddMediaItems(
        mediaSession: MediaSession,
        controller: MediaSession.ControllerInfo,
        mediaItems: MutableList<MediaItem>,
    ): ListenableFuture<MutableList<MediaItem>> = scope.future {
        mediaItems.mapNotNull { resolve(it) }.toMutableList()
    }

    private suspend fun resolve(item: MediaItem): MediaItem? {
        val id = item.mediaId.toIntOrNull() ?: return null
        val prayer = prayers.get(id) ?: return null
        val extras = item.requestMetadata.extras
        return buildItem(
            prayer = prayer,
            routineId = extras?.getLong(PlaybackCommands.KEY_ROUTINE_ID, -1L) ?: -1L,
            contextLabel = extras?.getString(PlaybackCommands.KEY_CONTEXT_LABEL),
        )
    }

    override fun onCustomCommand(
        session: MediaSession,
        controller: MediaSession.ControllerInfo,
        customCommand: SessionCommand,
        args: Bundle,
    ): ListenableFuture<SessionResult> {
        val player = session.player
        when (customCommand.customAction) {
            PlaybackCommands.CYCLE_REPEAT_MODE -> player.repeatMode = repeat.start(repeat.mode.next())
            PlaybackCommands.SET_REPEAT_MODE -> {
                val mode = args.getString(PlaybackCommands.KEY_REPEAT_MODE)
                    ?.let { name -> RepeatMode.entries.firstOrNull { it.name == name } }
                    ?: RepeatMode.OFF
                player.repeatMode = repeat.start(mode)
            }
            else -> return Futures.immediateFuture(SessionResult(SessionError.ERROR_NOT_SUPPORTED))
        }
        onRepeatChanged()
        return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
    }

    companion object {
        fun buildItem(prayer: Prayer, routineId: Long, contextLabel: String?): MediaItem {
            val path = prayer.audioPath
            val local = prayer.isDownloaded && path != null && File(path).exists()
            val uri: Uri = if (local) Uri.fromFile(File(path)) else prayer.audioUrl.toUri()
            val extras = Bundle().apply {
                putInt(PlaybackCommands.KEY_PRAYER_ID, prayer.id)
                putLong(PlaybackCommands.KEY_ROUTINE_ID, routineId)
                putString(PlaybackCommands.KEY_CONTEXT_LABEL, contextLabel)
                putString(PlaybackCommands.KEY_SOURCE, (if (local) PlaybackSource.LOCAL else PlaybackSource.STREAM).name)
            }
            val metadata = MediaMetadata.Builder()
                .setTitle(prayer.titleEn.ifBlank { prayer.title })
                .setSubtitle(prayer.title)
                .setArtist("Daily Prayer")
                .setExtras(extras)
                .build()
            return MediaItem.Builder()
                .setMediaId(prayer.id.toString())
                .setUri(uri)
                .setMediaMetadata(metadata)
                .build()
        }
    }
}
