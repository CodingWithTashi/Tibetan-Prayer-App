package com.codingwithtashi.dailyprayer.ui.reader

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.codingwithtashi.dailyprayer.core.di.ApplicationScope
import com.codingwithtashi.dailyprayer.core.locale.AppLanguage
import com.codingwithtashi.dailyprayer.core.text.PrayerText
import com.codingwithtashi.dailyprayer.core.text.TibetanNumerals
import com.codingwithtashi.dailyprayer.core.time.AppClock
import com.codingwithtashi.dailyprayer.data.model.Appearance
import com.codingwithtashi.dailyprayer.data.model.Prayer
import com.codingwithtashi.dailyprayer.data.model.PrayerCategory
import com.codingwithtashi.dailyprayer.data.prefs.UserPreferencesRepository
import com.codingwithtashi.dailyprayer.data.repository.PrayerRepository
import com.codingwithtashi.dailyprayer.data.repository.RoutineRepository
import com.codingwithtashi.dailyprayer.playback.PlaybackError
import com.codingwithtashi.dailyprayer.playback.PlayerControllerApi
import com.codingwithtashi.dailyprayer.ui.settings.MAX_TEXT_SIZE
import com.codingwithtashi.dailyprayer.ui.settings.MIN_TEXT_SIZE
import com.codingwithtashi.dailyprayer.ui.settings.TEXT_SIZE_STEP
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlin.math.abs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Position of the open prayer inside the routine it was opened from. */
data class RoutineCtx(
    val name: String,
    /** 0-based. */
    val index: Int,
    val total: Int,
    /** Next prayer in routine order, null when this is the last one. */
    val nextPrayerId: Int?,
)

data class ReaderUiState(
    val prayer: Prayer? = null,
    val paragraphs: List<String> = emptyList(),
    val category: PrayerCategory = PrayerCategory.OTHER,
    val estMinutes: Int = 1,
    val routineCtx: RoutineCtx? = null,
    val textSizeSp: Int = 22,
    val appearance: Appearance = Appearance.SYSTEM,
    val goal: Int = 108,
    val count: Int = 0,
    val isSaved: Boolean = false,
    /** This prayer is the current track and playing (headphones show filled). */
    val isPlayingThis: Boolean = false,
    val scrollProgress: Float = 0f,
    val confirmReset: Boolean = false,
    val showAaSheet: Boolean = false,
    val keepScreenOn: Boolean = true,
    val language: AppLanguage = AppLanguage.EN,
) {
    val countText: String get() = TibetanNumerals.formatCount(count, language)
    val goalText: String get() = TibetanNumerals.formatCount(goal, language)
    val minutesText: String get() = TibetanNumerals.formatMinutes(estMinutes, language)
    val counterProgress: Float get() = if (goal <= 0) 0f else minOf(1f, count.toFloat() / goal)

    /** "{index+1} / {total}" with the app's digits, or null outside a routine. */
    val routinePositionText: String?
        get() = routineCtx?.let {
            "${TibetanNumerals.formatCount(it.index + 1, language)} / ${TibetanNumerals.formatCount(it.total, language)}"
        }
}

sealed interface ReaderEvent {
    data object Back : ReaderEvent
    data object OpenAa : ReaderEvent
    data object CloseAa : ReaderEvent
    data object Listen : ReaderEvent
    data object ToggleSave : ReaderEvent
    data object Inc : ReaderEvent
    data object Dec : ReaderEvent
    data object AskReset : ReaderEvent
    data object CancelReset : ReaderEvent
    data object DoReset : ReaderEvent
    data object DoneNext : ReaderEvent
    data object DoneFinish : ReaderEvent
    data class ScrollProgress(val pct: Float) : ReaderEvent

    /** Persist the latest scroll progress now (screen stopped). */
    data object Flush : ReaderEvent
    data object SizeUp : ReaderEvent
    data object SizeDown : ReaderEvent
    data class SetAppearance(val appearance: Appearance) : ReaderEvent
    data class SetGoal(val goal: Int) : ReaderEvent
}

sealed interface ReaderToast {
    data object AddedToSaved : ReaderToast
    data object RemovedFromSaved : ReaderToast
    data class GoalReached(val count: Int) : ReaderToast
    data object CountReset : ReaderToast
    data class RoutineComplete(val name: String) : ReaderToast
}

