package com.codingwithtashi.dailyprayer.ui.routine

import com.codingwithtashi.dailyprayer.ui.testRoutine
import com.codingwithtashi.dailyprayer.ui.testPrayer
import androidx.lifecycle.SavedStateHandle
import com.codingwithtashi.dailyprayer.core.text.PrayerText
import com.codingwithtashi.dailyprayer.ui.FakeClock
import com.codingwithtashi.dailyprayer.data.model.Routine
import com.codingwithtashi.dailyprayer.data.model.RoutineCompletion
import com.codingwithtashi.dailyprayer.data.prefs.UserPreferencesRepository
import com.codingwithtashi.dailyprayer.data.prefs.UserPrefs
import com.codingwithtashi.dailyprayer.data.repository.PrayerRepository
import com.codingwithtashi.dailyprayer.data.repository.RoutineRepository
import com.codingwithtashi.dailyprayer.domain.StartLabel
import com.codingwithtashi.dailyprayer.playback.PlaybackError
import com.codingwithtashi.dailyprayer.ui.FakePlayerControllerApi
import com.codingwithtashi.dailyprayer.reminders.ReminderHealth
import com.codingwithtashi.dailyprayer.reminders.ReminderIssue
import com.codingwithtashi.dailyprayer.ui.PlayCall
import com.codingwithtashi.dailyprayer.ui.FakeReminderHealthMonitor
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import java.time.DayOfWeek
import java.time.LocalDate
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
class RoutineDetailViewModelTest {
    private val dispatcher = UnconfinedTestDispatcher()
    private val routine = MutableStateFlow<Routine?>(null)
    private val completions = MutableStateFlow<List<RoutineCompletion>>(emptyList())
    private val prefs = MutableStateFlow(UserPrefs())
    private val player = FakePlayerControllerApi()
    private val routines = mockk<RoutineRepository>()
    private val prayers = mockk<PrayerRepository>()
    private val userPrefs = mockk<UserPreferencesRepository>()
    private val health = FakeReminderHealthMonitor()

    @Before fun setUp() {
        Dispatchers.setMain(dispatcher)
        every { routines.observeRoutine(1) } returns routine
        every { routines.observeCompletions(any()) } returns completions
        coEvery { prayers.text(any()) } returns PrayerText(emptyList(), emptyList(), 120, 4)
        every { userPrefs.prefs } returns prefs
    }

    @After fun tearDown() = Dispatchers.resetMain()

    private fun done(prayerId: Int) = RoutineCompletion(1, prayerId, LocalDate.of(2026, 3, 4), 0L)

    private fun TestScopeAlias.vm(): RoutineDetailViewModel {
        val vm = RoutineDetailViewModel(SavedStateHandle(mapOf("routineId" to 1L)), routines, prayers, userPrefs, health, FakeClock(), player)
        backgroundScope.launch(dispatcher) { vm.state.collect { } }
        return vm
    }

    @Test fun startsLoadingThenReady() = runTest(dispatcher) {
        routine.value = testRoutine()
        val vm = vm()
        val s = vm.state.value
        assertThat(s.status).isEqualTo(RoutineDetailStatus.READY)
        assertThat(s.prayers.map { it.id }).containsExactly(1, 2, 3).inOrder()
        assertThat(s.prayers.first().estMinutes).isEqualTo(4)
        assertThat(s.startLabel).isEqualTo(StartLabel.START_READING)
        assertThat(s.startTarget).isEqualTo(1)
    }

    @Test fun progressAndStartLabelTransitions() = runTest(dispatcher) {
        routine.value = testRoutine()
        val vm = vm()
        completions.value = listOf(done(1))
        assertThat(vm.state.value.startLabel).isEqualTo(StartLabel.CONTINUE)
        assertThat(vm.state.value.startTarget).isEqualTo(2)
        assertThat(vm.state.value.progress.doneCount).isEqualTo(1)
        assertThat(vm.state.value.prayers.map { it.done }).containsExactly(true, false, false).inOrder()
        completions.value = listOf(done(1), done(2), done(3))
        assertThat(vm.state.value.startLabel).isEqualTo(StartLabel.READ_AGAIN)
        assertThat(vm.state.value.startTarget).isEqualTo(1)
        assertThat(vm.state.value.progress.fraction).isEqualTo(1f)
    }

    @Test fun otherRoutineCompletionsIgnored() = runTest(dispatcher) {
        routine.value = testRoutine()
        val vm = vm()
        completions.value = listOf(RoutineCompletion(2, 1, LocalDate.of(2026, 3, 4), 0L))
        assertThat(vm.state.value.progress.doneCount).isEqualTo(0)
    }

