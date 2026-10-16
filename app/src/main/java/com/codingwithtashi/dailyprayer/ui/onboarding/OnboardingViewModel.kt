package com.codingwithtashi.dailyprayer.ui.onboarding

import android.os.Build
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.codingwithtashi.dailyprayer.core.locale.AppLanguage
import com.codingwithtashi.dailyprayer.core.locale.AppLocale
import com.codingwithtashi.dailyprayer.data.model.Prayer
import com.codingwithtashi.dailyprayer.data.model.RoutineDraft
import com.codingwithtashi.dailyprayer.data.prefs.UserPreferencesRepository
import com.codingwithtashi.dailyprayer.data.repository.PrayerRepository
import com.codingwithtashi.dailyprayer.data.repository.RoutineRepository
import com.codingwithtashi.dailyprayer.data.seed.OnboardingDefaults
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.DayOfWeek
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class OnboardingUiState(
    val step: Int = 0,
    val language: AppLanguage = AppLanguage.EN,
    val name: String = "",
    val picks: Set<String> = emptySet(),
    val candidates: List<Prayer> = emptyList(),
    val timeMinutes: Int = OnboardingDefaults.DEFAULT_TIME_MINUTES,
    val showTimePicker: Boolean = false,
)

sealed interface OnboardingEvent {
    data object Back : OnboardingEvent
    data object Next : OnboardingEvent
    data class SetLanguage(val language: AppLanguage) : OnboardingEvent
    data class SetName(val name: String) : OnboardingEvent
    data class TogglePick(val slug: String) : OnboardingEvent
    data object OpenTimePicker : OnboardingEvent
    data object DismissTimePicker : OnboardingEvent
    data class SetTime(val minutes: Int) : OnboardingEvent

    /** [routineName] is the localized "Morning practice" resolved by the UI layer. */
    data class Finish(val withRoutine: Boolean, val routineName: String = "") : OnboardingEvent
}

sealed interface OnboardingEffect {
    data object PickAtLeastOne : OnboardingEffect
    data object RequestNotificationPermission : OnboardingEffect

    /** Welcome toast; [name] is blank when the user skipped the name. */
    data class Welcome(val name: String) : OnboardingEffect
    data object Finished : OnboardingEffect
}

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val savedState: SavedStateHandle,
    private val prefs: UserPreferencesRepository,
    prayers: PrayerRepository,
    private val routines: RoutineRepository,
) : ViewModel() {

    /** Overridable for tests (AppCompatDelegate is not available on the JVM). */
    internal var applyLocale: (AppLanguage) -> Unit = { AppLocale.apply(it) }
    internal var sdkInt: Int = Build.VERSION.SDK_INT

    private val step = savedState.getStateFlow(KEY_STEP, 0)
    private val name = savedState.getStateFlow(KEY_NAME, "")
    private val picks = savedState.getStateFlow(KEY_PICKS, ArrayList(OnboardingDefaults.DEFAULT_PICK_SLUGS))
    private val time = savedState.getStateFlow(KEY_TIME, OnboardingDefaults.DEFAULT_TIME_MINUTES)
    private val showTimePicker = MutableStateFlow(false)

    private val candidates = prayers.observeAll().map { all ->
        OnboardingDefaults.CANDIDATE_SLUGS.mapNotNull { slug -> all.firstOrNull { it.slug == slug } }
    }

    private val effectChannel = Channel<OnboardingEffect>(Channel.BUFFERED)
    val effects = effectChannel
    private var finishing = false

    val uiState: StateFlow<OnboardingUiState> = combine(
        combine(step, name, picks, time, showTimePicker) { s, n, p, t, d ->
            OnboardingUiState(step = s, name = n, picks = p.toSet(), timeMinutes = t, showTimePicker = d)
        },
        candidates,
        prefs.prefs.map { it.appLanguage },
    ) { base, cands, lang -> base.copy(candidates = cands, language = lang) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), OnboardingUiState())

    fun onEvent(event: OnboardingEvent) {
        when (event) {
            OnboardingEvent.Back -> if (step.value > 0) savedState[KEY_STEP] = step.value - 1
            OnboardingEvent.Next -> if (step.value < LAST_STEP) savedState[KEY_STEP] = step.value + 1
            is OnboardingEvent.SetLanguage -> viewModelScope.launch {
                prefs.setAppLanguage(event.language)
                applyLocale(event.language)
            }
            is OnboardingEvent.SetName -> savedState[KEY_NAME] = event.name.take(MAX_NAME)
            is OnboardingEvent.TogglePick -> {
                val cur = picks.value
                savedState[KEY_PICKS] = if (event.slug in cur) ArrayList(cur - event.slug) else ArrayList(cur + event.slug)
            }
            OnboardingEvent.OpenTimePicker -> showTimePicker.value = true
            OnboardingEvent.DismissTimePicker -> showTimePicker.value = false
            is OnboardingEvent.SetTime -> {
                savedState[KEY_TIME] = event.minutes
                showTimePicker.value = false
            }
            is OnboardingEvent.Finish -> finish(event.withRoutine, event.routineName)
        }
    }

    /** Called by the UI after the notification-permission dialog closed (result ignored). */
    fun onPermissionAsked() {
        viewModelScope.launch { prefs.setNotificationPermissionAsked(true) }
    }

    /** True once a routine was created by the finish step (drives the exact-alarm prompt). */
    var routineCreated: Boolean = false
        private set

    /** Marks the exact-alarm explanation as shown; returns true only the first time. */
    suspend fun promptExactAlarmOnce(): Boolean {
        if (prefs.snapshot().exactAlarmPromptShown) return false
        prefs.setExactAlarmPromptShown(true)
        return true
    }

    private fun finish(withRoutine: Boolean, routineName: String) {
        if (finishing) return
        finishing = true
        viewModelScope.launch {
            var created = false
            if (withRoutine) {
                val chosen = picks.value.toSet()
                val ids = candidates.first().filter { it.slug in chosen }.map { it.id }
                if (ids.isEmpty()) {
                    finishing = false
                    effectChannel.send(OnboardingEffect.PickAtLeastOne)
                    return@launch
                }
                routines.save(
                    RoutineDraft(
                        id = null,
                        name = routineName,
                        timeMinutes = time.value,
                        days = DayOfWeek.entries.toSet(),
                        reminderEnabled = true,
                        prayerIds = ids,
                    ),
                )
                created = true
            }
            val trimmed = name.value.trim()
            prefs.setUserName(trimmed)
            prefs.setOnboarded(true)
            routineCreated = created
            if (created && sdkInt >= Build.VERSION_CODES.TIRAMISU) {
                effectChannel.send(OnboardingEffect.RequestNotificationPermission)
            }
            effectChannel.send(OnboardingEffect.Welcome(trimmed))
            effectChannel.send(OnboardingEffect.Finished)
        }
    }

    private companion object {
        const val KEY_STEP = "step"
        const val KEY_NAME = "name"
        const val KEY_PICKS = "picks"
        const val KEY_TIME = "time"
        const val LAST_STEP = 2
        const val MAX_NAME = 24
    }
}
