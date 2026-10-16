package com.codingwithtashi.dailyprayer.ui.saved

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.codingwithtashi.dailyprayer.R
import com.codingwithtashi.dailyprayer.ui.components.AppText
import com.codingwithtashi.dailyprayer.ui.components.EmptyState
import com.codingwithtashi.dailyprayer.ui.components.LocalToaster
import com.codingwithtashi.dailyprayer.ui.components.PillButton
import com.codingwithtashi.dailyprayer.ui.components.PillVariant
import com.codingwithtashi.dailyprayer.ui.components.SavedRowEdit
import com.codingwithtashi.dailyprayer.ui.components.SavedRowView
import com.codingwithtashi.dailyprayer.ui.components.ScreenTitle
import com.codingwithtashi.dailyprayer.ui.components.contentWidth
import com.codingwithtashi.dailyprayer.ui.theme.PrayerTheme
import kotlinx.coroutines.launch

@Composable
fun SavedTab(
    listState: LazyListState,
    onOpenReader: (Int) -> Unit,
    onBrowseLibrary: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SavedViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val toaster = LocalToaster.current
    val message = stringResource(R.string.removed_from_saved)
    val undo = stringResource(R.string.undo)
    val savedLabel = stringResource(R.string.tab_saved)
    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is SavedEffect.Removed -> launch {
                    toaster.show(message, undo) { viewModel.onEvent(SavedEvent.UndoRemove(effect.id, effect.previousLabel)) }
                }
            }
        }
    }
    SavedContent(
        state = state,
        listState = listState,
        onToggleEdit = { viewModel.onEvent(SavedEvent.ToggleEdit) },
        onOpen = onOpenReader,
        onPlayPause = { viewModel.onEvent(SavedEvent.PlayPause(it, savedLabel)) },
        onSetLabel = { id, text -> viewModel.onEvent(SavedEvent.SetLabel(id, text)) },
        onRemove = { viewModel.onEvent(SavedEvent.Remove(it)) },
        onBrowseLibrary = onBrowseLibrary,
        modifier = modifier,
    )
}

@Composable
internal fun SavedContent(
    state: SavedUiState,
    listState: LazyListState,
    onToggleEdit: () -> Unit,
    onOpen: (Int) -> Unit,
    onPlayPause: (Int) -> Unit,
    onSetLabel: (Int, String) -> Unit,
    onRemove: (Int) -> Unit,
    onBrowseLibrary: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = PrayerTheme.colors
    Box(modifier.fillMaxSize().background(c.background), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            modifier = Modifier.contentWidth().statusBarsPadding().imePadding(),
            state = listState,
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 24.dp),
        ) {
            item {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    ScreenTitle(stringResource(R.string.tab_saved))
                    if (state.items.isNotEmpty()) {
                        PillButton(
                            text = stringResource(if (state.editing) R.string.done else R.string.edit),
                            onClick = onToggleEdit,
                            variant = if (state.editing) PillVariant.Filled else PillVariant.Text,
                            height = 36.dp,
                            horizontalPadding = 16.dp,
                        )
                    }
                }
            }
            if (state.items.isEmpty()) {
                item {
                    EmptyState(R.drawable.ic_heart, stringResource(R.string.saved_empty), button = stringResource(R.string.browse_library) to onBrowseLibrary)
                }
            } else {
                if (state.editing) {
                    item {
                        AppText(
                            stringResource(R.string.rename_hint),
                            style = PrayerTheme.type.body,
                            color = c.textMuted,
                            modifier = Modifier.padding(top = 4.dp, bottom = 8.dp),
                        )
                    }
                }
                items(state.items, key = { it.id }) { item ->
                    val last = item.id == state.items.last().id
                    if (state.editing) {
                        SavedRowEdit(
                            syllable = item.syllable,
                            category = item.category,
                            name = item.draftLabel,
                            onNameChange = { onSetLabel(item.id, it) },
                            namePlaceholder = item.titleEn,
                            tib = item.tib,
                            onOpen = { onOpen(item.id) },
                            onRemove = { onRemove(item.id) },
                            showDivider = !last,
                        )
                    } else {
                        SavedRowView(
                            syllable = item.syllable,
                            category = item.category,
                            label = item.label,
                            tib = item.tib,
                            hasAudio = item.hasAudio,
                            isPlaying = item.isPlaying,
                            onOpen = { onOpen(item.id) },
                            onPlayPause = { onPlayPause(item.id) },
                            showDivider = !last,
                        )
                    }
                }
            }
        }
    }
}
