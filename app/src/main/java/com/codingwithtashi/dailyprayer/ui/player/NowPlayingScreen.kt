package com.codingwithtashi.dailyprayer.ui.player

import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.DragInteraction
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.codingwithtashi.dailyprayer.R
import com.codingwithtashi.dailyprayer.playback.RepeatMode
import com.codingwithtashi.dailyprayer.ui.components.AppText
import com.codingwithtashi.dailyprayer.ui.components.IconActionButton
import com.codingwithtashi.dailyprayer.ui.components.LocalToaster
import com.codingwithtashi.dailyprayer.ui.components.toastAnchor
import com.codingwithtashi.dailyprayer.ui.components.Overline
import com.codingwithtashi.dailyprayer.ui.components.SheetHandle
import com.codingwithtashi.dailyprayer.ui.components.SmallPill
import com.codingwithtashi.dailyprayer.ui.theme.NowPlayingPalette as P
import com.codingwithtashi.dailyprayer.ui.theme.PrayerDimens
import com.codingwithtashi.dailyprayer.ui.theme.PrayerTheme
import com.codingwithtashi.dailyprayer.ui.theme.SystemBarIcons
import com.codingwithtashi.dailyprayer.ui.theme.motionDuration
import kotlin.math.max
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.codingwithtashi.dailyprayer.playback.PlaybackError

// Local fallbacks: the strings catalogue has no entries for these yet.

private const val WIDE_BREAKPOINT_DP = 840
private const val AUTO_FOLLOW_RESUME_MS = 5_000L

// Verse timing is estimated from the audio length and drifts from the chanting, so auto-scroll and the
// playing-verse highlight stay off until real per-verse timestamps exist.
private const val FOLLOW_AUDIO = false

/** Always-dark full-screen player. */
@Composable
fun NowPlayingScreen(onClose: () -> Unit, viewModel: NowPlayingViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val toaster = LocalToaster.current
    val latestClose by rememberUpdatedState(onClose)
    val added = stringResource(R.string.added_to_saved)
    val removed = stringResource(R.string.removed_from_saved)
    val undo = stringResource(R.string.undo)
    val offline = stringResource(R.string.offline_cannot_stream)
    val failed = stringResource(R.string.playback_error)
    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is NowPlayingEffect.Saved -> launch { toaster.show(if (effect.saved) added else removed, undo) { effect.undo() } }
                is NowPlayingEffect.Error -> launch { toaster.show(if (effect.error == PlaybackError.OFFLINE) offline else failed) }
                NowPlayingEffect.Close -> latestClose()
            }
        }
    }
    NowPlayingContent(
        state = state,
        onEvent = { if (it == NowPlayingEvent.Close) onClose() else viewModel.onEvent(it) },
    )
}

@Composable
fun NowPlayingContent(state: NowPlayingUiState, onEvent: (NowPlayingEvent) -> Unit) {
    SystemBarIcons(darkBackground = true)
    BackHandler { onEvent(NowPlayingEvent.Close) }
    val widthDp = with(LocalDensity.current) { LocalWindowInfo.current.containerSize.width.toDp() }
    val wide = widthDp >= WIDE_BREAKPOINT_DP.dp
    Column(Modifier.fillMaxSize().background(P.bg)) {
        PlayerTopBar(state.contextLabel, onEvent)
        if (wide) {
            Row(Modifier.weight(1f).fillMaxWidth()) {
                Column(Modifier.width(360.dp).fillMaxHeight()) {
                    PlayerHeader(state, onEvent)
                    Spacer(Modifier.weight(1f))
                    PlayerControls(state, onEvent)
                }
                VersesPane(state, onEvent, Modifier.weight(1f).fillMaxHeight().padding(top = 14.dp))
            }
        } else {
            PlayerHeader(state, onEvent)
            VersesPane(state, onEvent, Modifier.weight(1f).heightIn(min = 120.dp).fillMaxWidth().padding(top = 14.dp))
            PlayerControls(state, onEvent)
        }
    }
    if (state.showQueue) QueueSheet(state, onEvent)
}

