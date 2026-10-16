package com.codingwithtashi.dailyprayer.ui.library

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.codingwithtashi.dailyprayer.data.model.Prayer
import com.codingwithtashi.dailyprayer.data.model.PrayerCategory
import com.codingwithtashi.dailyprayer.data.repository.PrayerRepository
import com.codingwithtashi.dailyprayer.ui.theme.CategoryKey
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

/** One Library list row. */
data class LibraryItem(
    val id: Int,
    val syllable: String,
    val category: CategoryKey,
    val titleEn: String,
    val title: String,
    val hasAudio: Boolean,
)

data class LibraryUiState(
    val query: String = "",
    val category: PrayerCategory? = null,
    val items: List<LibraryItem> = emptyList(),
    /** Chip list; null is "All". */
    val categories: List<PrayerCategory?> = DefaultChips,
)

/** User actions on the Library tab. */
sealed interface LibraryEvent {
    data class SetQuery(val query: String) : LibraryEvent
    data class SetCategory(val category: PrayerCategory?) : LibraryEvent
}

private val DefaultChips: List<PrayerCategory?> = listOf(
    null, PrayerCategory.DAILY, PrayerCategory.SUTRA, PrayerCategory.LIFE, PrayerCategory.PRAISE, PrayerCategory.ASPIRE,
)

@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    repository: PrayerRepository,
) : ViewModel() {

    val state: StateFlow<LibraryUiState> = combine(
        savedStateHandle.getStateFlow(KEY_QUERY, ""),
        savedStateHandle.getStateFlow<String?>(KEY_CATEGORY, null),
        repository.observeAll(),
    ) { query, categoryKey, prayers ->
        val category = categoryKey?.let { PrayerCategory.fromKey(it) }
        LibraryUiState(
            query = query,
            category = category,
            items = prayers.filter { matches(it, category, query) }.map { it.toItem() },
            categories = if (prayers.any { it.category == PrayerCategory.OTHER }) DefaultChips + PrayerCategory.OTHER else DefaultChips,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LibraryUiState())

    fun onEvent(event: LibraryEvent) {
        when (event) {
            is LibraryEvent.SetQuery -> savedStateHandle[KEY_QUERY] = event.query
            is LibraryEvent.SetCategory -> savedStateHandle[KEY_CATEGORY] = event.category?.key
        }
    }

    private fun matches(p: Prayer, cat: PrayerCategory?, q: String): Boolean =
        (cat == null || p.category == cat) &&
            (q.isBlank() || p.titleEn.contains(q, ignoreCase = true) || p.title.contains(q.trim()))

    private fun Prayer.toItem() = LibraryItem(
        id = id,
        syllable = syllable,
        category = CategoryKey.valueOf(category.name),
        titleEn = titleEn,
        title = title,
        hasAudio = hasAudio,
    )

    private companion object {
        const val KEY_QUERY = "library_query"
        const val KEY_CATEGORY = "library_category"
    }
}
