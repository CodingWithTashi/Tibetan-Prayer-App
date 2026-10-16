package com.codingwithtashi.dailyprayer.ui.player

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.codingwithtashi.dailyprayer.core.locale.AppLanguage
import com.codingwithtashi.dailyprayer.core.text.PrayerText
import com.codingwithtashi.dailyprayer.core.text.TibetanNumerals
import com.codingwithtashi.dailyprayer.core.time.DateFormats
import com.codingwithtashi.dailyprayer.data.model.Prayer
import com.codingwithtashi.dailyprayer.data.prefs.UserPreferencesRepository
import com.codingwithtashi.dailyprayer.data.repository.PrayerRepository
import com.codingwithtashi.dailyprayer.playback.PlaybackError
import com.codingwithtashi.dailyprayer.playback.PlayerControllerApi
import com.codingwithtashi.dailyprayer.playback.RepeatMode
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@Immutable
data class QueueItem(
    val index: Int,
    val id: Int,
    val en: String,
    val tib: String,
    /** "m:ss" (English digits). */
    val durLabel: String,
    val isCurrent: Boolean,
)

@Immutable
data class NowPlayingUiState(
    val prayer: Prayer? = null,
    val contextLabel: String = "",
    val verses: List<String> = emptyList(),
    /** 0-based. */
    val verseIndex: Int = 0,
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = false,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val progress: Float = 0f,
    val posLabel: String = "0:00",
    val durLabel: String = "0:00",
    val speed: Float = 1f,
    val repeatMode: RepeatMode = RepeatMode.OFF,
    val repeatsLeft: Int = 0,
    val isSaved: Boolean = false,
    val isDownloaded: Boolean = false,
    val queue: List<QueueItem> = emptyList(),
    val showQueue: Boolean = false,
    val error: PlaybackError? = null,
    val language: AppLanguage = AppLanguage.EN,
) {
    val speedLabel: String get() = speedLabel(speed)
    val repeatLabel: String? get() = repeatLabel(repeatMode)
    val verseCount: Int get() = verses.size
    fun count(n: Int): String = TibetanNumerals.formatCount(n, language)
}

/** "0.8×", "1×", "1.25×". */
fun speedLabel(speed: Float): String {
    val text = when {
        kotlin.math.abs(speed - 1f) < 0.01f -> "1"
        kotlin.math.abs(speed - 0.8f) < 0.01f -> "0.8"
        kotlin.math.abs(speed - 1.25f) < 0.01f -> "1.25"
        else -> speed.toString().trimEnd('0').trimEnd('.')
    }
    return "$text×"
}

/** Suffix after the repeat icon; null for Off (the screen shows the localized "Off"). */
fun repeatLabel(mode: RepeatMode): String? = when (mode) {
    RepeatMode.OFF -> null
    RepeatMode.THREE -> "×3"
    RepeatMode.SEVEN -> "×7"
    RepeatMode.INFINITE -> "∞"
}

sealed interface NowPlayingEvent {
    data object Close : NowPlayingEvent
    data object OpenQueue : NowPlayingEvent
    data object CloseQueue : NowPlayingEvent
    data object ToggleSave : NowPlayingEvent
    data object PlayPause : NowPlayingEvent
    data object Prev : NowPlayingEvent
    data object Next : NowPlayingEvent
    data class Seek(val fraction: Float) : NowPlayingEvent
    data class SeekToVerse(val index: Int) : NowPlayingEvent
    data object CycleSpeed : NowPlayingEvent
    data object CycleRepeat : NowPlayingEvent
    data class PlayQueueItem(val index: Int) : NowPlayingEvent
}

sealed interface NowPlayingEffect {
    data class Saved(val saved: Boolean, val undo: () -> Unit) : NowPlayingEffect
    data class Error(val error: PlaybackError) : NowPlayingEffect

