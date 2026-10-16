package com.codingwithtashi.dailyprayer.ui.main

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.res.stringResource
import com.codingwithtashi.dailyprayer.core.text.TibetanNumerals
import com.codingwithtashi.dailyprayer.playback.PlaybackError
import com.codingwithtashi.dailyprayer.ui.components.LocalToaster
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.codingwithtashi.dailyprayer.R
import com.codingwithtashi.dailyprayer.navigation.MainTab
import com.codingwithtashi.dailyprayer.ui.components.MiniPlayer
import com.codingwithtashi.dailyprayer.ui.components.MiniPlayerUi
import com.codingwithtashi.dailyprayer.ui.components.PrayerBottomBar
import com.codingwithtashi.dailyprayer.ui.components.PrayerNavRail
import com.codingwithtashi.dailyprayer.ui.components.TabSpec
import com.codingwithtashi.dailyprayer.ui.components.contentWidth
import com.codingwithtashi.dailyprayer.ui.components.rememberIsCompactWidth
import com.codingwithtashi.dailyprayer.ui.library.LibraryTab
import com.codingwithtashi.dailyprayer.ui.listen.ListenTab
import com.codingwithtashi.dailyprayer.ui.saved.SavedTab
import com.codingwithtashi.dailyprayer.ui.theme.PrayerTheme
import com.codingwithtashi.dailyprayer.ui.theme.motionDuration
import com.codingwithtashi.dailyprayer.ui.today.TodayTab
import kotlinx.coroutines.launch

private val Tabs = listOf(
    TabSpec(R.string.tab_today, R.drawable.ic_sunrise),
    TabSpec(R.string.tab_library, R.drawable.ic_library),
    TabSpec(R.string.tab_saved, R.drawable.ic_heart),
    TabSpec(R.string.tab_listen, R.drawable.ic_headphones),
)

@Composable
fun MainScaffold(
    onOpenSettings: () -> Unit,
    onOpenNotifications: () -> Unit,
    onOpenRoutine: (Long) -> Unit,
    onNewRoutine: () -> Unit,
    onOpenReader: (prayerId: Int, routineId: Long?) -> Unit,
    onOpenPlayer: () -> Unit,
    viewModel: MainViewModel = hiltViewModel(),
) {
    val tab by viewModel.selectedTab.collectAsStateWithLifecycle()
    val filter by viewModel.listenFilter.collectAsStateWithLifecycle()
    val miniModel by viewModel.miniPlayer.collectAsStateWithLifecycle()
    val language = PrayerTheme.language
    val miniPlayer = miniModel?.let { m ->
        MiniPlayerUi(
            syllable = m.syllable,
            title = m.title,
            subtitle = stringResource(
                R.string.verse_of,
                TibetanNumerals.formatCount(m.verseIndex + 1, language),
                TibetanNumerals.formatCount(m.verseCount, language),
            ),
            isPlaying = m.isPlaying,
            progress = m.progress,
        )
    }
    val toaster = LocalToaster.current
    val offlineMessage = stringResource(R.string.offline_cannot_stream)
    val errorMessage = stringResource(R.string.playback_error)
    LaunchedEffect(viewModel) {
        viewModel.playerErrors.collect { error ->
            launch { toaster.show(if (error == PlaybackError.OFFLINE) offlineMessage else errorMessage) }
        }
    }
    MainScaffoldContent(
        tab = tab,
        miniPlayer = miniPlayer,
        onSelectTab = viewModel::selectTab,
        onMiniPlayerOpen = onOpenPlayer,
        onMiniPlayerPlayPause = viewModel::onMiniPlayerPlayPause,
        onMiniPlayerClose = viewModel::onMiniPlayerClose,
        tabContent = { current, listState, modifier ->
            when (current) {
                MainTab.TODAY -> TodayTab(
                    listState = listState,
                    onOpenSettings = onOpenSettings,
                    onOpenNotifications = onOpenNotifications,
                    onOpenRoutine = onOpenRoutine,
                    onNewRoutine = onNewRoutine,
                    onOpenReader = onOpenReader,
                    onSeeAllSaved = { viewModel.selectTab(MainTab.SAVED) },
                    onOpenPlayer = onOpenPlayer,
                    modifier = modifier,
                )
                MainTab.LIBRARY -> LibraryTab(listState, { onOpenReader(it, null) }, modifier)
                MainTab.SAVED -> SavedTab(listState, { onOpenReader(it, null) }, { viewModel.selectTab(MainTab.LIBRARY) }, modifier)
                MainTab.LISTEN -> ListenTab(listState, filter, viewModel::setListenFilter, onOpenPlayer, modifier)
            }
        },
    )
}

/**
 * Stateless scaffold: bottom bar on compact width, navigation rail from 600dp. [tabContent] draws
 * the selected tab (the real, ViewModel-backed tabs at runtime; stateless content in previews).
 */
@Composable
internal fun MainScaffoldContent(
    tab: MainTab,
    miniPlayer: MiniPlayerUi?,
    onSelectTab: (MainTab) -> Unit,
    onMiniPlayerOpen: () -> Unit,
    onMiniPlayerPlayPause: () -> Unit,
    onMiniPlayerClose: () -> Unit,
    tabContent: @Composable (tab: MainTab, listState: LazyListState, modifier: Modifier) -> Unit,
) {
    val compact = rememberIsCompactWidth()
    val scope = rememberCoroutineScope()
    val listStates: Map<MainTab, LazyListState> = MainTab.entries.associateWith { t ->
        key(t) { rememberSaveable(t.name, saver = LazyListState.Saver) { LazyListState() } }
    }
    val onSelectIndex: (Int) -> Unit = { index ->
        val target = MainTab.entries[index]
        if (target == tab) {
            scope.launch { listStates.getValue(tab).animateScrollToItem(0) }
        } else {
            onSelectTab(target)
        }
    }
    val fade = tween<Float>(motionDuration(200))

    val content: @Composable () -> Unit = {
        Crossfade(targetState = tab, animationSpec = fade, label = "tab") { current ->
            val state = listStates.getValue(current)
            tabContent(current, state, Modifier.fillMaxSize())
        }
    }
    val mini: @Composable () -> Unit = {
        AnimatedVisibility(visible = miniPlayer != null) {
            miniPlayer?.let { MiniPlayer(it, onMiniPlayerOpen, onMiniPlayerPlayPause, onMiniPlayerClose, Modifier.then(if (compact) Modifier else Modifier.contentWidth())) }
        }
    }

    Box(Modifier.fillMaxSize().background(PrayerTheme.colors.background)) {
        if (compact) {
            Column(Modifier.fillMaxSize()) {
                Box(Modifier.weight(1f).fillMaxWidth()) { content() }
                mini()
                PrayerBottomBar(Tabs, tab.ordinal, onSelectIndex)
            }
        } else {
            Row(Modifier.fillMaxSize()) {
                PrayerNavRail(Tabs, tab.ordinal, onSelectIndex)
                Column(Modifier.weight(1f).fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(Modifier.weight(1f).contentWidth()) { content() }
                    mini()
                }
            }
        }
    }
}
