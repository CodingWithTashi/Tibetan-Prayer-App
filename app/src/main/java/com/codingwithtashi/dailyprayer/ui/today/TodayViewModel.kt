package com.codingwithtashi.dailyprayer.ui.today

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.codingwithtashi.dailyprayer.core.locale.AppLanguage
import com.codingwithtashi.dailyprayer.core.locale.AppLocale
import com.codingwithtashi.dailyprayer.core.text.TibetanNumerals
import com.codingwithtashi.dailyprayer.core.time.AppClock
import com.codingwithtashi.dailyprayer.core.time.DateFormats
import com.codingwithtashi.dailyprayer.data.model.Prayer
import com.codingwithtashi.dailyprayer.data.model.Routine
import com.codingwithtashi.dailyprayer.data.model.RoutineCompletion
import com.codingwithtashi.dailyprayer.data.prefs.UserPrefs
import com.codingwithtashi.dailyprayer.data.prefs.UserPreferencesRepository
import com.codingwithtashi.dailyprayer.data.repository.NotificationRepository
import com.codingwithtashi.dailyprayer.data.repository.PrayerRepository
import com.codingwithtashi.dailyprayer.data.repository.RoutineRepository
import com.codingwithtashi.dailyprayer.domain.RoutineProgressCalculator
import com.codingwithtashi.dailyprayer.domain.StreakCalculator
import com.codingwithtashi.dailyprayer.ui.components.avatarInitial
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.DayOfWeek
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class WeekDay(val dayOfWeek: DayOfWeek, val filled: Boolean, val isToday: Boolean)

sealed interface RoutineSubtitle {
    data object Complete : RoutineSubtitle

    /** English prayer names joined by " · ", plus " (d/n)" when some are done. */
    data class Names(val text: String) : RoutineSubtitle
}

data class TodayRoutine(
    val id: Long,
    val name: String,
    val timeLabel: String,
    val subtitle: RoutineSubtitle,
    val done: Boolean,
)

sealed interface TodayCta {
    data class Start(val routineId: Long, val name: String, val nextPrayerId: Int) : TodayCta
    data object AllDone : TodayCta
    data object AddRoutine : TodayCta
}

data class ContinueReading(val prayer: Prayer, val pct: String)
data class SavedTile(val prayer: Prayer, val label: String)

data class TodayUiState(
    val language: AppLanguage = AppLanguage.EN,
    val greetingName: String? = null,
    val initial: String = "D",
    /** Formatted by the composable with the resource arrays (see [DateFormats.dateLabel]). */
    val date: LocalDate = LocalDate.now(),
    val week: List<WeekDay> = emptyList(),
    val streak: String = "0",
    val routines: List<TodayRoutine> = emptyList(),
    val practiceCount: String? = null,
    val cta: TodayCta = TodayCta.AddRoutine,
    val continueReading: ContinueReading? = null,
    val savedTiles: List<SavedTile> = emptyList(),
    val hasSaved: Boolean = false,
    val unreadNotifications: Int = 0,
)

