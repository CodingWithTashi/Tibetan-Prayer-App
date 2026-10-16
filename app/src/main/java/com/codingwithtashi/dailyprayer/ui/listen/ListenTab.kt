package com.codingwithtashi.dailyprayer.ui.listen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.codingwithtashi.dailyprayer.R
import com.codingwithtashi.dailyprayer.navigation.ListenFilter
import com.codingwithtashi.dailyprayer.ui.components.AppText
import com.codingwithtashi.dailyprayer.ui.components.FilterChipPill
import com.codingwithtashi.dailyprayer.ui.components.ListenRow
import com.codingwithtashi.dailyprayer.ui.components.LocalToaster
import com.codingwithtashi.dailyprayer.ui.components.ScreenTitle
import com.codingwithtashi.dailyprayer.ui.components.contentWidth
import com.codingwithtashi.dailyprayer.ui.theme.PrayerTheme
import kotlinx.coroutines.launch

/** Listen tab: every prayer with audio, filterable to downloads; play, download and remove inline. */
@Composable
fun ListenTab(
    listState: LazyListState,
    filter: ListenFilter,
    onFilterChange: (ListenFilter) -> Unit,
    @Suppress("UNUSED_PARAMETER") onOpenPlayer: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ListenViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val toaster = LocalToaster.current
    val offline = stringResource(R.string.offline_cannot_stream)
    val failed = stringResource(R.string.download_failed)
    val removed = stringResource(R.string.removed_download)
    val undo = stringResource(R.string.undo)
    LaunchedEffect(filter) { viewModel.onEvent(ListenEvent.SetFilter(filter)) }
    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is ListenEffect.Toast -> launch {
                    toaster.show(if (effect.toast == ListenToast.OFFLINE_CANNOT_STREAM) offline else failed)
                }
                is ListenEffect.ConfirmRemoveDownload -> launch {
                    var undone = false
                    try {
                        toaster.show(removed, undo) {
                            undone = true
                            viewModel.onEvent(ListenEvent.UndoRemove(effect.id))
                        }
                    } finally {
                        // Closed (or the tab left composition) without Undo: delete the file.
                        if (!undone) viewModel.onEvent(ListenEvent.CommitRemove(effect.id))
                    }
                }
            }
        }
    }
    ListenContent(
        state = state,
        listState = listState,
        contextLabel = stringResource(R.string.tab_listen),
        onFilterChange = onFilterChange,
        onEvent = viewModel::onEvent,
        modifier = modifier,
    )
}

@Composable
internal fun ListenContent(
    state: ListenUiState,
    listState: LazyListState,
    contextLabel: String,
    onFilterChange: (ListenFilter) -> Unit,
    onEvent: (ListenEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = PrayerTheme.colors
    val t = PrayerTheme.type
    val downloadedWord = stringResource(R.string.downloaded)
    Box(modifier.fillMaxSize().background(c.background), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            modifier = Modifier.contentWidth().statusBarsPadding(),
            state = listState,
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 24.dp),
        ) {
            item { ScreenTitle(stringResource(R.string.tab_listen)) }
            item {
                Row(
                    Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    FilterChipPill(stringResource(R.string.all), state.filter == ListenFilter.ALL, { onFilterChange(ListenFilter.ALL) })
                    FilterChipPill(
                        stringResource(R.string.downloaded_count, state.downloadedCountText),
                        state.filter == ListenFilter.DOWNLOADED,
                        { onFilterChange(ListenFilter.DOWNLOADED) },
                    )
                    AppText(
                        state.storageLabel.orEmpty(),
                        style = t.bodySmallMedium,
                        color = c.textMuted,
                        textAlign = TextAlign.End,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
            if (state.items.isEmpty() && state.filter == ListenFilter.DOWNLOADED) {
                item {
                    AppText(
                        stringResource(R.string.no_downloads),
                        style = t.body,
                        color = c.textMuted,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 40.dp, horizontal = 12.dp),
                    )
                }
            }
            items(state.items, key = { it.id }) { item ->
                val detail = when (item.metaKind) {
                    MetaKind.DOWNLOADED -> downloadedWord
                    MetaKind.DOWNLOADING -> item.percentLabel.orEmpty()
                    MetaKind.TIBETAN -> item.tib
                }
                ListenRow(
                    syllable = item.syllable,
                    category = item.category,
                    en = item.en,
                    meta = "${item.durationLabel} · $detail",
                    downloadState = item.download,
                    isCurrent = item.isCurrent,
                    isPlaying = item.isPlaying,
                    onPlay = { onEvent(ListenEvent.PlayPause(item.id, contextLabel)) },
                    onDownloadToggle = { onEvent(ListenEvent.DownloadToggle(item.id)) },
                    showDivider = item.id != state.items.last().id,
                )
            }
        }
    }
}