sealed interface ReaderEffect {
    data object OpenPlayer : ReaderEffect
    data class ReplaceWith(val prayerId: Int, val routineId: Long) : ReaderEffect
    data object Finished : ReaderEffect

    /** Playback could not start (offline); the screen shows a toast and stays put. */
    data class PlayerError(val error: PlaybackError) : ReaderEffect

    /** [undo] non-null means the toast shows an Undo action. */
    data class Toast(val toast: ReaderToast, val undo: (() -> Unit)? = null) : ReaderEffect
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ReaderViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val prayers: PrayerRepository,
    private val routines: RoutineRepository,
    private val prefs: UserPreferencesRepository,
    private val clock: AppClock,
    @ApplicationScope private val appScope: CoroutineScope,
    private val player: PlayerControllerApi,
) : ViewModel() {

    private val prayerId: Int = checkNotNull(savedStateHandle["prayerId"])
    private val routineId: Long? = savedStateHandle.get<Long>("routineId")

    private data class Local(val confirmReset: Boolean = false, val showAa: Boolean = false, val scroll: Float = 0f)

    private val local = MutableStateFlow(Local())
    private val effectChannel = Channel<ReaderEffect>(Channel.BUFFERED)
    val effects: Flow<ReaderEffect> = effectChannel.receiveAsFlow()

    private var lastPersisted = 0f
    private var latestProgress = 0f
    private var progressTouched = false
    private var lastReadSet = false

    private val paragraphsFlow: Flow<PrayerText?> = prayers.observe(prayerId)
        .map { it?.content }
        .distinctUntilChanged()
        .mapLatest { if (it == null) null else prayers.text(prayerId) }

    private val routineCtxFlow: Flow<RoutineCtx?> =
        if (routineId == null) flowOf(null) else routines.observeRoutine(routineId).map { r ->
            val list = r?.prayers.orEmpty()
            val i = list.indexOfFirst { it.id == prayerId }
            if (r == null || i < 0) null else RoutineCtx(r.name, i, list.size, list.getOrNull(i + 1)?.id)
        }

    val state: StateFlow<ReaderUiState> = combine(
        prayers.observe(prayerId),
        paragraphsFlow,
        routineCtxFlow,
        prefs.prefs,
        combine(local, player.state.map { it.currentPrayerId == prayerId && it.isPlaying }.distinctUntilChanged()) { l, playing -> l to playing },
    ) { prayer, text, ctx, p, (l, playingThis) ->
        ReaderUiState(
            prayer = prayer,
            paragraphs = text?.paragraphs.orEmpty(),
            category = prayer?.category ?: PrayerCategory.OTHER,
            estMinutes = text?.estMinutes ?: 1,
            routineCtx = ctx,
            textSizeSp = p.textSize,
            appearance = p.appearance,
            goal = p.recitationGoal,
            count = prayer?.count ?: 0,
            isSaved = prayer?.isSaved == true,
            isPlayingThis = playingThis,
            scrollProgress = l.scroll,
            confirmReset = l.confirmReset,
            showAaSheet = l.showAa,
            keepScreenOn = p.keepScreenOn,
            language = p.appLanguage,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ReaderUiState())

    fun onEvent(event: ReaderEvent) {
        when (event) {
            ReaderEvent.Back -> Unit // handled by the screen (navigation)
            ReaderEvent.OpenAa -> local.update { it.copy(showAa = true) }
            ReaderEvent.CloseAa -> local.update { it.copy(showAa = false) }
            ReaderEvent.Listen -> listen()
            ReaderEvent.ToggleSave -> toggleSave()
            ReaderEvent.Inc -> inc()
            ReaderEvent.Dec -> dec()
            ReaderEvent.AskReset -> if (state.value.count > 0) local.update { it.copy(confirmReset = true) }
            ReaderEvent.CancelReset -> local.update { it.copy(confirmReset = false) }
            ReaderEvent.DoReset -> reset()
            ReaderEvent.DoneNext -> done(finish = false)
            ReaderEvent.DoneFinish -> done(finish = true)
            is ReaderEvent.ScrollProgress -> onScroll(event.pct)
            ReaderEvent.Flush -> flushProgress(appScope)
            ReaderEvent.SizeUp -> setSize(state.value.textSizeSp + TEXT_SIZE_STEP)
            ReaderEvent.SizeDown -> setSize(state.value.textSizeSp - TEXT_SIZE_STEP)
            is ReaderEvent.SetAppearance -> viewModelScope.launch { prefs.setAppearance(event.appearance) }
            is ReaderEvent.SetGoal -> viewModelScope.launch { prefs.setRecitationGoal(event.goal) }
        }
    }

    private fun listen() {
        viewModelScope.launch {
            if (player.state.value.currentPrayerId == prayerId) {
                send(ReaderEffect.OpenPlayer)
                return@launch
            }
            val ctx = state.value.routineCtx
            val label = ctx?.name ?: state.value.prayer?.titleEn.orEmpty()
            when (val error = player.play(listOf(prayerId), 0, label, routineId)) {
                null -> send(ReaderEffect.OpenPlayer)
                else -> send(ReaderEffect.PlayerError(error))
            }
        }
    }

    private fun send(effect: ReaderEffect) {
        effectChannel.trySend(effect)
    }

    private fun setSize(size: Int) {
        viewModelScope.launch { prefs.setTextSize(size.coerceIn(MIN_TEXT_SIZE, MAX_TEXT_SIZE)) }
    }

    private fun toggleSave() {
        val old = state.value.prayer?.isSaved ?: return
        viewModelScope.launch {
            prayers.setSaved(prayerId, !old)
            send(
                ReaderEffect.Toast(
                    if (old) ReaderToast.RemovedFromSaved else ReaderToast.AddedToSaved,
                    undo = { appScope.launch { prayers.setSaved(prayerId, old) } },
                ),
            )
        }
    }

    private fun inc() {
        viewModelScope.launch {
            val n = prayers.incrementCount(prayerId)
            if (n == state.value.goal) send(ReaderEffect.Toast(ReaderToast.GoalReached(n)))
        }
    }

    private fun dec() {
        if (state.value.count <= 0) return
        viewModelScope.launch { prayers.decrementCount(prayerId) }
    }

    private fun reset() {
        val previous = state.value.count
        local.update { it.copy(confirmReset = false) }
        if (previous <= 0) return
        viewModelScope.launch {
            prayers.setCount(prayerId, 0)
            send(ReaderEffect.Toast(ReaderToast.CountReset, undo = { appScope.launch { prayers.setCount(prayerId, previous) } }))
        }
    }

    private fun done(finish: Boolean) {
        val rid = routineId ?: return
        val ctx = state.value.routineCtx
        viewModelScope.launch {
            routines.markDone(rid, prayerId, clock.today())
            if (finish) {
                send(ReaderEffect.Toast(ReaderToast.RoutineComplete(ctx?.name.orEmpty())))
                send(ReaderEffect.Finished)
            } else {
                val next = ctx?.nextPrayerId
                if (next != null) send(ReaderEffect.ReplaceWith(next, rid)) else send(ReaderEffect.Finished)
            }
        }
    }

    private fun onScroll(raw: Float) {
        val pct = raw.coerceIn(0f, 1f)
        progressTouched = true
        latestProgress = pct
        local.update { it.copy(scroll = pct) }
        if (!lastReadSet) {
            lastReadSet = true
            viewModelScope.launch { prefs.setLastReadPrayerId(prayerId) }
        }
        if (abs(pct - lastPersisted) > PERSIST_DELTA || (pct > PERSIST_END && pct != lastPersisted)) {
            lastPersisted = pct
            viewModelScope.launch { prayers.setReadProgress(prayerId, pct) }
        }
    }

    private fun flushProgress(scope: CoroutineScope) {
        if (!progressTouched || latestProgress == lastPersisted) return
        val value = latestProgress
        lastPersisted = value
        scope.launch { prayers.setReadProgress(prayerId, value) }
    }

    override fun onCleared() {
        flushProgress(appScope)
    }

    private companion object {
        const val PERSIST_DELTA = 0.02f
        const val PERSIST_END = 0.98f
    }
}
