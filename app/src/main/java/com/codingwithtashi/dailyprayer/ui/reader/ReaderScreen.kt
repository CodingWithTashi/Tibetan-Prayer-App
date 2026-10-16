package com.codingwithtashi.dailyprayer.ui.reader

import androidx.compose.ui.platform.testTag
import android.content.Context
import android.view.WindowManager
import androidx.activity.compose.LocalActivity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.LineBreak
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.codingwithtashi.dailyprayer.R
import com.codingwithtashi.dailyprayer.core.locale.AppLanguage
import com.codingwithtashi.dailyprayer.core.text.TibetanNumerals
import com.codingwithtashi.dailyprayer.data.model.PrayerCategory
import com.codingwithtashi.dailyprayer.playback.PlaybackError
import com.codingwithtashi.dailyprayer.ui.components.AppText
import com.codingwithtashi.dailyprayer.ui.components.CounterBar
import com.codingwithtashi.dailyprayer.ui.components.IconActionButton
import com.codingwithtashi.dailyprayer.ui.components.LocalToaster
import com.codingwithtashi.dailyprayer.ui.components.toastAnchor
import com.codingwithtashi.dailyprayer.ui.components.PillButton
import com.codingwithtashi.dailyprayer.ui.components.PillVariant
import com.codingwithtashi.dailyprayer.ui.components.ProgressBarThin
import com.codingwithtashi.dailyprayer.ui.components.TopBar
import com.codingwithtashi.dailyprayer.ui.components.contentWidth
import com.codingwithtashi.dailyprayer.ui.settings.ReadingSettingsSheet
import com.codingwithtashi.dailyprayer.ui.settings.ReadingSettingsState
import com.codingwithtashi.dailyprayer.ui.theme.CategoryKey
import com.codingwithtashi.dailyprayer.ui.theme.PrayerDimens
import com.codingwithtashi.dailyprayer.ui.theme.PrayerTheme
import com.codingwithtashi.dailyprayer.ui.theme.SystemBarIcons
import com.codingwithtashi.dailyprayer.ui.theme.category
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

@Composable
fun ReaderScreen(
    prayerId: Int,
    routineId: Long?,
    onBack: () -> Unit,
    onReplaceWith: (prayerId: Int, routineId: Long) -> Unit,
    onFinished: () -> Unit,
    onOpenPlayer: () -> Unit,
) {
    val viewModel: ReaderViewModel = hiltViewModel()
    val state by viewModel.state.collectAsStateWithLifecycle()
    val toaster = LocalToaster.current
    val context = LocalContext.current
    val latestState by rememberUpdatedState(state)
    val undo = stringResource(R.string.undo)
    val offlineMessage = stringResource(R.string.offline_cannot_stream)
    val playbackErrorMessage = stringResource(R.string.playback_error)
    // Outlives this screen so the "routine complete" toast survives the pop.
    val toastScope = remember { CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate) }
    val onBackLatest by rememberUpdatedState(onBack)
    val onReplaceLatest by rememberUpdatedState(onReplaceWith)
    val onFinishedLatest by rememberUpdatedState(onFinished)
    val onPlayerLatest by rememberUpdatedState(onOpenPlayer)

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                ReaderEffect.OpenPlayer -> onPlayerLatest()
                is ReaderEffect.ReplaceWith -> onReplaceLatest(effect.prayerId, effect.routineId)
                ReaderEffect.Finished -> onFinishedLatest()
                is ReaderEffect.PlayerError -> launch {
                    toaster.show(if (effect.error == PlaybackError.OFFLINE) offlineMessage else playbackErrorMessage)
                }
                is ReaderEffect.Toast -> {
                    val message = effect.toast.message(context, latestState.language)
                    val scope = if (effect.toast is ReaderToast.RoutineComplete) toastScope else this
                    scope.launch {
                        val undoFn = effect.undo
                        if (undoFn != null) toaster.show(message, undo) { undoFn() } else toaster.show(message)
                    }
                }
            }
        }
    }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, viewModel) {
        val observer = LifecycleEventObserver { _, e -> if (e == Lifecycle.Event.ON_STOP) viewModel.onEvent(ReaderEvent.Flush) }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    ReaderContent(
        state = state,
        onEvent = { if (it == ReaderEvent.Back) onBackLatest() else viewModel.onEvent(it) },
    )
}

private fun ReaderToast.message(context: Context, language: AppLanguage): String = when (this) {
    ReaderToast.AddedToSaved -> context.getString(R.string.added_to_saved)
    ReaderToast.RemovedFromSaved -> context.getString(R.string.removed_from_saved)
    is ReaderToast.GoalReached -> context.getString(R.string.goal_reached, TibetanNumerals.formatCount(count, language))
    ReaderToast.CountReset -> context.getString(R.string.count_reset)
    is ReaderToast.RoutineComplete -> context.getString(R.string.routine_complete, name)
}

