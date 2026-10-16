package com.codingwithtashi.dailyprayer.ui.routine

import androidx.annotation.StringRes
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.codingwithtashi.dailyprayer.R
import com.codingwithtashi.dailyprayer.core.di.ApplicationScope
import com.codingwithtashi.dailyprayer.core.locale.AppLanguage
import com.codingwithtashi.dailyprayer.core.time.DateFormats
import com.codingwithtashi.dailyprayer.data.model.Prayer
import com.codingwithtashi.dailyprayer.data.model.Routine
import com.codingwithtashi.dailyprayer.data.model.RoutineDraft
import com.codingwithtashi.dailyprayer.data.prefs.UserPreferencesRepository
import com.codingwithtashi.dailyprayer.data.repository.PrayerRepository
import com.codingwithtashi.dailyprayer.data.repository.RoutineRepository
import com.codingwithtashi.dailyprayer.reminders.DaysMask
import com.codingwithtashi.dailyprayer.reminders.NotificationPermissionPolicy
import com.codingwithtashi.dailyprayer.ui.theme.CategoryKey
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.DayOfWeek
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Prayer as shown by the editor rows and the picker. */
data class EditorPrayer(
    val id: Int,
    val syllable: String,
    val category: CategoryKey,
    val tib: String,
    val en: String,
)

data class RoutineEditorUiState(
    val isNew: Boolean = true,
    val name: String = "",
    val timeMinutes: Int = DEFAULT_TIME,
    /** Monday first. */
    val days: List<Boolean> = List(7) { true },
    val reminder: Boolean = true,
    val prayers: List<EditorPrayer> = emptyList(),
    val canSave: Boolean = false,
    val showPicker: Boolean = false,
    val showTimePicker: Boolean = false,
    val showDiscardDialog: Boolean = false,
    val showExactAlarmPrompt: Boolean = false,
    val showNotificationsOffDialog: Boolean = false,
    val allPrayers: List<EditorPrayer> = emptyList(),
    val isDirty: Boolean = false,
) {
    val pickedIds: Set<Int> get() = prayers.mapTo(HashSet()) { it.id }

    companion object {
        const val DEFAULT_TIME = 420
        const val MAX_NAME = 30
    }
}

sealed interface RoutineEditorEvent {
    data class SetName(val value: String) : RoutineEditorEvent
    data class SetTime(val minutes: Int) : RoutineEditorEvent
    data class ToggleDay(val index: Int) : RoutineEditorEvent
    data object ToggleReminder : RoutineEditorEvent
    data class MoveUp(val index: Int) : RoutineEditorEvent
    data class Remove(val prayerId: Int) : RoutineEditorEvent
    data object OpenPicker : RoutineEditorEvent
    data object ClosePicker : RoutineEditorEvent
    data class TogglePick(val prayerId: Int) : RoutineEditorEvent
    data object OpenTimePicker : RoutineEditorEvent
    data object CloseTimePicker : RoutineEditorEvent
    data object Save : RoutineEditorEvent
    data object Delete : RoutineEditorEvent
    data object Cancel : RoutineEditorEvent
    data object ConfirmDiscard : RoutineEditorEvent
    data object DismissDiscard : RoutineEditorEvent

    /** The "Deleted" toast's Undo was tapped: restore [routine] (runs in the application scope). */
    data class UndoDelete(val routine: Routine) : RoutineEditorEvent

    /** The exact-alarm explanation dialog was closed. */
    data object DismissExactAlarmPrompt : RoutineEditorEvent

    /**
     * Sent by the screen after a save with the reminder on (and on again after the permission dialog closes).
     * [permissionMissing]: API 33+ and POST_NOTIFICATIONS not granted. [shouldShowRationale]: Android's
     * answer for whether a request can still be made. [exactAlarmsAllowed]: exact alarms may be scheduled.
     */
    data class ContinueAfterSave(
        val permissionMissing: Boolean,
        val shouldShowRationale: Boolean,
        val exactAlarmsAllowed: Boolean,
    ) : RoutineEditorEvent

