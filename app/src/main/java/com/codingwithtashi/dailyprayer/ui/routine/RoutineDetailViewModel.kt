package com.codingwithtashi.dailyprayer.ui.routine

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.codingwithtashi.dailyprayer.core.time.AppClock
import com.codingwithtashi.dailyprayer.data.model.Routine
import com.codingwithtashi.dailyprayer.data.prefs.UserPreferencesRepository
import com.codingwithtashi.dailyprayer.data.repository.PrayerRepository
import com.codingwithtashi.dailyprayer.data.repository.RoutineRepository
import com.codingwithtashi.dailyprayer.domain.RoutineProgress
import com.codingwithtashi.dailyprayer.playback.PlaybackError
import com.codingwithtashi.dailyprayer.playback.PlayerControllerApi
import com.codingwithtashi.dailyprayer.reminders.ReminderHealthMonitor
import com.codingwithtashi.dailyprayer.reminders.ReminderIssue
import com.codingwithtashi.dailyprayer.domain.RoutineProgressCalculator
import com.codingwithtashi.dailyprayer.domain.StartLabel
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.DayOfWeek
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class RoutineDetailPrayer(
    val id: Int,
    /** 0-based position in the routine. */
    val index: Int,
    val done: Boolean,
    val tib: String,
    val titleEn: String,
    val estMinutes: Int,
)

enum class RoutineDetailStatus { LOADING, READY, GONE }

/** Raw pieces: the screen resolves strings and digits (meta line, "N min", start label). */
data class RoutineDetailUiState(
    val status: RoutineDetailStatus = RoutineDetailStatus.LOADING,
    val name: String = "",
    val timeMinutes: Int = 0,
    val days: Set<DayOfWeek> = emptySet(),
    /** The reminder is set and can sound: `routine.reminderEnabled`, reminders on globally, notifications not blocked. */
    val reminderOn: Boolean = false,
    /** The routine's reminder is set but reminders are switched off globally in Settings. */
    val globalRemindersOff: Boolean = false,
    /** Set when the routine's reminder is on but the device would not show it (or show it on time). */
    val reminderIssue: ReminderIssue? = null,
    val progress: RoutineProgress = RoutineProgressCalculator.compute(emptyList(), emptySet()),
    val prayers: List<RoutineDetailPrayer> = emptyList(),
    val startLabel: StartLabel = StartLabel.START_READING,
    /** Prayer the primary button opens (next unread, or the first when everything is done). */
    val startTarget: Int? = null,
    val hasAudio: Boolean = false,
    val audioQueue: List<Int> = emptyList(),
) {
    val everyDay: Boolean get() = days.size == 7
}

/** User actions on the routine detail screen. */
sealed interface RoutineDetailEvent {
    /** Send on resume so a midnight rollover resets the progress. */
    data object RefreshDate : RoutineDetailEvent
    /** Start the routine's audio queue. */
    data object Listen : RoutineDetailEvent
}

/** One-shot effects of the routine detail screen. */
sealed interface RoutineDetailEffect {
    /** Playback was requested; show the player. */
    data object OpenPlayer : RoutineDetailEffect
    /** Playback could not start. */
    data class PlaybackFailed(val error: PlaybackError) : RoutineDetailEffect
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class RoutineDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val routineRepository: RoutineRepository,
    private val prayerRepository: PrayerRepository,
    prefs: UserPreferencesRepository,
    healthMonitor: ReminderHealthMonitor,
    private val clock: AppClock,
    private val player: PlayerControllerApi,
) : ViewModel() {

    val routineId: Long = savedStateHandle.get<Long>("routineId") ?: -1L

    private val today = MutableStateFlow(clock.today())
    private val minutesCache = HashMap<Int, Int>()
    private val effectChannel = Channel<RoutineDetailEffect>(Channel.BUFFERED)
    val effects: Flow<RoutineDetailEffect> = effectChannel.receiveAsFlow()

    val state: StateFlow<RoutineDetailUiState> = combine(
        routineRepository.observeRoutine(routineId).map { routine -> routine to routine?.let { minutesFor(it) }.orEmpty() },
        today.flatMapLatest { routineRepository.observeCompletions(it) },
        prefs.prefs,
        healthMonitor.health,
    ) { (routine, minutes), completions, userPrefs, h ->
        if (routine == null) {
            RoutineDetailUiState(status = RoutineDetailStatus.GONE)
        } else {
            val ids = routine.prayers.map { it.id }
            val completed = completions.filter { it.routineId == routine.id }.mapTo(HashSet()) { it.prayerId }
            val progress = RoutineProgressCalculator.compute(ids, completed)
            RoutineDetailUiState(
                status = RoutineDetailStatus.READY,
                name = routine.name,
                timeMinutes = routine.timeMinutes,
                days = routine.days,
                reminderOn = routine.reminderEnabled && userPrefs.remindersEnabled && h.issue != ReminderIssue.NOTIFICATIONS_OFF,
                globalRemindersOff = routine.reminderEnabled && !userPrefs.remindersEnabled,
                reminderIssue = h.issue.takeIf { routine.reminderEnabled && userPrefs.remindersEnabled },
                progress = progress,
                prayers = routine.prayers.mapIndexed { i, p ->
                    RoutineDetailPrayer(p.id, i, p.id in progress.doneIds, p.title, p.titleEn, minutes[p.id] ?: 1)
                },
                startLabel = RoutineProgressCalculator.startLabel(progress),
                startTarget = progress.nextPrayerId,
                hasAudio = routine.prayers.any { it.hasAudio },
                audioQueue = routine.prayers.filter { it.hasAudio }.map { it.id },
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), RoutineDetailUiState())

    private suspend fun minutesFor(routine: Routine): Map<Int, Int> =
        routine.prayers.associate { p ->
            p.id to minutesCache.getOrPut(p.id) { prayerRepository.text(p.id).estMinutes }
        }

    fun onEvent(event: RoutineDetailEvent) {
        when (event) {
            RoutineDetailEvent.RefreshDate -> today.value = clock.today()
            RoutineDetailEvent.Listen -> listen()
        }
    }

    /** Starts the routine's audio queue with the routine name as context; reports the outcome as an effect. */
    private fun listen() {
        val s = state.value
        if (s.audioQueue.isEmpty()) return
        viewModelScope.launch {
            val error = player.play(s.audioQueue, 0, s.name, routineId)
            effectChannel.send(if (error == null) RoutineDetailEffect.OpenPlayer else RoutineDetailEffect.PlaybackFailed(error))
        }
    }
}
