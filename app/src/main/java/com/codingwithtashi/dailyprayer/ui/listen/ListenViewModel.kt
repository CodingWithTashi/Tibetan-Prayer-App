package com.codingwithtashi.dailyprayer.ui.listen

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.codingwithtashi.dailyprayer.core.locale.AppLanguage
import com.codingwithtashi.dailyprayer.core.text.TibetanNumerals
import com.codingwithtashi.dailyprayer.core.time.DateFormats
import com.codingwithtashi.dailyprayer.data.model.DownloadError
import com.codingwithtashi.dailyprayer.data.model.DownloadState
import com.codingwithtashi.dailyprayer.data.model.Prayer
import com.codingwithtashi.dailyprayer.data.prefs.UserPreferencesRepository
import com.codingwithtashi.dailyprayer.data.repository.DownloadRepository
import com.codingwithtashi.dailyprayer.data.repository.PrayerRepository
import com.codingwithtashi.dailyprayer.navigation.ListenFilter
import com.codingwithtashi.dailyprayer.playback.PlayerControllerApi
import com.codingwithtashi.dailyprayer.ui.components.DownloadUiState
import com.codingwithtashi.dailyprayer.ui.theme.CategoryKey
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
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Which word follows the duration in a row's caption; the screen resolves the string. */
enum class MetaKind { DOWNLOADED, DOWNLOADING, TIBETAN }

data class ListenItem(
    val id: Int,
    val syllable: String,
    val category: CategoryKey,
    val en: String,
    val tib: String,
    /** "m:ss" (English digits). */
    val durationLabel: String,
    val metaKind: MetaKind,
    /** Localised "42%" while downloading, else null. */
    val percentLabel: String?,
    val download: DownloadUiState,
    val isCurrent: Boolean,
    val isPlaying: Boolean,
    val hasAudio: Boolean = true,
)

data class ListenUiState(
    val filter: ListenFilter = ListenFilter.ALL,
    val items: List<ListenItem> = emptyList(),
    val downloadedCount: Int = 0,
    val storageLabel: String? = null,
    val language: AppLanguage = AppLanguage.EN,
) {
    val downloadedCountText: String get() = TibetanNumerals.formatCount(downloadedCount, language)
}

sealed interface ListenEvent {
    data class PlayPause(val id: Int, val contextLabel: String) : ListenEvent
    data class DownloadToggle(val id: Int) : ListenEvent

    /** The "Removed download" toast's Undo was tapped. */
    data class UndoRemove(val id: Int) : ListenEvent

    /** The toast closed: delete the file unless it was undone. */
    data class CommitRemove(val id: Int) : ListenEvent
}

enum class ListenToast { OFFLINE_CANNOT_STREAM, DOWNLOAD_FAILED }

sealed interface ListenEffect {
    data class Toast(val toast: ListenToast) : ListenEffect