    /** Playback was cleared elsewhere (notification, mini player): leave the screen. */
    data object Close : NowPlayingEffect
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class NowPlayingViewModel @Inject constructor(
    private val prayers: PrayerRepository,
    private val player: PlayerControllerApi,
    prefs: UserPreferencesRepository,
) : ViewModel() {

    private val showQueue = MutableStateFlow(false)
    private val textCache = HashMap<Int, PrayerText>()
    private var currentText: PrayerText? = null
    private var hadMedia = false

    private val effectChannel = Channel<NowPlayingEffect>(Channel.BUFFERED)
    val effects: Flow<NowPlayingEffect> = effectChannel.receiveAsFlow()

    init {
        viewModelScope.launch { player.errors.collect { effectChannel.trySend(NowPlayingEffect.Error(it)) } }
        viewModelScope.launch {
            player.state.map { it.hasMedia }.distinctUntilChanged().collect { has ->
                if (hadMedia && !has) effectChannel.trySend(NowPlayingEffect.Close)
                if (has) hadMedia = true
            }
        }
    }

    private val currentId: Flow<Int?> = player.state.map { it.currentPrayerId }.distinctUntilChanged()

    private val prayerFlow: Flow<Prayer?> = currentId.flatMapLatest { id -> if (id == null) flowOf(null) else prayers.observe(id) }

    private val textFlow: Flow<PrayerText?> = currentId.mapLatest { id -> id?.let { textOf(it) } }

    private val queueFlow: Flow<List<QueueRow>> = player.state.map { it.queue }.distinctUntilChanged().mapLatest { ids ->
        val byId = prayers.getByIds(ids.filter { it > 0 }).associateBy { it.id }
        ids.mapIndexedNotNull { i, id ->
            val p = byId[id] ?: return@mapIndexedNotNull null
            QueueRow(i, p, p.audioDurationMs ?: (textOf(id).estDurationSec * 1000L))
        }
    }

    private class QueueRow(val index: Int, val prayer: Prayer, val durationMs: Long)

    val state: StateFlow<NowPlayingUiState> = combine(
        player.state, prayerFlow, textFlow, queueFlow, combine(showQueue, prefs.prefs.map { it.appLanguage }) { q, l -> q to l },
    ) { s, prayer, text, queue, (queueOpen, language) ->
        currentText = text
        val verses = text?.verses.orEmpty()
        val estimate = (text?.estDurationSec ?: 0) * 1000L
        val duration = s.durationMs.takeIf { it > 0 } ?: prayer?.audioDurationMs ?: estimate
        NowPlayingUiState(
            prayer = prayer,
            contextLabel = s.contextLabel.orEmpty(),
            verses = verses,
            verseIndex = if (text == null) 0 else player.currentVerseIndex(text, verses.size, estimate),
            isPlaying = s.isPlaying,
            isBuffering = s.isBuffering,
            positionMs = s.positionMs,
            durationMs = duration,
            progress = if (duration > 0) (s.positionMs.toFloat() / duration).coerceIn(0f, 1f) else 0f,
            posLabel = DateFormats.mmss(s.positionMs),
            durLabel = DateFormats.mmss(duration),
            speed = s.speed,
            repeatMode = s.repeatMode,
            repeatsLeft = s.repeatsLeft,
            isSaved = prayer?.isSaved == true,
            isDownloaded = prayer?.isDownloaded == true || s.source == com.codingwithtashi.dailyprayer.playback.PlaybackSource.LOCAL,
            queue = queue.map { QueueItem(it.index, it.prayer.id, it.prayer.titleEn, it.prayer.title, DateFormats.mmss(it.durationMs), it.index == s.currentIndex) },
            showQueue = queueOpen,
            error = s.error,
            language = language,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), NowPlayingUiState())

    fun onEvent(event: NowPlayingEvent) {
        when (event) {
            NowPlayingEvent.Close -> Unit // handled by the screen (navigation)
            NowPlayingEvent.OpenQueue -> showQueue.value = true
            NowPlayingEvent.CloseQueue -> showQueue.value = false
            NowPlayingEvent.ToggleSave -> toggleSave()
            NowPlayingEvent.PlayPause -> player.togglePlayPause()
            NowPlayingEvent.Prev -> player.previous()
            NowPlayingEvent.Next -> player.next()
            is NowPlayingEvent.Seek -> player.seekToFraction(event.fraction)
            is NowPlayingEvent.SeekToVerse -> seekToVerse(event.index)
            NowPlayingEvent.CycleSpeed -> player.cycleSpeed()
            NowPlayingEvent.CycleRepeat -> player.cycleRepeat()
            is NowPlayingEvent.PlayQueueItem -> {
                player.skipToQueueIndex(event.index)
                showQueue.value = false
            }
        }
    }

    private fun seekToVerse(index: Int) {
        val text = currentText ?: return
        val n = text.verses.size
        if (index !in 0 until n) return
        val ts = text.verseStartMs
        if (ts != null && index < ts.size) {
            player.seekTo(ts[index])
            return
        }
        val duration = state.value.durationMs
        if (duration <= 0) return
        player.seekTo(((index.toDouble() / n) * duration).toLong() + VERSE_SEEK_NUDGE_MS)
    }

    private fun toggleSave() {
        val prayer = state.value.prayer ?: return
        val old = prayer.isSaved
        viewModelScope.launch {
            prayers.setSaved(prayer.id, !old)
            effectChannel.send(NowPlayingEffect.Saved(!old) { viewModelScope.launch { prayers.setSaved(prayer.id, old) } })
        }
    }

    private suspend fun textOf(id: Int): PrayerText = textCache.getOrPut(id) { prayers.text(id) }

    private companion object {
        /** Lands inside the verse, past rounding at its boundary. */
        const val VERSE_SEEK_NUDGE_MS = 500L
    }
}