    /** "Open settings" in the notifications-off dialog: opens the settings, then continues to the exact-alarm step. */
    data object OpenNotificationSettings : RoutineEditorEvent

    /** "Not now" in the notifications-off dialog. */
    data object DismissNotificationsOff : RoutineEditorEvent
}

sealed interface RoutineEditorEffect {
    data class Toast(@StringRes val res: Int, val args: List<Any> = emptyList()) : RoutineEditorEffect

    /**
     * Saved; when [reminderOn] (reminder set and reminders on globally) the screen reports the device state
     * with [RoutineEditorEvent.ContinueAfterSave], otherwise it closes.
     */
    data class Saved(val reminderOn: Boolean) : RoutineEditorEffect

    /** The screen should show the runtime notification permission dialog. */
    data object RequestNotificationPermission : RoutineEditorEffect

    /** The screen should open this app's notification settings. The exact-alarm step or [Close] follows. */
    data object OpenNotificationSettings : RoutineEditorEffect

    /** Deleted; the screen shows the Undo toast, which sends [RoutineEditorEvent.UndoDelete]. */
    data class Deleted(val routine: Routine) : RoutineEditorEffect
    data object Close : RoutineEditorEffect
}

private data class Draft(
    val name: String = "",
    val timeMinutes: Int = RoutineEditorUiState.DEFAULT_TIME,
    val daysMask: Int = DaysMask.EVERY_DAY,
    val reminder: Boolean = true,
    val prayerIds: List<Int> = emptyList(),
)

private data class Ui(
    val showPicker: Boolean = false,
    val showTimePicker: Boolean = false,
    val showDiscard: Boolean = false,
    val showExactAlarm: Boolean = false,
    val showNotificationsOff: Boolean = false,
)