    @Test fun metaPieces() = runTest(dispatcher) {
        routine.value = testRoutine(time = 390, days = setOf(DayOfWeek.MONDAY, DayOfWeek.FRIDAY), reminder = true)
        val vm = vm()
        val s = vm.state.value
        assertThat(s.timeMinutes).isEqualTo(390)
        assertThat(s.everyDay).isFalse()
        assertThat(s.reminderOn).isTrue()
        assertThat(buildRoutineMeta("6:30 AM", "M F", "Reminder on")).isEqualTo("6:30 AM · M F · Reminder on")
        assertThat(buildRoutineMeta("6:30 AM", "Every day", null)).isEqualTo("6:30 AM · Every day")
        prefs.value = UserPrefs(remindersEnabled = false)
        assertThat(vm.state.value.reminderOn).isFalse()
        routine.value = testRoutine(reminder = false)
        prefs.value = UserPrefs(remindersEnabled = true)
        assertThat(vm.state.value.reminderOn).isFalse()
        assertThat(vm.state.value.everyDay).isTrue()
    }

    @Test fun audioQueueOnlyAudioPrayers() = runTest(dispatcher) {
        routine.value = testRoutine(prayers = listOf(testPrayer(1, hasAudio = true), testPrayer(2), testPrayer(3, hasAudio = true)))
        val vm = vm()
        assertThat(vm.state.value.hasAudio).isTrue()
        assertThat(vm.state.value.audioQueue).containsExactly(1, 3).inOrder()
        routine.value = testRoutine(prayers = listOf(testPrayer(2)))
        assertThat(vm.state.value.hasAudio).isFalse()
        assertThat(vm.state.value.audioQueue).isEmpty()
    }

    @Test fun deletedRoutineReportsGone() = runTest(dispatcher) {
        routine.value = testRoutine()
        val vm = vm()
        routine.value = null
        assertThat(vm.state.value.status).isEqualTo(RoutineDetailStatus.GONE)
    }

    @Test fun listenPlaysAudioQueueWithRoutineContext() = runTest(dispatcher) {
        routine.value = testRoutine(prayers = listOf(testPrayer(1, hasAudio = true), testPrayer(2), testPrayer(3, hasAudio = true)))
        val vm = vm()
        vm.onEvent(RoutineDetailEvent.Listen)
        assertThat(vm.effects.first()).isEqualTo(RoutineDetailEffect.OpenPlayer)
        assertThat(player.lastPlay).isEqualTo(PlayCall(listOf(1, 3), 0, "Morning", 1L))
    }

    @Test fun listenReportsOfflineAndSkipsEmptyQueue() = runTest(dispatcher) {
        routine.value = testRoutine(prayers = listOf(testPrayer(1, hasAudio = true)))
        val vm = vm()
        player.playResult = PlaybackError.OFFLINE
        vm.onEvent(RoutineDetailEvent.Listen)
        assertThat(vm.effects.first()).isEqualTo(RoutineDetailEffect.PlaybackFailed(PlaybackError.OFFLINE))
        routine.value = testRoutine(prayers = listOf(testPrayer(1)))
        player.lastPlay = null
        vm.onEvent(RoutineDetailEvent.Listen)
        assertThat(player.lastPlay).isNull()
    }

    @Test fun noNoticeWhenHealthy() = runTest(dispatcher) {
        routine.value = testRoutine(reminder = true)
        val s = vm().state.value
        assertThat(s.reminderIssue).isNull()
        assertThat(s.globalRemindersOff).isFalse()
        assertThat(s.reminderOn).isTrue()
    }

    @Test fun notificationsOffWarnsAndDropsReminderOn() = runTest(dispatcher) {
        health.state.value = ReminderHealth(false, true)
        routine.value = testRoutine(reminder = true)
        val s = vm().state.value
        assertThat(s.reminderIssue).isEqualTo(ReminderIssue.NOTIFICATIONS_OFF)
        assertThat(s.reminderOn).isFalse()
    }

    @Test fun exactOffWarnsButReminderStillOn() = runTest(dispatcher) {
        health.state.value = ReminderHealth(true, false)
        routine.value = testRoutine(reminder = true)
        val s = vm().state.value
        assertThat(s.reminderIssue).isEqualTo(ReminderIssue.EXACT_ALARMS_OFF)
        assertThat(s.reminderOn).isTrue()
    }

    @Test fun globalOffShowsNoticeAndNoReminderOn() = runTest(dispatcher) {
        prefs.value = UserPrefs(remindersEnabled = false)
        routine.value = testRoutine(reminder = true)
        val s = vm().state.value
        assertThat(s.globalRemindersOff).isTrue()
        assertThat(s.reminderIssue).isNull()
        assertThat(s.reminderOn).isFalse()
    }

    @Test fun routineReminderOffNeverWarns() = runTest(dispatcher) {
        health.state.value = ReminderHealth(false, false)
        routine.value = testRoutine(reminder = false)
        assertThat(vm().state.value.reminderIssue).isNull()
        assertThat(vm().state.value.globalRemindersOff).isFalse()
    }

    @Test fun refreshClearsWarningAfterFix() = runTest(dispatcher) {
        health.state.value = ReminderHealth(false, true)
        routine.value = testRoutine(reminder = true)
        val vm = vm()
        assertThat(vm.state.value.reminderIssue).isEqualTo(ReminderIssue.NOTIFICATIONS_OFF)
        health.state.value = ReminderHealth(true, true)
        assertThat(vm.state.value.reminderIssue).isNull()
        assertThat(vm.state.value.reminderOn).isTrue()
    }
}

private typealias TestScopeAlias = kotlinx.coroutines.test.TestScope
