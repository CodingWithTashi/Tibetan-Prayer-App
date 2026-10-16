package com.codingwithtashi.dailyprayer.ui.saved

import com.codingwithtashi.dailyprayer.playback.PlayerState
import com.codingwithtashi.dailyprayer.ui.FakePlayerControllerApi
import com.codingwithtashi.dailyprayer.ui.FakePrayerRepo
import com.codingwithtashi.dailyprayer.ui.testPrayer
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SavedViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val player = FakePlayerControllerApi()

    @After fun tearDown() = Dispatchers.resetMain()

    private fun setup(repo: FakePrayerRepo): SavedViewModel {
        Dispatchers.setMain(dispatcher)
        return SavedViewModel(repo, player)
    }

    @Test fun labelFallsBackToEnglishTitle() = runTest(dispatcher) {
        val repo = FakePrayerRepo(listOf(testPrayer(1, "Tara", isSaved = true), testPrayer(2, "Guru", isSaved = true, savedLabel = "Mine"), testPrayer(3)))
        val vm = setup(repo)
        backgroundScope.launch { vm.state.collect {} }
        advanceTimeBy(1)
        assertThat(vm.state.value.items.map { it.label }).containsExactly("Tara", "Mine").inOrder()
        assertThat(vm.state.value.items.map { it.draftLabel }).containsExactly("", "Mine").inOrder()
    }

    @Test fun renameClampsTo28AndPersistsDebounced() = runTest(dispatcher) {
        val repo = FakePrayerRepo(listOf(testPrayer(1, isSaved = true)))
        val vm = setup(repo)
        backgroundScope.launch { vm.state.collect {} }
        vm.onEvent(SavedEvent.SetLabel(1, "x".repeat(40)))
        advanceTimeBy(100)
        assertThat(repo.all.value[0].savedLabel).isNull()
        advanceTimeBy(300)
        assertThat(repo.all.value[0].savedLabel).isEqualTo("x".repeat(28))
        vm.onEvent(SavedEvent.SetLabel(1, "   "))
        advanceTimeBy(400)
        assertThat(repo.all.value[0].savedLabel).isNull()
    }

    @Test fun exitingEditFlushesPendingLabel() = runTest(dispatcher) {
        val repo = FakePrayerRepo(listOf(testPrayer(1, isSaved = true)))
        val vm = setup(repo)
        backgroundScope.launch { vm.state.collect {} }
        vm.onEvent(SavedEvent.ToggleEdit)
        vm.onEvent(SavedEvent.SetLabel(1, "Morning"))
        vm.onEvent(SavedEvent.ToggleEdit)
        advanceTimeBy(1)
        assertThat(repo.all.value[0].savedLabel).isEqualTo("Morning")
    }

    @Test fun removeThenUndoRestoresLabel() = runTest(dispatcher) {
        val repo = FakePrayerRepo(listOf(testPrayer(1, isSaved = true, savedLabel = "Mine")))
        val vm = setup(repo)
        backgroundScope.launch { vm.state.collect {} }
        val effects = mutableListOf<SavedEffect>()
        backgroundScope.launch { vm.effects.collect { effects += it } }
        vm.onEvent(SavedEvent.Remove(1))
        advanceUntilIdle()
        val e = effects.single() as SavedEffect.Removed
        assertThat(repo.all.value[0].isSaved).isFalse()
        vm.onEvent(SavedEvent.UndoRemove(e.id, e.previousLabel))
        advanceUntilIdle()
        assertThat(repo.all.value[0].isSaved).isTrue()
        assertThat(repo.all.value[0].savedLabel).isEqualTo("Mine")
    }

    @Test fun playPauseDelegatesWithSavedContext() = runTest(dispatcher) {
        val vm = setup(FakePrayerRepo(listOf(testPrayer(1, isSaved = true))))
        vm.onEvent(SavedEvent.PlayPause(1, "Saved"))
        assertThat(player.calls).containsExactly("playOrToggle(1,Saved)")
    }

    @Test fun isPlayingFollowsPlayerState() = runTest(dispatcher) {
        val vm = setup(FakePrayerRepo(listOf(testPrayer(1, isSaved = true), testPrayer(2, isSaved = true))))
        backgroundScope.launch { vm.state.collect {} }
        advanceTimeBy(1)
        assertThat(vm.state.value.items.map { it.isPlaying }).containsExactly(false, false)
        player.mutableState.value = PlayerState(currentPrayerId = 2, queue = listOf(2), isPlaying = true)
        advanceTimeBy(1)
        assertThat(vm.state.value.items.map { it.isPlaying }).containsExactly(false, true).inOrder()
        player.mutableState.value = PlayerState(currentPrayerId = 2, queue = listOf(2), isPlaying = false)
        advanceTimeBy(1)
        assertThat(vm.state.value.items.map { it.isPlaying }).containsExactly(false, false)
    }
}