@HiltViewModel
class RoutineEditorViewModel @Inject constructor(
    private val handle: SavedStateHandle,
    private val routineRepository: RoutineRepository,
    prayerRepository: PrayerRepository,
    private val prefs: UserPreferencesRepository,
    private val permissionPolicy: NotificationPermissionPolicy,
    @ApplicationScope private val appScope: CoroutineScope,
) : ViewModel() {

    private val routineId: Long? = handle.get<Long>(KEY_ROUTINE_ID)

    private val draft = MutableStateFlow(readDraft())
    private val original = MutableStateFlow<Draft?>(if (routineId == null) Draft() else null)
    private val ui = MutableStateFlow(Ui())
    private var exactAlarmsAllowed = true
    private val effectChannel = Channel<RoutineEditorEffect>(Channel.BUFFERED)
    val effects: Flow<RoutineEditorEffect> = effectChannel.receiveAsFlow()

    val state: StateFlow<RoutineEditorUiState> = combine(
        draft, original, ui, prayerRepository.observeAll(),
    ) { d, orig, u, all ->
        val byId = all.associateBy { it.id }
        RoutineEditorUiState(
            isNew = routineId == null,
            name = d.name,
            timeMinutes = d.timeMinutes,
            days = DayOfWeek.entries.map { DaysMask.contains(d.daysMask, it) },
            reminder = d.reminder,
            prayers = d.prayerIds.mapNotNull { byId[it]?.toEditor() },
            canSave = canSave(d),
            showPicker = u.showPicker,
            showTimePicker = u.showTimePicker,
            showDiscardDialog = u.showDiscard,
            showExactAlarmPrompt = u.showExactAlarm,
            showNotificationsOffDialog = u.showNotificationsOff,
            allPrayers = all.map { it.toEditor() },
            isDirty = orig != null && orig != d,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), RoutineEditorUiState(isNew = routineId == null))

    init {
        if (routineId != null) viewModelScope.launch { loadExisting(routineId) }
    }

    private suspend fun loadExisting(id: Long) {
        val routine = routineRepository.getRoutine(id)
        if (routine == null) {
            effectChannel.send(RoutineEditorEffect.Close)
            return
        }
        val loaded = routine.toDraft()
        original.value = loaded
        if (handle.get<Boolean>(KEY_INITIALIZED) != true) {
            writeDraft(loaded)
            handle[KEY_INITIALIZED] = true
        }
    }

    fun onEvent(event: RoutineEditorEvent) {
        when (event) {
            is RoutineEditorEvent.SetName -> edit { it.copy(name = event.value.take(RoutineEditorUiState.MAX_NAME)) }
            is RoutineEditorEvent.SetTime -> {
                edit { it.copy(timeMinutes = event.minutes.coerceIn(0, 1439)) }
                ui.update { it.copy(showTimePicker = false) }
            }
            is RoutineEditorEvent.ToggleDay -> edit {
                val day = DayOfWeek.of(event.index + 1)
                val next = it.daysMask xor DaysMask.bit(day)
                if (next and DaysMask.EVERY_DAY == 0) it else it.copy(daysMask = next)
            }
            RoutineEditorEvent.ToggleReminder -> edit { it.copy(reminder = !it.reminder) }
            is RoutineEditorEvent.MoveUp -> edit {
                val i = event.index
                if (i <= 0 || i >= it.prayerIds.size) it
                else it.copy(prayerIds = it.prayerIds.toMutableList().also { l -> java.util.Collections.swap(l, i, i - 1) })
            }
            is RoutineEditorEvent.Remove -> edit { it.copy(prayerIds = it.prayerIds - event.prayerId) }
            RoutineEditorEvent.OpenPicker -> ui.update { it.copy(showPicker = true) }
            RoutineEditorEvent.ClosePicker -> ui.update { it.copy(showPicker = false) }
            is RoutineEditorEvent.TogglePick -> edit {
                if (event.prayerId in it.prayerIds) it.copy(prayerIds = it.prayerIds - event.prayerId)
                else it.copy(prayerIds = it.prayerIds + event.prayerId)
            }
            RoutineEditorEvent.OpenTimePicker -> ui.update { it.copy(showTimePicker = true) }
            RoutineEditorEvent.CloseTimePicker -> ui.update { it.copy(showTimePicker = false) }
            RoutineEditorEvent.Save -> save()
            RoutineEditorEvent.Delete -> delete()
            RoutineEditorEvent.Cancel -> cancel()
            RoutineEditorEvent.ConfirmDiscard -> {
                ui.update { it.copy(showDiscard = false) }
                send(RoutineEditorEffect.Close)
            }
            RoutineEditorEvent.DismissDiscard -> ui.update { it.copy(showDiscard = false) }
            is RoutineEditorEvent.ContinueAfterSave -> continueAfterSave(event)
            is RoutineEditorEvent.UndoDelete -> undoDelete(event.routine)
            RoutineEditorEvent.DismissExactAlarmPrompt -> ui.update { it.copy(showExactAlarm = false) }
            RoutineEditorEvent.OpenNotificationSettings -> {
                ui.update { it.copy(showNotificationsOff = false) }
                send(RoutineEditorEffect.OpenNotificationSettings)
                promptExactAlarmOrClose()
            }
            RoutineEditorEvent.DismissNotificationsOff -> {
                ui.update { it.copy(showNotificationsOff = false) }
                promptExactAlarmOrClose()
            }
        }
    }

    private fun cancel() {
        if (isDirtyNow()) ui.update { it.copy(showDiscard = true) }
        else send(RoutineEditorEffect.Close)
    }

    private fun isDirtyNow(): Boolean = original.value.let { it != null && it != draft.value }

    private fun save() {
        val d = draft.value
        if (!canSave(d)) {
            val msg = if (d.name.isBlank()) R.string.give_name else R.string.add_one_prayer
            send(RoutineEditorEffect.Toast(msg))
            return
        }
        viewModelScope.launch {
            val name = d.name.trim()
            routineRepository.save(
                RoutineDraft(
                    id = routineId,
                    name = name,
                    timeMinutes = d.timeMinutes,
                    days = DaysMask.toSet(d.daysMask),
                    reminderEnabled = d.reminder,
                    prayerIds = d.prayerIds,
                ),
            )
            // Clock times always use English digits, so the language argument is irrelevant here.
            effectChannel.send(
                RoutineEditorEffect.Toast(R.string.saved_routine, listOf(name, DateFormats.time12(d.timeMinutes, AppLanguage.EN))),
            )
            val globalOn = prefs.snapshot().remindersEnabled
            if (d.reminder && !globalOn) effectChannel.send(RoutineEditorEffect.Toast(R.string.reminders_off_in_settings))
            effectChannel.send(RoutineEditorEffect.Saved(d.reminder && globalOn))
        }
    }

    private fun delete() {
        val id = routineId ?: return
        viewModelScope.launch {
            val copy = routineRepository.getRoutine(id)
            routineRepository.delete(id)
            if (copy != null) effectChannel.send(RoutineEditorEffect.Deleted(copy)) else effectChannel.send(RoutineEditorEffect.Close)
        }
    }

    /** Runs in the application scope: the editor (and this ViewModel) is gone by the time Undo is tapped. */
    private fun undoDelete(routine: Routine) {
        appScope.launch { routineRepository.restore(routine) }
    }

    /**
     * Permission follow-ups after saving a reminder, in order: notification permission (request it while
     * Android allows, else offer the settings), then the exact-alarm explanation, then close. Choosing "Open settings"
     * or "Not now" in the notifications-off dialog also continues to the exact-alarm step before closing. Saving is never blocked.
     */
    private fun continueAfterSave(event: RoutineEditorEvent.ContinueAfterSave) {
        exactAlarmsAllowed = event.exactAlarmsAllowed
        if (!event.permissionMissing) {
            promptExactAlarmOrClose()
            return
        }
        viewModelScope.launch {
            if (permissionPolicy.claimRequest(event.shouldShowRationale)) {
                effectChannel.send(RoutineEditorEffect.RequestNotificationPermission)
            } else {
                ui.update { it.copy(showNotificationsOff = true) }
            }
        }
    }

    private fun promptExactAlarmOrClose() {
        if (exactAlarmsAllowed) send(RoutineEditorEffect.Close) else ui.update { it.copy(showExactAlarm = true) }
    }

    private fun canSave(d: Draft) = d.name.isNotBlank() && d.prayerIds.isNotEmpty()

    private fun edit(block: (Draft) -> Draft) {
        val next = block(draft.value)
        if (next == draft.value) return
        draft.value = next
        writeDraft(next)
    }

    private fun send(effect: RoutineEditorEffect) {
        effectChannel.trySend(effect)
    }

    private fun readDraft() = Draft(
        name = handle.get<String>("name").orEmpty(),
        timeMinutes = handle.get<Int>("time") ?: RoutineEditorUiState.DEFAULT_TIME,
        daysMask = handle.get<Int>("days") ?: DaysMask.EVERY_DAY,
        reminder = handle.get<Boolean>("reminder") ?: true,
        prayerIds = handle.get<ArrayList<Int>>("prayers") ?: emptyList(),
    )

    private fun writeDraft(d: Draft) {
        draft.value = d
        handle["name"] = d.name
        handle["time"] = d.timeMinutes
        handle["days"] = d.daysMask
        handle["reminder"] = d.reminder
        handle["prayers"] = ArrayList(d.prayerIds)
    }

    private fun Routine.toDraft() = Draft(name, timeMinutes, DaysMask.fromSet(days), reminderEnabled, prayers.map { it.id })

    private fun Prayer.toEditor() = EditorPrayer(id, syllable, CategoryKey.valueOf(category.name), title, titleEn)

    private companion object {
        const val KEY_ROUTINE_ID = "routineId"
        const val KEY_INITIALIZED = "initialized"
    }
}
