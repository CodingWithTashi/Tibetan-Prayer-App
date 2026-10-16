package com.codingwithtashi.dailyprayer.ui.reader

import androidx.lifecycle.SavedStateHandle
import com.codingwithtashi.dailyprayer.core.locale.AppLanguage
import com.codingwithtashi.dailyprayer.core.text.PrayerText
import com.codingwithtashi.dailyprayer.core.time.AppClock
import com.codingwithtashi.dailyprayer.data.model.Appearance
import com.codingwithtashi.dailyprayer.data.model.Prayer
import com.codingwithtashi.dailyprayer.data.model.PrayerCategory
import com.codingwithtashi.dailyprayer.data.model.Routine
import com.codingwithtashi.dailyprayer.data.model.RoutineCompletion
import com.codingwithtashi.dailyprayer.data.model.RoutineDraft
import com.codingwithtashi.dailyprayer.data.prefs.UserPrefs
import com.codingwithtashi.dailyprayer.data.prefs.UserPreferencesRepository
import com.codingwithtashi.dailyprayer.data.repository.PrayerRepository
import com.codingwithtashi.dailyprayer.data.repository.RoutineRepository
import com.codingwithtashi.dailyprayer.playback.PlaybackError
import com.codingwithtashi.dailyprayer.playback.PlayerState
import com.codingwithtashi.dailyprayer.ui.FakePlayerControllerApi
import com.google.common.truth.Truth.assertThat
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Test

private fun prayer(id: Int, count: Int = 0, saved: Boolean = false) = Prayer(
    id = id, slug = "s$id", title = "བོད$id", titleEn = "Prayer $id", content = "c$id", category = PrayerCategory.DAILY,
    sortOrder = id, audioUrl = "", hasAudio = true, isDownloaded = false, audioPath = null, audioDurationMs = null,
    isSaved = saved, savedLabel = null, savedAt = null, count = count, lastReadProgress = 0f, lastReadAt = null, syllable = "བ",
)

private class RPrayers(initial: List<Prayer>) : PrayerRepository {
    val all = MutableStateFlow(initial)
    val progress = mutableListOf<Float>()
    private fun edit(id: Int, f: (Prayer) -> Prayer) { all.value = all.value.map { if (it.id == id) f(it) else it } }
    override fun observeAll() = all
    override fun observeSaved() = all.map { l -> l.filter { it.isSaved } }
    override fun observeWithAudio() = all
    override fun observe(id: Int) = all.map { l -> l.firstOrNull { it.id == id } }
    override suspend fun get(id: Int) = all.value.firstOrNull { it.id == id }
    override suspend fun getByIds(ids: List<Int>) = ids.mapNotNull { get(it) }
    override fun observeContinueReading(): Flow<Prayer?> = MutableStateFlow(null)
    override suspend fun setSaved(id: Int, saved: Boolean) = edit(id) { it.copy(isSaved = saved) }
    override suspend fun setSavedLabel(id: Int, label: String?) = Unit
    override suspend fun setCount(id: Int, count: Int) = edit(id) { it.copy(count = count) }
    override suspend fun incrementCount(id: Int): Int { edit(id) { it.copy(count = it.count + 1) }; return get(id)!!.count }
    override suspend fun decrementCount(id: Int): Int { edit(id) { it.copy(count = maxOf(0, it.count - 1)) }; return get(id)!!.count }
    override suspend fun setReadProgress(id: Int, progress: Float) { this.progress += progress }
    override suspend fun text(id: Int) = PrayerText(listOf("v"), listOf("p1", "p2"), 60, 3)
    override suspend fun updateAudioDuration(id: Int, ms: Long) = Unit
    override suspend fun upsertRemote(title: String, content: String, audioUrl: String?, titleEn: String?, category: String?) = 0
}

private class RRoutines(routine: Routine?) : RoutineRepository {
    val routine = MutableStateFlow(routine)
    val done = mutableListOf<Pair<Long, Int>>()
    override fun observeRoutines() = this.routine.map { listOfNotNull(it) }
    override fun observeRoutine(id: Long) = this.routine
    override suspend fun getRoutine(id: Long) = routine.value
    override suspend fun getRoutines() = listOfNotNull(routine.value)
    override suspend fun save(draft: RoutineDraft) = 1L
    override suspend fun delete(id: Long) = Unit
    override suspend fun restore(routine: Routine) = Unit
    override fun observeCompletions(date: LocalDate): Flow<List<RoutineCompletion>> = MutableStateFlow(emptyList())
    override fun observeCompletionDates(): Flow<Set<LocalDate>> = MutableStateFlow(emptySet())
    override suspend fun markDone(routineId: Long, prayerId: Int, date: LocalDate?) { done += routineId to prayerId }
}

