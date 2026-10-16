package com.codingwithtashi.dailyprayer.ui.routine

import com.codingwithtashi.dailyprayer.ui.testRoutine
import com.codingwithtashi.dailyprayer.ui.testPrayer
import androidx.lifecycle.SavedStateHandle
import com.codingwithtashi.dailyprayer.R
import com.codingwithtashi.dailyprayer.data.model.RoutineDraft
import com.codingwithtashi.dailyprayer.data.prefs.UserPreferencesRepository
import com.codingwithtashi.dailyprayer.data.prefs.UserPrefs
import com.codingwithtashi.dailyprayer.reminders.NotificationPermissionPolicy
import com.codingwithtashi.dailyprayer.data.repository.PrayerRepository
import com.codingwithtashi.dailyprayer.data.repository.RoutineRepository
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import java.time.DayOfWeek
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class RoutineEditorViewModelTest {
    private val dispatcher = UnconfinedTestDispatcher()
    private val routines = mockk<RoutineRepository>(relaxed = true)
    private val prayers = mockk<PrayerRepository>()
    private val prefs = mockk<UserPreferencesRepository>(relaxed = true)
    private val all = (1..5).map { testPrayer(it) }

    @Before fun setUp() {
        Dispatchers.setMain(dispatcher)
        coEvery { prayers.observeAll() } returns MutableStateFlow(all)
        coEvery { prefs.snapshot() } returns UserPrefs()
        coEvery { routines.save(any()) } returns 9L
    }

    @After fun tearDown() = Dispatchers.resetMain()

    private fun newVm(existing: Boolean = false, handle: SavedStateHandle? = null): RoutineEditorViewModel {
        if (existing) coEvery { routines.getRoutine(1) } returns testRoutine()
        return RoutineEditorViewModel(
            handle ?: if (existing) SavedStateHandle(mapOf("routineId" to 1L)) else SavedStateHandle(),
            routines, prayers, prefs, NotificationPermissionPolicy(prefs), CoroutineScope(dispatcher),
        )
    }

    private fun kotlinx.coroutines.test.TestScope.observe(vm: RoutineEditorViewModel) {
        backgroundScope.launch(dispatcher) { vm.state.collect { } }
    }

    @Test fun canSaveNeedsNameAndPrayer() = runTest(dispatcher) {
        val vm = newVm(); observe(vm)
        assertThat(vm.state.value.canSave).isFalse()
        vm.onEvent(RoutineEditorEvent.SetName("   "))
        vm.onEvent(RoutineEditorEvent.TogglePick(1))
        assertThat(vm.state.value.canSave).isFalse()
        vm.onEvent(RoutineEditorEvent.SetName("Dawn"))
        assertThat(vm.state.value.canSave).isTrue()
        vm.onEvent(RoutineEditorEvent.Remove(1))
        assertThat(vm.state.value.canSave).isFalse()
    }

    @Test fun nameIsCappedAt30() = runTest(dispatcher) {
        val vm = newVm(); observe(vm)
        vm.onEvent(RoutineEditorEvent.SetName("x".repeat(45)))
        assertThat(vm.state.value.name).hasLength(30)
    }

    @Test fun lastDayCannotBeCleared() = runTest(dispatcher) {
        val vm = newVm(); observe(vm)
        (0..5).forEach { vm.onEvent(RoutineEditorEvent.ToggleDay(it)) }
        assertThat(vm.state.value.days).containsExactly(false, false, false, false, false, false, true).inOrder()
        vm.onEvent(RoutineEditorEvent.ToggleDay(6))
        assertThat(vm.state.value.days[6]).isTrue()
    }

    @Test fun moveUpAndRemove() = runTest(dispatcher) {
        val vm = newVm(); observe(vm)
        listOf(1, 2, 3).forEach { vm.onEvent(RoutineEditorEvent.TogglePick(it)) }
        vm.onEvent(RoutineEditorEvent.MoveUp(0))
        assertThat(vm.state.value.prayers.map { it.id }).containsExactly(1, 2, 3).inOrder()
        vm.onEvent(RoutineEditorEvent.MoveUp(2))
        assertThat(vm.state.value.prayers.map { it.id }).containsExactly(1, 3, 2).inOrder()
        vm.onEvent(RoutineEditorEvent.Remove(1))
        assertThat(vm.state.value.prayers.map { it.id }).containsExactly(3, 2).inOrder()
    }

    @Test fun pickerToggleKeepsInsertionOrder() = runTest(dispatcher) {
        val vm = newVm(); observe(vm)
        listOf(4, 2, 5).forEach { vm.onEvent(RoutineEditorEvent.TogglePick(it)) }
        vm.onEvent(RoutineEditorEvent.TogglePick(2))
        vm.onEvent(RoutineEditorEvent.TogglePick(1))
        assertThat(vm.state.value.prayers.map { it.id }).containsExactly(4, 5, 1).inOrder()
        assertThat(vm.state.value.allPrayers).hasSize(5)
    }

    @Test fun saveInvalidShowsToast() = runTest(dispatcher) {
        val vm = newVm(); observe(vm)
        vm.onEvent(RoutineEditorEvent.Save)
        assertThat((vm.effects.first() as RoutineEditorEffect.Toast).res).isEqualTo(R.string.give_name)
        vm.onEvent(RoutineEditorEvent.SetName("A"))
        vm.onEvent(RoutineEditorEvent.Save)
        assertThat((vm.effects.first() as RoutineEditorEffect.Toast).res).isEqualTo(R.string.add_one_prayer)
        coVerify(exactly = 0) { routines.save(any()) }
    }

    @Test fun saveSendsDraftWithPositions() = runTest(dispatcher) {
        val vm = newVm(); observe(vm)
        vm.onEvent(RoutineEditorEvent.SetName("  Dawn "))
        listOf(3, 1, 2).forEach { vm.onEvent(RoutineEditorEvent.TogglePick(it)) }
        vm.onEvent(RoutineEditorEvent.SetTime(390))
        vm.onEvent(RoutineEditorEvent.ToggleDay(5)); vm.onEvent(RoutineEditorEvent.ToggleDay(6))
        vm.onEvent(RoutineEditorEvent.ToggleReminder)
        vm.onEvent(RoutineEditorEvent.Save)
        val slot = slot<RoutineDraft>()
        coVerify { routines.save(capture(slot)) }
        assertThat(slot.captured).isEqualTo(
            RoutineDraft(null, "Dawn", 390, DayOfWeek.entries.take(5).toSet(), false, listOf(3, 1, 2)),
        )
        val toast = vm.effects.first() as RoutineEditorEffect.Toast
        assertThat(toast.args).containsExactly("Dawn", "6:30 AM").inOrder()
        assertThat(vm.effects.first()).isEqualTo(RoutineEditorEffect.Saved(false))
    }

    @Test fun deleteThenUndoRestoresCopy() = runTest(dispatcher) {
        val vm = newVm(existing = true); observe(vm)
        vm.onEvent(RoutineEditorEvent.Delete)
        val effect = vm.effects.first() as RoutineEditorEffect.Deleted
        coVerify { routines.delete(1) }
        vm.onEvent(RoutineEditorEvent.UndoDelete(effect.routine))
        coVerify { routines.restore(testRoutine()) }
    }

    @Test fun dirtyTrackingAndDiscard() = runTest(dispatcher) {
        val vm = newVm(existing = true); observe(vm)
        assertThat(vm.state.value.name).isEqualTo("Morning")
        assertThat(vm.state.value.isNew).isFalse()
        assertThat(vm.state.value.isDirty).isFalse()
        vm.onEvent(RoutineEditorEvent.SetName("Evening"))
        assertThat(vm.state.value.isDirty).isTrue()
        vm.onEvent(RoutineEditorEvent.SetName("Morning"))
        assertThat(vm.state.value.isDirty).isFalse()
        vm.onEvent(RoutineEditorEvent.Remove(1))
        vm.onEvent(RoutineEditorEvent.Cancel)
        assertThat(vm.state.value.showDiscardDialog).isTrue()
        vm.onEvent(RoutineEditorEvent.ConfirmDiscard)
        assertThat(vm.effects.first()).isEqualTo(RoutineEditorEffect.Close)
    }

    @Test fun cancelWhenCleanClosesWithoutDialog() = runTest(dispatcher) {
        val vm = newVm(); observe(vm)
        vm.onEvent(RoutineEditorEvent.Cancel)
        assertThat(vm.state.value.showDiscardDialog).isFalse()
        assertThat(vm.effects.first()).isEqualTo(RoutineEditorEffect.Close)
    }

    @Test fun draftSurvivesRecreationViaSavedState() = runTest(dispatcher) {
        val handle = SavedStateHandle()
        val first = newVm(handle = handle)
        first.onEvent(RoutineEditorEvent.SetName("Kept"))
        first.onEvent(RoutineEditorEvent.TogglePick(2))
        val second = newVm(handle = handle); observe(second)
        assertThat(second.state.value.name).isEqualTo("Kept")
        assertThat(second.state.value.prayers.map { it.id }).containsExactly(2)
    }

    private fun after(permissionMissing: Boolean = false, rationale: Boolean = false, exactAllowed: Boolean = true) =
        RoutineEditorEvent.ContinueAfterSave(permissionMissing, rationale, exactAllowed)

    @Test fun exactAlarmPromptShownOnEveryReminderSaveWhileNotAllowed() = runTest(dispatcher) {
        val vm = newVm(); observe(vm)
        repeat(2) {
            vm.onEvent(after(exactAllowed = false))
            assertThat(vm.state.value.showExactAlarmPrompt).isTrue()
            vm.onEvent(RoutineEditorEvent.DismissExactAlarmPrompt)
            assertThat(vm.state.value.showExactAlarmPrompt).isFalse()
        }
    }

    @Test fun closesWhenNothingToAsk() = runTest(dispatcher) {
        val vm = newVm(); observe(vm)
        vm.onEvent(after())
        assertThat(vm.effects.first()).isEqualTo(RoutineEditorEffect.Close)
    }

    @Test fun requestsPermissionWhenNeverAsked() = runTest(dispatcher) {
        val vm = newVm(); observe(vm)
        vm.onEvent(after(permissionMissing = true))
        assertThat(vm.effects.first()).isEqualTo(RoutineEditorEffect.RequestNotificationPermission)
        coVerify { prefs.setNotificationPermissionAsked(true) }
        assertThat(vm.state.value.showNotificationsOffDialog).isFalse()
    }

    @Test fun requestsPermissionWhenRationaleEvenIfAsked() = runTest(dispatcher) {
        coEvery { prefs.snapshot() } returns UserPrefs(notificationPermissionAsked = true)
        val vm = newVm(); observe(vm)
        vm.onEvent(after(permissionMissing = true, rationale = true))
        assertThat(vm.effects.first()).isEqualTo(RoutineEditorEffect.RequestNotificationPermission)
    }

    @Test fun permanentlyDeniedShowsSettingsDialogThenExactPrompt() = runTest(dispatcher) {
        coEvery { prefs.snapshot() } returns UserPrefs(notificationPermissionAsked = true)
        val vm = newVm(); observe(vm)
        vm.onEvent(after(permissionMissing = true, exactAllowed = false))
        assertThat(vm.state.value.showNotificationsOffDialog).isTrue()
        assertThat(vm.state.value.showExactAlarmPrompt).isFalse()
        vm.onEvent(RoutineEditorEvent.DismissNotificationsOff)
        assertThat(vm.state.value.showNotificationsOffDialog).isFalse()
        assertThat(vm.state.value.showExactAlarmPrompt).isTrue()
    }

    @Test fun openSettingsFromDialogEmitsEffect() = runTest(dispatcher) {
        coEvery { prefs.snapshot() } returns UserPrefs(notificationPermissionAsked = true)
        val vm = newVm(); observe(vm)
        vm.onEvent(after(permissionMissing = true))
        vm.onEvent(RoutineEditorEvent.OpenNotificationSettings)
        assertThat(vm.state.value.showNotificationsOffDialog).isFalse()
        assertThat(vm.effects.first()).isEqualTo(RoutineEditorEffect.OpenNotificationSettings)
        // Exact alarms allowed: the flow ends after the settings are opened.
        assertThat(vm.effects.first()).isEqualTo(RoutineEditorEffect.Close)
    }

    @Test fun openSettingsFromDialogContinuesToExactAlarmPromptBeforeClosing() = runTest(dispatcher) {
        coEvery { prefs.snapshot() } returns UserPrefs(notificationPermissionAsked = true)
        val vm = newVm(); observe(vm)
        vm.onEvent(after(permissionMissing = true, exactAllowed = false))
        vm.onEvent(RoutineEditorEvent.OpenNotificationSettings)
        assertThat(vm.effects.first()).isEqualTo(RoutineEditorEffect.OpenNotificationSettings)
        assertThat(vm.state.value.showNotificationsOffDialog).isFalse()
        assertThat(vm.state.value.showExactAlarmPrompt).isTrue()
    }

    @Test fun globalRemindersOffToastsAndSkipsPrompts() = runTest(dispatcher) {
        coEvery { prefs.snapshot() } returns UserPrefs(remindersEnabled = false)
        val vm = newVm(); observe(vm)
        vm.onEvent(RoutineEditorEvent.SetName("Morning"))
        vm.onEvent(RoutineEditorEvent.TogglePick(1))
        vm.onEvent(RoutineEditorEvent.Save)
        val effects = listOf(vm.effects.first(), vm.effects.first(), vm.effects.first())
        assertThat(effects[1]).isEqualTo(RoutineEditorEffect.Toast(R.string.reminders_off_in_settings))
        assertThat(effects[2]).isEqualTo(RoutineEditorEffect.Saved(false))
    }

    @Test fun reminderOnAndGlobalOnSavesWithReminderOn() = runTest(dispatcher) {
        val vm = newVm(); observe(vm)
        vm.onEvent(RoutineEditorEvent.SetName("Morning"))
        vm.onEvent(RoutineEditorEvent.TogglePick(1))
        vm.onEvent(RoutineEditorEvent.Save)
        val effects = listOf(vm.effects.first(), vm.effects.first())
        assertThat(effects[1]).isEqualTo(RoutineEditorEffect.Saved(true))
    }
}