@OptIn(FlowPreview::class)
@Composable
fun ReaderContent(state: ReaderUiState, onEvent: (ReaderEvent) -> Unit) {
    val pal = PrayerTheme.reader
    val colors = PrayerTheme.colors
    val type = PrayerTheme.type
    val readerBody = remember(type, state.textSizeSp) { type.tibetanReaderBody(state.textSizeSp) }
    SystemBarIcons(darkBackground = colors.isDark)
    KeepScreenOn(state.keepScreenOn)
    val bg by animateColorAsState(pal.bg, tween(300), label = "readerBg")
    val listState = rememberLazyListState()
    val latestEvent by rememberUpdatedState(onEvent)
    LaunchedEffect(listState) {
        snapshotFlow { listState.scrollFraction() }
            .distinctUntilChanged()
            .debounce(300)
            .collect { latestEvent(ReaderEvent.ScrollProgress(it)) }
    }
    val compactHeight = (LocalWindowInfo.current.containerSize.height / LocalDensity.current.density) < 480f
    val prayer = state.prayer
    // The counter bar grows with text size, so the scroll inset and the fade follow its measured height.
    val density = LocalDensity.current
    var counterBarHeight by remember { mutableStateOf(PrayerDimens.counterBarMin) }

    Box(Modifier.fillMaxSize().background(bg)) {
        Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
            TopBar(
                onBack = { onEvent(ReaderEvent.Back) },
                tint = pal.fg,
                center = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        AppText(prayer?.titleEn.orEmpty(), style = type.bodyStrong, color = pal.fg, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        val ctx = state.routineCtx
                        if (ctx != null) {
                            AppText(
                                "${ctx.name} · ${state.routinePositionText}",
                                style = type.caption, color = pal.muted, maxLines = 1, overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                },
                actions = {
                    IconActionButton(stringResource(R.string.cd_reading_settings), { onEvent(ReaderEvent.OpenAa) }) {
                        AppText(stringResource(R.string.reader_settings_glyph), style = type.aaGlyph, color = pal.fg)
                    }
                    if (prayer?.hasAudio == true) {
                        IconActionButton(
                            icon = if (state.isPlayingThis) R.drawable.ic_headphones_filled else R.drawable.ic_headphones,
                            contentDescription = stringResource(R.string.cd_listen),
                            onClick = { onEvent(ReaderEvent.Listen) },
                            tint = pal.fg,
                        )
                    }
                    IconActionButton(
                        icon = if (state.isSaved) R.drawable.ic_heart_filled else R.drawable.ic_heart,
                        contentDescription = stringResource(if (state.isSaved) R.string.cd_unsave_prayer else R.string.cd_save_prayer),
                        onClick = { onEvent(ReaderEvent.ToggleSave) },
                        tint = pal.accent,
                    )
                },
            )
            ProgressBarThin(
                progress = state.scrollProgress,
                height = 2.dp,
                trackColor = pal.border,
                fillColor = pal.accent,
                animate = false,
                announceProgress = false,
            )
            val navBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
            LazyColumn(
                state = listState,
                modifier = Modifier.weight(1f).contentWidth(720.dp),
                contentPadding = PaddingValues(start = 28.dp, end = 28.dp, top = 16.dp, bottom = counterBarHeight + 80.dp + navBottom),
            ) {
                if (prayer != null) {
                    item(key = "header") {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Box(Modifier.size(8.dp).background(colors.category(CategoryKey.valueOf(state.category.name)).dot, CircleShape))
                                val label = stringResource(state.category.labelRes())
                                AppText(label.uppercase(), style = type.label, color = pal.muted)
                            }
                            AppText(prayer.title, style = type.tibetanReaderTitle, color = pal.accent)
                            AppText(
                                "${prayer.titleEn} · ${stringResource(R.string.minutes_short, state.minutesText)}",
                                style = type.readerSubtitle, color = pal.muted,
                                // Tightens the gap left by the tall Tibetan title line box above.
                                modifier = Modifier.offset(y = (-6).dp),
                            )
                            HorizontalDivider(Modifier.padding(top = 12.dp, bottom = 14.dp), color = pal.border)
                        }
                    }
                    item(key = "body") {
                        SelectionContainer {
                            Column {
                                state.paragraphs.forEach { p ->
                                    AppText(
                                        p,
                                        style = readerBody,
                                        color = pal.fg,
                                        modifier = Modifier.padding(bottom = 14.dp),
                                    )
                                }
                            }
                        }
                    }
                    item(key = "end") {
                        AppText(
                            "༄༅།",
                            style = type.tibetanEndMark, color = pal.muted, textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth().padding(top = 10.dp, bottom = 18.dp),
                        )
                    }
                    val ctx = state.routineCtx
                    if (ctx != null) {
                        item(key = "done") {
                            val last = ctx.nextPrayerId == null
                            PillButton(
                                text = if (last) stringResource(R.string.done_finish, ctx.name) else stringResource(R.string.done_next),
                                onClick = { onEvent(if (last) ReaderEvent.DoneFinish else ReaderEvent.DoneNext) },
                                modifier = Modifier.fillMaxWidth().testTag("reader_done"),
                                variant = PillVariant.Filled,
                                height = 52.dp,
                                containerColor = pal.accentSolid,
                            )
                        }
                    }
                }
            }
        }
        Box(Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(counterBarHeight + 60.dp).background(pal.fadeBrush()))
        Column(
            Modifier
                .align(Alignment.BottomCenter)
                .widthIn(max = 640.dp)
                .fillMaxWidth()
                .toastAnchor()
                .navigationBarsPadding()
                .padding(start = 14.dp, end = 14.dp, bottom = if (compactHeight) 12.dp else 28.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            AnimatedVisibility(state.confirmReset) {
                ConfirmResetBar(onCancel = { onEvent(ReaderEvent.CancelReset) }, onReset = { onEvent(ReaderEvent.DoReset) })
            }
            CounterBar(
                modifier = Modifier.onSizeChanged { counterBarHeight = with(density) { it.height.toDp() } },
                countText = state.countText,
                goalText = state.goalText,
                progress = state.counterProgress,
                onDec = { onEvent(ReaderEvent.Dec) },
                onInc = { onEvent(ReaderEvent.Inc) },
                onReset = { onEvent(ReaderEvent.AskReset) },
                resetEnabled = state.count > 0,
            )
        }
    }
    if (state.showAaSheet) {
        ReadingSettingsSheet(
            state = ReadingSettingsState(state.textSizeSp, state.appearance, state.goal),
            onSizeDown = { onEvent(ReaderEvent.SizeDown) },
            onSizeUp = { onEvent(ReaderEvent.SizeUp) },
            onAppearance = { onEvent(ReaderEvent.SetAppearance(it)) },
            onGoal = { onEvent(ReaderEvent.SetGoal(it)) },
            onDismiss = { onEvent(ReaderEvent.CloseAa) },
        )
    }
}