private class RPrefs(initial: UserPrefs = UserPrefs(recitationGoal = 3)) : UserPreferencesRepository {
    val state = MutableStateFlow(initial)
    var lastReadSets = 0
    override val prefs: Flow<UserPrefs> = state
    override suspend fun snapshot() = state.value
    override suspend fun setOnboarded(value: Boolean) = Unit
    override suspend fun setUserName(name: String) = Unit
    override suspend fun setAppLanguage(language: AppLanguage) { state.value = state.value.copy(appLanguage = language) }
    override suspend fun setAppearance(appearance: Appearance) { state.value = state.value.copy(appearance = appearance) }
    override suspend fun setTextSize(size: Int) { state.value = state.value.copy(textSize = size) }
    override suspend fun setRecitationGoal(goal: Int) { state.value = state.value.copy(recitationGoal = goal) }
    override suspend fun setRemindersEnabled(enabled: Boolean) = Unit
    override suspend fun setKeepScreenOn(enabled: Boolean) = Unit
    override suspend fun setLastReadPrayerId(id: Int?) { lastReadSets++; state.value = state.value.copy(lastReadPrayerId = id) }
    override suspend fun setLegacyCleanupDone(done: Boolean) = Unit
    override suspend fun setExactAlarmPromptShown(shown: Boolean) = Unit
    override suspend fun setNotificationPermissionAsked(asked: Boolean) = Unit
    override suspend fun setWelcomeNotificationInserted(inserted: Boolean) = Unit
}

private object TestClock : AppClock {
    override fun now(): Instant = Instant.EPOCH
    override fun today(): LocalDate = LocalDate.of(2026, 1, 1)
    override fun zone(): ZoneId = ZoneId.of("UTC")
}

