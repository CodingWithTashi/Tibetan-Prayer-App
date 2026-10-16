package com.codingwithtashi.dailyprayer.ui.main

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import com.codingwithtashi.dailyprayer.data.repository.NotificationRepository
import com.codingwithtashi.dailyprayer.navigation.ListenFilter
import com.codingwithtashi.dailyprayer.navigation.MainTab
import com.codingwithtashi.dailyprayer.core.text.PrayerText
import com.codingwithtashi.dailyprayer.data.model.Prayer
import com.codingwithtashi.dailyprayer.data.repository.PrayerRepository
import com.codingwithtashi.dailyprayer.playback.PlaybackError
import com.codingwithtashi.dailyprayer.playback.PlayerControllerApi
import androidx.compose.runtime.Immutable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.mapLatest
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/** Raw mini-player data; the scaffold formats "Verse i / n" with the app's digits. */
@Immutable
data class MiniPlayerModel(
    val syllable: String,
    val title: String,
    /** 0-based. */
    val verseIndex: Int,
    val verseCount: Int,
    val isPlaying: Boolean,
    val progress: Float,
)

/** Activity-scoped so Settings and deep links can switch tabs. */
@HiltViewModel
class MainViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    notifications: NotificationRepository,
    private val prayers: PrayerRepository,
    private val player: PlayerControllerApi,
) : ViewModel() {

    private val prayerCache = HashMap<Int, Pair<Prayer, PrayerText>>()

    val selectedTab: StateFlow<MainTab> = savedStateHandle.getStateFlow(KEY_TAB, MainTab.TODAY)
    val listenFilter: StateFlow<ListenFilter> = savedStateHandle.getStateFlow(KEY_FILTER, ListenFilter.ALL)

    @OptIn(ExperimentalCoroutinesApi::class)
    val miniPlayer: StateFlow<MiniPlayerModel?> = player.state.mapLatest { s ->
        val id = s.currentPrayerId ?: return@mapLatest null
        val (prayer, text) = prayerCache[id] ?: run {
            val p = prayers.get(id) ?: return@mapLatest null
            (p to prayers.text(id)).also { prayerCache[id] = it }
        }
        val verses = text.verses.size
        MiniPlayerModel(
            syllable = prayer.syllable,
            title = prayer.titleEn,
            verseIndex = player.currentVerseIndex(text, verses, text.estDurationSec * 1000L),
            verseCount = verses,
            isPlaying = s.isPlaying,
            progress = if (s.durationMs > 0) (s.positionMs.toFloat() / s.durationMs).coerceIn(0f, 1f) else 0f,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** Playback failures raised while a Main tab is showing (the scaffold turns them into toasts). */
    val playerErrors: Flow<PlaybackError> = player.errors

    val unreadNotifications: StateFlow<Boolean> =
        notifications.unreadCount().map { it > 0 }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    fun selectTab(tab: MainTab) {
        savedStateHandle[KEY_TAB] = tab
    }

    fun setListenFilter(filter: ListenFilter) {
        savedStateHandle[KEY_FILTER] = filter
    }

    fun showDownloads() {
        savedStateHandle[KEY_TAB] = MainTab.LISTEN
        savedStateHandle[KEY_FILTER] = ListenFilter.DOWNLOADED
    }

    fun onMiniPlayerPlayPause() = player.togglePlayPause()
    fun onMiniPlayerClose() = player.stopAndClear()

    private companion object {
        const val KEY_TAB = "selected_tab"
        const val KEY_FILTER = "listen_filter"
    }
}