@Composable
private fun PlayerTopBar(contextLabel: String, onEvent: (NowPlayingEvent) -> Unit) {
    Row(
        Modifier.fillMaxWidth().statusBarsPadding().heightIn(min = PrayerDimens.topBarMin).padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconActionButton(R.drawable.ic_chevron_down, stringResource(R.string.cd_close), { onEvent(NowPlayingEvent.Close) }, tint = P.text, iconSize = 24.dp)
        Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
            Overline(stringResource(R.string.playing_from), color = P.overline)
            AppText(contextLabel, style = PrayerTheme.type.bodyStrong, color = P.text, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        IconActionButton(R.drawable.ic_queue, stringResource(R.string.cd_queue), { onEvent(NowPlayingEvent.OpenQueue) }, tint = P.text, iconSize = 20.dp)
    }
}

@Composable
private fun PlayerHeader(state: NowPlayingUiState, onEvent: (NowPlayingEvent) -> Unit) {
    val prayer = state.prayer ?: return
    val t = PrayerTheme.type
    Row(
        Modifier.fillMaxWidth().padding(start = 24.dp, end = 24.dp, top = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Box(Modifier.size(84.dp).clip(RoundedCornerShape(16.dp)).background(P.artBg).padding(6.dp)) {
            Box(
                Modifier.fillMaxSize().border(1.dp, P.artInnerBorder, RoundedCornerShape(11.dp)),
                contentAlignment = Alignment.Center,
            ) { AppText(prayer.syllable, style = t.tileGlyphHero, color = P.artSyllable) }
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            AppText(prayer.titleEn, style = t.playerTitle, color = P.text)
            AppText(prayer.title, style = t.tibetanList, color = P.tib, maxLines = 1, overflow = TextOverflow.Ellipsis)
            AppText(
                stringResource(if (state.isDownloaded) R.string.downloaded else R.string.streaming),
                style = t.bodySmallMedium,
                color = if (state.isDownloaded) P.downloaded else P.time,
            )
        }
        IconActionButton(
            icon = if (state.isSaved) R.drawable.ic_heart_filled else R.drawable.ic_heart,
            contentDescription = stringResource(if (state.isSaved) R.string.cd_unsave_prayer else R.string.cd_save_prayer),
            onClick = { onEvent(NowPlayingEvent.ToggleSave) },
            tint = P.control,
            iconSize = 24.dp,
        )
    }
}

@Composable
private fun VersesPane(state: NowPlayingUiState, onEvent: (NowPlayingEvent) -> Unit, modifier: Modifier) {
    val listState = rememberLazyListState()
    if (FOLLOW_AUDIO) {
        val reduceMotion = PrayerTheme.reduceMotion
        var autoFollow by remember { mutableStateOf(true) }

        // A user drag pauses following; 5 s after it ends following resumes. Programmatic scrolls
        // never produce drag interactions, so they cannot trip this.
        LaunchedEffect(listState) {
            var resume: Job? = null
            listState.interactionSource.interactions.collect { interaction ->
                when (interaction) {
                    is DragInteraction.Start -> {
                        resume?.cancel()
                        autoFollow = false
                    }
                    is DragInteraction.Stop, is DragInteraction.Cancel -> {
                        resume?.cancel()
                        resume = launch {
                            delay(AUTO_FOLLOW_RESUME_MS)
                            autoFollow = true
                        }
                    }
                }
            }
        }
        LaunchedEffect(state.verseIndex, autoFollow, state.verses.size) {
            if (autoFollow && state.verses.isNotEmpty()) {
                val target = max(0, state.verseIndex - 1)
                if (reduceMotion) listState.scrollToItem(target) else listState.animateScrollToItem(target)
            }
        }
    }

    Box(modifier.clip(RoundedCornerShape(0.dp))) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 28.dp, end = 28.dp, top = 34.dp),
        ) {
            itemsIndexed(state.verses) { i, verse ->
                val target = when {
                    !FOLLOW_AUDIO -> P.verseFuture
                    i == state.verseIndex -> P.verseCurrent
                    i < state.verseIndex -> P.versePast
                    else -> P.verseFuture
                }
                val color by animateColorAsState(target, tween(motionDuration(400)), label = "verse")
                val description = stringResource(R.string.cd_verse, state.count(i + 1), state.count(state.verseCount)) + ". " + verse
                AppText(
                    verse,
                    style = PrayerTheme.type.tibetanVerse,
                    color = color,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            role = Role.Button,
                        ) { onEvent(NowPlayingEvent.SeekToVerse(i)) }
                        .padding(vertical = 6.dp)
                        .semantics {
                            contentDescription = description
                            selected = FOLLOW_AUDIO && i == state.verseIndex
                        },
                )
            }
            item { Spacer(Modifier.height(70.dp)) }
        }
        Box(
            Modifier.fillMaxWidth().height(40.dp).align(Alignment.TopCenter)
                .background(Brush.verticalGradient(listOf(P.bg, P.bg.copy(alpha = 0f)))),
        )
        Box(
            Modifier.fillMaxWidth().height(70.dp).align(Alignment.BottomCenter)
                .background(Brush.verticalGradient(listOf(P.bg.copy(alpha = 0f), P.bg))),
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PlayerControls(state: NowPlayingUiState, onEvent: (NowPlayingEvent) -> Unit) {
    val t = PrayerTheme.type
    val cdSeek = stringResource(R.string.cd_seek)
    val cdSpeed = stringResource(R.string.cd_speed)
    val cdRepeat = stringResource(R.string.cd_repeat)
    val offLabel = stringResource(R.string.repeat_off)
    Column(
        Modifier.fillMaxWidth().toastAnchor().navigationBarsPadding().padding(start = 28.dp, end = 28.dp, top = 6.dp, bottom = 40.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            var scrub by remember { mutableStateOf<Float?>(null) }
            val value = scrub ?: state.progress
            Slider(
                value = value,
                onValueChange = { scrub = it },
                onValueChangeFinished = {
                    scrub?.let { onEvent(NowPlayingEvent.Seek(it)) }
                    scrub = null
                },
                modifier = Modifier.fillMaxWidth().semantics {
                    contentDescription = cdSeek
                    stateDescription = "${state.posLabel} / ${state.durLabel}"
                },
                thumb = { Box(Modifier.size(14.dp).background(P.control, CircleShape)) },
                track = {
                    Box(Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)).background(P.seekTrack)) {
                        Box(Modifier.fillMaxWidth(value.coerceIn(0f, 1f)).fillMaxHeight().background(P.control))
                    }
                },
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                AppText(state.posLabel, style = t.bodySmallMedium, color = P.time)
                AppText(state.durLabel, style = t.bodySmallMedium, color = P.time)
            }
        }
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            SmallPill(
                onClick = { onEvent(NowPlayingEvent.CycleSpeed) },
                contentDescription = "$cdSpeed ${state.speedLabel}",
                borderColor = P.pillBorder,
            ) { AppText(state.speedLabel, style = t.bodySmallStrong, color = P.text, maxLines = 1) }
            IconActionButton(R.drawable.ic_skip_prev, stringResource(R.string.cd_previous), { onEvent(NowPlayingEvent.Prev) }, tint = P.text, iconSize = 30.dp)
            val playCd = stringResource(if (state.isPlaying) R.string.cd_pause else R.string.cd_play)
            Box(
                Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(P.control)
                    .semantics(mergeDescendants = true) { contentDescription = playCd }
                    .clickable(role = Role.Button) { onEvent(NowPlayingEvent.PlayPause) },
                contentAlignment = Alignment.Center,
            ) {
                if (state.isBuffering) {
                    CircularProgressIndicator(Modifier.size(28.dp), color = P.playIcon, strokeWidth = 2.5.dp)
                } else {
                    Icon(
                        painterResource(if (state.isPlaying) R.drawable.ic_pause else R.drawable.ic_play),
                        contentDescription = null,
                        tint = P.playIcon,
                        modifier = Modifier.size(28.dp),
                    )
                }
            }
            IconActionButton(R.drawable.ic_skip_next, stringResource(R.string.cd_next), { onEvent(NowPlayingEvent.Next) }, tint = P.text, iconSize = 30.dp)
            val repeating = state.repeatMode != RepeatMode.OFF
            val repeatText = state.repeatLabel ?: offLabel
            val repeatColor = if (repeating) P.playIcon else P.text
            SmallPill(
                onClick = { onEvent(NowPlayingEvent.CycleRepeat) },
                contentDescription = "$cdRepeat $repeatText",
                borderColor = if (repeating) P.control else P.pillBorder,
                containerColor = if (repeating) P.control else Color.Transparent,
            ) {
                Icon(painterResource(R.drawable.ic_repeat), null, tint = repeatColor, modifier = Modifier.size(13.dp))
                Spacer(Modifier.width(4.dp))
                AppText(repeatText, style = t.bodySmallStrong, color = repeatColor, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun QueueSheet(state: NowPlayingUiState, onEvent: (NowPlayingEvent) -> Unit) {
    val t = PrayerTheme.type
    ModalBottomSheet(
        onDismissRequest = { onEvent(NowPlayingEvent.CloseQueue) },
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = P.sheet,
        scrimColor = P.queueScrim,
        shape = RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp),
        dragHandle = { SheetHandle(color = P.pillBorder) },
    ) {
        Column(Modifier.navigationBarsPadding()) {
            AppText(
                stringResource(R.string.queue),
                style = t.sheetTitle,
                color = P.text,
                modifier = Modifier.padding(start = 22.dp, end = 22.dp, top = 14.dp, bottom = 6.dp),
            )
            LazyColumn(
                Modifier.heightIn(max = 520.dp),
                contentPadding = PaddingValues(start = 22.dp, end = 22.dp, bottom = 30.dp),
            ) {
                itemsIndexed(state.queue, key = { _, q -> q.index }) { _, q ->
                    Column {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .heightIn(min = 58.dp)
                            .clickable(role = Role.Button) { onEvent(NowPlayingEvent.PlayQueueItem(q.index)) }
                            .semantics { selected = q.isCurrent },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        AppText(
                            state.count(q.index + 1),
                            style = t.bodyStrong,
                            color = if (q.isCurrent) P.verseCurrent else P.time,
                            modifier = Modifier.widthIn(min = PrayerDimens.queueNumberMin),
                        )
                        Column(Modifier.weight(1f)) {
                            AppText(q.en, style = t.bodyLargeStrong, color = if (q.isCurrent) P.verseCurrent else P.text)
                            AppText(q.tib, style = t.tibetanCaption, color = P.time)
                        }
                        AppText(q.durLabel, style = t.bodySmallMedium, color = P.time)
                    }
                    Box(Modifier.fillMaxWidth().height(1.dp).background(P.queueDivider))
                    }
                }
            }
        }
    }
}