sealed interface TodayEvent {
    data object ToggleLanguage : TodayEvent
    data object OpenSettings : TodayEvent
    data object OpenNotifications : TodayEvent
    data class OpenRoutine(val id: Long) : TodayEvent
    data class StartRoutine(val routineId: Long, val nextPrayerId: Int) : TodayEvent
    data object NewRoutine : TodayEvent
    data class OpenReader(val prayerId: Int) : TodayEvent
    data object SeeAllSaved : TodayEvent
    /** Re-reads "today" (midnight rollover / app returning to the foreground). */
    data object Refresh : TodayEvent
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class TodayViewModel @Inject constructor(
    private val prefs: UserPreferencesRepository,
    routines: RoutineRepository,
    prayers: PrayerRepository,
    notifications: NotificationRepository,
    private val clock: AppClock,
) : ViewModel() {

    /** Overridable for tests (AppCompatDelegate is not available on the JVM). */
    internal var applyLocale: (AppLanguage) -> Unit = { AppLocale.apply(it) }

    private val today = MutableStateFlow(clock.today())

    val state: StateFlow<TodayUiState> = today.flatMapLatest { day ->
        combine(
            prefs.prefs,
            routines.observeRoutines(),
            routines.observeCompletions(day),
            routines.observeCompletionDates(),
            combine(prayers.observeSaved(), prayers.observeContinueReading(), notifications.unreadCount()) { s, c, u ->
                Triple(s, c, u)
            },
        ) { p, r, completions, dates, extra ->
            build(day, p, r, completions, dates, extra.first, extra.second, extra.third)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TodayUiState(date = clock.today()))

    fun onEvent(event: TodayEvent) {
        when (event) {
            TodayEvent.Refresh -> today.value = clock.today()
            TodayEvent.ToggleLanguage -> viewModelScope.launch {
                val next = if (state.value.language == AppLanguage.EN) AppLanguage.BO else AppLanguage.EN
                prefs.setAppLanguage(next)
                applyLocale(next)
            }
            // Navigation events are handled by the screen.
            else -> Unit
        }
    }

    private fun build(
        day: LocalDate,
        p: UserPrefs,
        all: List<Routine>,
        completions: List<RoutineCompletion>,
        practiceDates: Set<LocalDate>,
        saved: List<Prayer>,
        reading: Prayer?,
        unread: Int,
    ): TodayUiState {
        val lang = p.appLanguage
        val name = p.userName.trim().ifBlank { null }
        val progress = all.associate { r ->
            val doneIds = completions.filter { it.routineId == r.id }.mapTo(HashSet()) { it.prayerId }
            r.id to RoutineProgressCalculator.compute(r.prayers.map { it.id }, doneIds)
        }
        val rows = all.map { r ->
            val pr = progress.getValue(r.id)
            val subtitle = when {
                pr.isComplete -> RoutineSubtitle.Complete
                else -> {
                    val names = r.prayers.joinToString(" · ") { it.titleEn }
                    val suffix = if (pr.doneCount > 0) {
                        " (${TibetanNumerals.formatCount(pr.doneCount, lang)}/${TibetanNumerals.formatCount(pr.total, lang)})"
                    } else ""
                    RoutineSubtitle.Names(names + suffix)
                }
            }
            TodayRoutine(r.id, r.name, DateFormats.time12(r.timeMinutes, lang), subtitle, pr.isComplete)
        }
        val next = all.firstOrNull { r -> progress.getValue(r.id).let { !it.isComplete && it.nextPrayerId != null } }
        val cta = when {
            next != null -> TodayCta.Start(next.id, next.name, progress.getValue(next.id).nextPrayerId!!)
            rows.isNotEmpty() && rows.all { it.done } -> TodayCta.AllDone
            else -> TodayCta.AddRoutine
        }
        val doneRoutines = rows.count { it.done }
        return TodayUiState(
            language = lang,
            greetingName = name,
            initial = avatarInitial(name),
            date = day,
            week = StreakCalculator.weekRow(practiceDates, day).map { WeekDay(it.dayOfWeek, it.done, it.isToday) },
            streak = TibetanNumerals.formatCount(StreakCalculator.currentStreak(practiceDates, day), lang),
            routines = rows,
            practiceCount = if (rows.isEmpty()) null else
                "${TibetanNumerals.formatCount(doneRoutines, lang)} / ${TibetanNumerals.formatCount(rows.size, lang)}",
            cta = cta,
            continueReading = reading?.takeIf { it.lastReadProgress > 0.03f && it.lastReadProgress < 0.97f }
                ?.let { ContinueReading(it, TibetanNumerals.formatPercent(it.lastReadProgress, lang)) },
            savedTiles = saved.take(4).map { SavedTile(it, savedLabel(it)) },
            hasSaved = saved.isNotEmpty(),
            unreadNotifications = unread,
        )
    }

    internal companion object {
        fun savedLabel(p: Prayer): String =
            p.savedLabel ?: p.titleEn.removePrefix("The ")
    }
}
