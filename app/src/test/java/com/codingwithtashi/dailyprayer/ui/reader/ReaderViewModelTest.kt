package com.codingwithtashi.dailyprayer.ui.reader

import androidx.lifecycle.SavedStateHandle
import com.codingwithtashi.dailyprayer.data.model.Appearance
import com.codingwithtashi.dailyprayer.core.locale.AppLanguage
import com.codingwithtashi.dailyprayer.data.model.Routine
import com.codingwithtashi.dailyprayer.data.prefs.UserPrefs
import com.codingwithtashi.dailyprayer.playback.PlaybackError
import com.codingwithtashi.dailyprayer.playback.PlayerState
import com.codingwithtashi.dailyprayer.ui.FakeClock
import com.codingwithtashi.dailyprayer.ui.FakePlayerControllerApi
import com.codingwithtashi.dailyprayer.ui.FakePrayerRepo
import com.codingwithtashi.dailyprayer.ui.FakePrefs
import com.codingwithtashi.dailyprayer.ui.FakeRoutineRepo
import com.codingwithtashi.dailyprayer.ui.testPrayer
import com.google.common.truth.Truth.assertThat
import java.time.DayOfWeek
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Test

private fun prayer(id: Int) = testPrayer(id, hasAudio = true)

@OptIn(ExperimentalCoroutinesApi::class)
class ReaderViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @After fun tearDown() = Dispatchers.resetMain()

    private class Env(
        val vm: ReaderViewModel,
        val prayers: FakePrayerRepo,
        val routines: FakeRoutineRepo,
        val prefs: FakePrefs,
        val effects: MutableList<ReaderEffect>,
        val player: FakePlayerControllerApi,
    )

    private fun TestScope.setup(
        prayerId: Int = 2,
        routineId: Long? = null,
        routinePrayers: List<Int> = listOf(1, 2, 3),
        prefs: FakePrefs = FakePrefs(UserPrefs(recitationGoal = 3)),
    ): Env {
        Dispatchers.setMain(dispatcher)
        val prayers = FakePrayerRepo((1..3).map { prayer(it) })
        val routine = routineId?.let {
            Routine(it, "Morning", 480, DayOfWeek.entries.toSet(), true, routinePrayers.map { id -> prayer(id) }, 0L)
        }
        val routines = FakeRoutineRepo(listOfNotNull(routine))
        val handle = SavedStateHandle(buildMap {
            put("prayerId", prayerId)
            if (routineId != null) put("routineId", routineId)
        })
        val appScope = CoroutineScope(dispatcher + SupervisorJob())
        val player = FakePlayerControllerApi()
        val vm = ReaderViewModel(handle, prayers, routines, prefs, FakeClock(), appScope, player)
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
        val e = setup(prefs = FakePrefs(UserPrefs(textSize = 34)))
        advanceUntilIdle()
        e.send(ReaderEvent.SizeUp); advanceUntilIdle()
        assertThat(e.vm.state.value.textSizeSp).isEqualTo(36)
        e.send(ReaderEvent.SizeUp); advanceUntilIdle()
        assertThat(e.vm.state.value.textSizeSp).isEqualTo(36)
        repeat(12) { e.send(ReaderEvent.SizeDown); advanceUntilIdle() }
        assertThat(e.vm.state.value.textSizeSp).isEqualTo(18)
    }

    @Test fun appearanceAndGoalPersist() = runTest(dispatcher) {
        val e = setup()
        advanceUntilIdle()
        e.send(ReaderEvent.SetAppearance(Appearance.DARK)); e.send(ReaderEvent.SetGoal(21)); advanceUntilIdle()
        assertThat(e.vm.state.value.appearance).isEqualTo(Appearance.DARK)
        assertThat(e.vm.state.value.goal).isEqualTo(21)
    }

    @Test fun tibetanDigitsInBo() = runTest(dispatcher) {
        val e = setup(routineId = 9, prefs = FakePrefs(UserPrefs(appLanguage = AppLanguage.BO, recitationGoal = 108)))
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