@Composable
private fun ConfirmResetBar(onCancel: () -> Unit, onReset: () -> Unit) {
    val c = PrayerTheme.colors
    val shape = RoundedCornerShape(16.dp)
    Row(
        Modifier
            .fillMaxWidth()
            .shadow(8.dp, shape, ambientColor = c.ink, spotColor = c.ink)
            .background(c.surface, shape)
            .border(BorderStroke(1.dp, c.border), shape)
            .heightIn(min = PrayerDimens.confirmBarMin)
            .padding(start = 16.dp, end = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AppText(stringResource(R.string.reset_q), style = PrayerTheme.type.bodyMedium, color = c.ink, modifier = Modifier.weight(1f))
        PillButton(stringResource(R.string.cancel), onCancel, variant = PillVariant.Text, height = 36.dp, contentColor = c.textMuted)
        PillButton(stringResource(R.string.reset), onReset, variant = PillVariant.Filled, height = 36.dp, horizontalPadding = 14.dp)
    }
}

@Composable
private fun KeepScreenOn(enabled: Boolean) {
    if (LocalInspectionMode.current) return
    val activity = LocalActivity.current
    DisposableEffect(enabled, activity) {
        val window = activity?.window
        if (enabled) window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose { if (enabled) window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) }
    }
}

private fun PrayerCategory.labelRes(): Int = when (this) {
    PrayerCategory.DAILY -> R.string.cat_daily
    PrayerCategory.SUTRA -> R.string.cat_sutra
    PrayerCategory.LIFE -> R.string.cat_life
    PrayerCategory.PRAISE -> R.string.cat_praise
    PrayerCategory.ASPIRE -> R.string.cat_aspire
    PrayerCategory.OTHER -> R.string.cat_other
}

/** Approximate scroll position (0..1) from the list's first visible item and the item count. */
private fun LazyListState.scrollFraction(): Float {
    val info = layoutInfo
    val count = info.totalItemsCount
    if (count == 0) return 0f
    if (!canScrollForward && (canScrollBackward)) return 1f
    val first = info.visibleItemsInfo.firstOrNull() ?: return 0f
    val within = if (first.size > 0) firstVisibleItemScrollOffset.toFloat() / first.size else 0f
    return ((firstVisibleItemIndex + within) / count).coerceIn(0f, 1f)
}