@OptIn(ExperimentalCoroutinesApi::class)
class ReaderViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @After fun tearDown() = Dispatchers.resetMain()

    private class Env(
        val vm: ReaderViewModel,
        val prayers: RPrayers,
        val routines: RRoutines,
        val prefs: RPrefs,
        val effects: MutableList<ReaderEffect>,
        val player: FakePlayerControllerApi,
    )

    private fun TestScope.setup(
        prayerId: Int = 2,
        routineId: Long? = null,
        routinePrayers: List<Int> = listOf(1, 2, 3),
        prefs: RPrefs = RPrefs(),
    ): Env {
        Dispatchers.setMain(dispatcher)
        val prayers = RPrayers((1..3).map { prayer(it) })
        val routine = routineId?.let {
            Routine(it, "Morning", 480, DayOfWeek.entries.toSet(), true, routinePrayers.map { id -> prayer(id) }, 0L)
        }
        val routines = RRoutines(routine)
        val handle = SavedStateHandle(buildMap {
            put("prayerId", prayerId)
            if (routineId != null) put("routineId", routineId)
        })
        val appScope = CoroutineScope(dispatcher + SupervisorJob())
        val player = FakePlayerControllerApi()
        val vm = ReaderViewModel(handle, prayers, routines, prefs, TestClock, appScope, player)
        val effects = mutableListOf<ReaderEffect>()
        backgroundScope.launch { vm.state.collect {} }
        appScope.launch { vm.effects.collect { effects += it } }
        return Env(vm, prayers, routines, prefs, effects, player)
    }

    private fun Env.send(e: ReaderEvent) = vm.onEvent(e)

    @Test fun loadsTextAndRoutineContext() = runTest(dispatcher) {
        val e = setup(prayerId = 2, routineId = 9)
        advanceUntilIdle()
        val s = e.vm.state.value
        assertThat(s.prayer?.id).isEqualTo(2)
        assertThat(s.paragraphs).containsExactly("p1", "p2").inOrder()
        assertThat(s.estMinutes).isEqualTo(3)
        assertThat(s.routineCtx).isEqualTo(RoutineCtx("Morning", 1, 3, 3))
    }

    @Test fun lastPrayerHasNoNextAndFinishes() = runTest(dispatcher) {
        val e = setup(prayerId = 3, routineId = 9)
        advanceUntilIdle()
        assertThat(e.vm.state.value.routineCtx).isEqualTo(RoutineCtx("Morning", 2, 3, null))
        e.send(ReaderEvent.DoneFinish)
        advanceUntilIdle()
        assertThat(e.routines.done).containsExactly(9L to 3)
        assertThat(e.effects.map { it::class }).containsAtLeast(ReaderEffect.Finished::class, ReaderEffect.Toast::class)
        assertThat(e.effects.filterIsInstance<ReaderEffect.Toast>().single().toast).isEqualTo(ReaderToast.RoutineComplete("Morning"))
    }

    @Test fun doneNextMarksDoneAndReplaces() = runTest(dispatcher) {
        val e = setup(prayerId = 1, routineId = 9)
        advanceUntilIdle()
        e.send(ReaderEvent.DoneNext)
        advanceUntilIdle()
        assertThat(e.routines.done).containsExactly(9L to 1)
        assertThat(e.effects).containsExactly(ReaderEffect.ReplaceWith(2, 9L))
    }

    @Test fun noRoutineMeansNoContext() = runTest(dispatcher) {
        val e = setup()
        advanceUntilIdle()
        assertThat(e.vm.state.value.routineCtx).isNull()
        e.send(ReaderEvent.DoneNext)
        advanceUntilIdle()
        assertThat(e.routines.done).isEmpty()
    }

    @Test fun countIncDecNeverBelowZero() = runTest(dispatcher) {
        val e = setup()
        advanceUntilIdle()
        e.send(ReaderEvent.Dec); advanceUntilIdle()
        assertThat(e.vm.state.value.count).isEqualTo(0)
        e.send(ReaderEvent.Inc); e.send(ReaderEvent.Inc); advanceUntilIdle()
        assertThat(e.vm.state.value.count).isEqualTo(2)
        e.send(ReaderEvent.Dec); advanceUntilIdle()
        assertThat(e.vm.state.value.count).isEqualTo(1)
    }

    @Test fun goalToastExactlyAtGoal() = runTest(dispatcher) {
        val e = setup() // goal = 3
        advanceUntilIdle()
        repeat(2) { e.send(ReaderEvent.Inc); advanceUntilIdle() }
        assertThat(e.effects).isEmpty()
        e.send(ReaderEvent.Inc); advanceUntilIdle()
        assertThat(e.effects.filterIsInstance<ReaderEffect.Toast>().map { it.toast }).containsExactly(ReaderToast.GoalReached(3))
        e.send(ReaderEvent.Inc); advanceUntilIdle()
        assertThat(e.effects).hasSize(1)
        assertThat(e.vm.state.value.counterProgress).isEqualTo(1f)
    }

    @Test fun resetNeedsCountThenUndoRestores() = runTest(dispatcher) {
        val e = setup()
        advanceUntilIdle()
        e.send(ReaderEvent.AskReset); advanceUntilIdle()
        assertThat(e.vm.state.value.confirmReset).isFalse()
        repeat(2) { e.send(ReaderEvent.Inc) }
        advanceUntilIdle()
        e.send(ReaderEvent.AskReset); advanceUntilIdle()
        assertThat(e.vm.state.value.confirmReset).isTrue()
        e.send(ReaderEvent.CancelReset); advanceUntilIdle()
        assertThat(e.vm.state.value.confirmReset).isFalse()
        e.send(ReaderEvent.AskReset); e.send(ReaderEvent.DoReset); advanceUntilIdle()
        assertThat(e.vm.state.value.count).isEqualTo(0)
        assertThat(e.vm.state.value.confirmReset).isFalse()
        val toast = e.effects.filterIsInstance<ReaderEffect.Toast>().single()
        assertThat(toast.toast).isEqualTo(ReaderToast.CountReset)
        toast.undo!!.invoke(); advanceUntilIdle()
        assertThat(e.vm.state.value.count).isEqualTo(2)
    }

    @Test fun toggleSaveWithUndo() = runTest(dispatcher) {
        val e = setup()
        advanceUntilIdle()
        e.send(ReaderEvent.ToggleSave); advanceUntilIdle()
        assertThat(e.vm.state.value.isSaved).isTrue()
        val toast = e.effects.filterIsInstance<ReaderEffect.Toast>().single()
        assertThat(toast.toast).isEqualTo(ReaderToast.AddedToSaved)
        toast.undo!!.invoke(); advanceUntilIdle()
        assertThat(e.vm.state.value.isSaved).isFalse()
    }

    @Test fun scrollProgressThrottled() = runTest(dispatcher) {
        val e = setup()
        advanceUntilIdle()
        e.send(ReaderEvent.ScrollProgress(0.01f)); advanceUntilIdle()
        assertThat(e.prayers.progress).isEmpty()
        assertThat(e.vm.state.value.scrollProgress).isEqualTo(0.01f)
        e.send(ReaderEvent.ScrollProgress(0.03f)); advanceUntilIdle()
        assertThat(e.prayers.progress).containsExactly(0.03f)
        e.send(ReaderEvent.ScrollProgress(0.04f)); advanceUntilIdle()
        assertThat(e.prayers.progress).hasSize(1)
        e.send(ReaderEvent.ScrollProgress(0.99f)); advanceUntilIdle()
        assertThat(e.prayers.progress).containsExactly(0.03f, 0.99f).inOrder()
        assertThat(e.prefs.lastReadSets).isEqualTo(1)
        assertThat(e.prefs.state.value.lastReadPrayerId).isEqualTo(2)
    }

    @Test fun flushPersistsLatest() = runTest(dispatcher) {
        val e = setup()
        advanceUntilIdle()
        e.send(ReaderEvent.ScrollProgress(0.03f))
        e.send(ReaderEvent.ScrollProgress(0.04f)); advanceUntilIdle()
        e.send(ReaderEvent.Flush); advanceUntilIdle()
        assertThat(e.prayers.progress).containsExactly(0.03f, 0.04f).inOrder()
    }

    @Test fun textSizeBounds() = runTest(dispatcher) {
        val e = setup(prefs = RPrefs(UserPrefs(textSize = 30)))
        advanceUntilIdle()
        e.send(ReaderEvent.SizeUp); advanceUntilIdle()
        assertThat(e.vm.state.value.textSizeSp).isEqualTo(32)
        e.send(ReaderEvent.SizeUp); advanceUntilIdle()
        assertThat(e.vm.state.value.textSizeSp).isEqualTo(32)
        repeat(12) { e.send(ReaderEvent.SizeDown); advanceUntilIdle() }
        assertThat(e.vm.state.value.textSizeSp).isEqualTo(16)
    }

    @Test fun appearanceAndGoalPersist() = runTest(dispatcher) {
        val e = setup()
        advanceUntilIdle()
        e.send(ReaderEvent.SetAppearance(Appearance.DARK)); e.send(ReaderEvent.SetGoal(21)); advanceUntilIdle()
        assertThat(e.vm.state.value.appearance).isEqualTo(Appearance.DARK)
        assertThat(e.vm.state.value.goal).isEqualTo(21)
    }

    @Test fun tibetanDigitsInBo() = runTest(dispatcher) {
        val e = setup(routineId = 9, prefs = RPrefs(UserPrefs(appLanguage = AppLanguage.BO, recitationGoal = 108)))
        advanceUntilIdle()
        e.send(ReaderEvent.Inc); advanceUntilIdle()
        val s = e.vm.state.value
        assertThat(s.countText).isEqualTo("༡")
        assertThat(s.goalText).isEqualTo("༡༠༨")
        assertThat(s.minutesText).isEqualTo("༣")
        assertThat(s.routinePositionText).isEqualTo("༢ / ༣")
    }

    @Test fun listenOpensPlayer() = runTest(dispatcher) {
        val e = setup()
        advanceUntilIdle()
        e.send(ReaderEvent.Listen); advanceUntilIdle()
        assertThat(e.effects).containsExactly(ReaderEffect.OpenPlayer)
    }

    @Test fun listenStartsPlaybackWithRoutineNameThenOpensPlayer() = runTest(dispatcher) {
        val e = setup(prayerId = 2, routineId = 9)
        advanceUntilIdle()
        e.send(ReaderEvent.Listen)
        advanceUntilIdle()
        assertThat(e.player.lastPlay?.ids).containsExactly(2)
        assertThat(e.player.lastPlay?.contextLabel).isEqualTo("Morning")
        assertThat(e.player.lastPlay?.routineId).isEqualTo(9L)
        assertThat(e.effects).contains(ReaderEffect.OpenPlayer)
    }

    @Test fun listenOutsideRoutineUsesEnglishTitleAndSkipsPlayWhenAlreadyCurrent() = runTest(dispatcher) {
        val e = setup(prayerId = 2)
        advanceUntilIdle()
        e.send(ReaderEvent.Listen)
        advanceUntilIdle()
        assertThat(e.player.lastPlay?.contextLabel).isEqualTo("Prayer 2")
        e.player.lastPlay = null
        e.effects.clear()
        e.player.mutableState.value = PlayerState(currentPrayerId = 2, queue = listOf(2), isPlaying = true)
        advanceUntilIdle()
        assertThat(e.vm.state.value.isPlayingThis).isTrue()
        e.send(ReaderEvent.Listen)
        advanceUntilIdle()
        assertThat(e.player.lastPlay).isNull()
        assertThat(e.effects).containsExactly(ReaderEffect.OpenPlayer)
    }

    @Test fun offlineListenShowsErrorInsteadOfOpeningPlayer() = runTest(dispatcher) {
        val e = setup(prayerId = 2)
        e.player.playResult = PlaybackError.OFFLINE
        advanceUntilIdle()
        e.send(ReaderEvent.Listen)
        advanceUntilIdle()
        assertThat(e.effects).containsExactly(ReaderEffect.PlayerError(PlaybackError.OFFLINE))
    }
}