    /** Show "Removed download" with Undo, then send [ListenEvent.CommitRemove] when it closes. */
    data class ConfirmRemoveDownload(val id: Int) : ListenEffect
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ListenViewModel @Inject constructor(
    private val prayers: PrayerRepository,
    private val downloads: DownloadRepository,
    private val player: PlayerControllerApi,
    prefs: UserPreferencesRepository,
) : ViewModel() {

    private val filter = MutableStateFlow(ListenFilter.ALL)
    private val pendingRemoval = MutableStateFlow<Set<Int>>(emptySet())
    private val estimates = HashMap<Int, Long>()
    private var lastStates: Map<Int, DownloadState>? = null

    private val effectChannel = Channel<ListenEffect>(Channel.BUFFERED)
    val effects: Flow<ListenEffect> = effectChannel.receiveAsFlow()

    fun setFilter(value: ListenFilter) {
        filter.value = value
    }

    private val library: Flow<List<Pair<Prayer, Long>>> = prayers.observeWithAudio().mapLatest { list ->
        list.map { p -> p to (p.audioDurationMs ?: estimateMs(p.id)) }
    }

    private val downloadStates: Flow<Map<Int, DownloadState>> = prayers.observeWithAudio()
        .map { list -> list.map { it.id } }
        .distinctUntilChanged()
        .flatMapLatest { ids ->
            if (ids.isEmpty()) {
                flowOf(emptyMap())
            } else {
                combine(ids.map { id -> downloads.state(id) }) { arr -> ids.zip(arr.toList()).toMap() }
            }
        }
        .onEach(::reportFailures)

    private val playing: Flow<Pair<Int?, Boolean>> =
        player.state.map { it.currentPrayerId to it.isPlaying }.distinctUntilChanged()

    private val rows: Flow<List<ListenItem>> = combine(
        library, downloadStates, playing, prefs.prefs.map { it.appLanguage }.distinctUntilChanged(), pendingRemoval,
    ) { list, states, (currentId, isPlaying), language, pending ->
        list.map { (p, durationMs) ->
            val raw = states[p.id] ?: if (p.isDownloaded) DownloadState.Downloaded else DownloadState.NotDownloaded
            val effective = if (p.id in pending) DownloadState.NotDownloaded else raw
            val kind = when (effective) {
                DownloadState.Downloaded -> MetaKind.DOWNLOADED
                is DownloadState.Downloading -> MetaKind.DOWNLOADING
                else -> MetaKind.TIBETAN
            }
            ListenItem(
                id = p.id,
                syllable = p.syllable,
                category = CategoryKey.valueOf(p.category.name),
                en = p.titleEn,
                tib = p.title,
                durationLabel = DateFormats.mmss(durationMs),
                metaKind = kind,
                percentLabel = (effective as? DownloadState.Downloading)
                    ?.let { TibetanNumerals.formatPercent(it.progress, language) },
                download = when (effective) {
                    DownloadState.Downloaded -> DownloadUiState.Downloaded
                    is DownloadState.Downloading -> DownloadUiState.Downloading(effective.progress)
                    else -> DownloadUiState.NotDownloaded
                },
                isCurrent = currentId == p.id,
                isPlaying = currentId == p.id && isPlaying,
            )
        }
    }

    val state: StateFlow<ListenUiState> = combine(
        rows, filter, downloads.downloadedCount(), downloads.totalBytes(), prefs.prefs.map { it.appLanguage },
    ) { items, f, count, bytes, language ->
        ListenUiState(
            filter = f,
            items = if (f == ListenFilter.DOWNLOADED) items.filter { it.download == DownloadUiState.Downloaded } else items,
            downloadedCount = count,
            storageLabel = if (count > 0) downloads.storageLabel(bytes) else null,
            language = language,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ListenUiState())

    fun onEvent(event: ListenEvent) {
        when (event) {
            is ListenEvent.PlayPause -> player.playOrToggle(event.id, event.contextLabel)
            is ListenEvent.DownloadToggle -> toggleDownload(event.id)
            is ListenEvent.UndoRemove -> pendingRemoval.update { it - event.id }
            is ListenEvent.CommitRemove -> commitRemove(event.id)
        }
    }

    private fun toggleDownload(id: Int) {
        val item = state.value.items.firstOrNull { it.id == id } ?: return
        when (item.download) {
            DownloadUiState.NotDownloaded -> downloads.download(id)
            is DownloadUiState.Downloading -> downloads.cancel(id)
            DownloadUiState.Downloaded -> {
                pendingRemoval.update { it + id }
                effectChannel.trySend(ListenEffect.ConfirmRemoveDownload(id))
            }
        }
    }

    private fun commitRemove(id: Int) {
        if (id !in pendingRemoval.value) return
        viewModelScope.launch {
            downloads.delete(id)
            pendingRemoval.update { it - id }
        }
    }

    private fun reportFailures(states: Map<Int, DownloadState>) {
        val previous = lastStates
        lastStates = states
        if (previous == null) return // seed: ignore failures from before this screen existed
        for ((id, s) in states) {
            if (s !is DownloadState.Failed || previous[id] is DownloadState.Failed) continue
            val toast = when (s.error) {
                DownloadError.OFFLINE -> ListenToast.OFFLINE_CANNOT_STREAM
                DownloadError.CANCELLED -> null
                else -> ListenToast.DOWNLOAD_FAILED
            }
            if (toast != null) effectChannel.trySend(ListenEffect.Toast(toast))
        }
    }

    private suspend fun estimateMs(id: Int): Long =
        estimates.getOrPut(id) { prayers.text(id).estDurationSec * 1000L }
}
