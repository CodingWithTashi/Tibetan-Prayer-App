package com.codingwithtashi.dailyprayer.ui.saved

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.codingwithtashi.dailyprayer.data.model.Prayer
import com.codingwithtashi.dailyprayer.data.repository.PrayerRepository
import com.codingwithtashi.dailyprayer.playback.PlayerControllerApi
import com.codingwithtashi.dailyprayer.ui.theme.CategoryKey
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SavedItem(
    val id: Int,
    val syllable: String,
    val category: CategoryKey,
    /** `savedLabel ?: titleEn`. */
    val label: String,
    val tib: String,
    val hasAudio: Boolean,
    val isPlaying: Boolean = false,
    /** Text shown in the rename field (empty = placeholder shows the original name). */
    val draftLabel: String,
    val titleEn: String,
)

data class SavedUiState(
    val items: List<SavedItem> = emptyList(),
    val editing: Boolean = false,
)

sealed interface SavedEvent {
    /** Show the "removed" toast; Undo calls [SavedViewModel.undoRemove]. */
    data class Removed(val id: Int, val previousLabel: String?) : SavedEvent
}

@HiltViewModel
class SavedViewModel @Inject constructor(
    private val repository: PrayerRepository,
    private val player: PlayerControllerApi,
) : ViewModel() {

    private val editing = MutableStateFlow(false)
    private val drafts = MutableStateFlow<Map<Int, String>>(emptyMap())
    private val saveJobs = mutableMapOf<Int, Job>()
    private val eventChannel = Channel<SavedEvent>(Channel.BUFFERED)
    val events: Flow<SavedEvent> = eventChannel.receiveAsFlow()

    private val playing = player.state.map { it.currentPrayerId to it.isPlaying }.distinctUntilChanged()

    val state: StateFlow<SavedUiState> = combine(repository.observeSaved(), editing, drafts, playing) { prayers, edit, draft, (currentId, isPlaying) ->
        SavedUiState(
            prayers.map { it.toItem(draft[it.id], isPlaying = isPlaying && currentId == it.id) },
            edit && prayers.isNotEmpty(),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SavedUiState())

    fun toggleEdit() {
        if (editing.value) flushPending()
        editing.value = !editing.value
    }

    /** Plays `[id]` (or toggles it when already current); [contextLabel] is the "Saved" tab name. */
    fun playPause(id: Int, contextLabel: String) {
        player.playOrToggle(id, contextLabel)
    }

    fun setLabel(id: Int, text: String) {
        val clamped = text.take(MAX_LABEL)
        drafts.update { it + (id to clamped) }
        saveJobs[id]?.cancel()
        saveJobs[id] = viewModelScope.launch {
            delay(DEBOUNCE_MS)
            persist(id)
        }
    }

    fun remove(id: Int) {
        viewModelScope.launch {
            // Any unsaved draft is dropped with the row; Undo restores the persisted label.
            saveJobs.remove(id)?.cancel()
            drafts.update { it - id }
            val prayer = repository.get(id) ?: return@launch
            repository.setSaved(id, false)
            eventChannel.send(SavedEvent.Removed(id, prayer.savedLabel))
        }
    }

    fun undoRemove(id: Int, previousLabel: String?) {
        viewModelScope.launch {
            repository.setSaved(id, true)
            repository.setSavedLabel(id, previousLabel)
        }
    }

    private fun flushPending() {
        val pending = drafts.value.keys.toList()
        pending.forEach { id ->
            saveJobs.remove(id)?.cancel()
            viewModelScope.launch { persist(id) }
        }
    }

    private suspend fun persist(id: Int) {
        val text = drafts.value[id] ?: return
        repository.setSavedLabel(id, text.trim().ifBlank { null })
        drafts.update { if (it[id] == text) it - id else it }
    }

    private fun Prayer.toItem(draft: String?, isPlaying: Boolean) = SavedItem(
        id = id,
        syllable = syllable,
        category = CategoryKey.valueOf(category.name),
        label = savedLabel ?: titleEn,
        tib = title,
        hasAudio = hasAudio,
        isPlaying = isPlaying,
        draftLabel = draft ?: savedLabel.orEmpty(),
        titleEn = titleEn,
    )

    private companion object {
        const val MAX_LABEL = 28
        const val DEBOUNCE_MS = 300L
    }
}
