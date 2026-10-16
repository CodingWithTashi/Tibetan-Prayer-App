package com.codingwithtashi.dailyprayer.ui.saved

import com.codingwithtashi.dailyprayer.playback.PlayerState
import com.codingwithtashi.dailyprayer.ui.FakePlayerControllerApi
import com.codingwithtashi.dailyprayer.ui.library.FakeLibraryRepo
import com.codingwithtashi.dailyprayer.ui.library.testPrayer
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

    private fun setup(repo: FakeLibraryRepo): SavedViewModel {
        Dispatchers.setMain(dispatcher)
        return SavedViewModel(repo, player)
    }

    @Test fun labelFallsBackToEnglishTitle() = runTest(dispatcher) {
        val repo = FakeLibraryRepo(listOf(testPrayer(1, "Tara", saved = true), testPrayer(2, "Guru", saved = true, label = "Mine"), testPrayer(3)))
        val vm = setup(repo)
        backgroundScope.launch { vm.state.collect {} }
        advanceTimeBy(1)
        assertThat(vm.state.value.items.map { it.label }).containsExactly("Tara", "Mine").inOrder()
        assertThat(vm.state.value.items.map { it.draftLabel }).containsExactly("", "Mine").inOrder()
    }

    @Test fun renameClampsTo28AndPersistsDebounced() = runTest(dispatcher) {
        val repo = FakeLibraryRepo(listOf(testPrayer(1, saved = true)))
        val vm = setup(repo)
        backgroundScope.launch { vm.state.collect {} }
        vm.setLabel(1, "x".repeat(40))
        advanceTimeBy(100)
        assertThat(repo.prayers.value[0].savedLabel).isNull()
        advanceTimeBy(300)
        assertThat(repo.prayers.value[0].savedLabel).isEqualTo("x".repeat(28))
        vm.setLabel(1, "   ")
        advanceTimeBy(400)
        assertThat(repo.prayers.value[0].savedLabel).isNull()
    }

    @Test fun exitingEditFlushesPendingLabel() = runTest(dispatcher) {
        val repo = FakeLibraryRepo(listOf(testPrayer(1, saved = true)))
        val vm = setup(repo)
        backgroundScope.launch { vm.state.collect {} }
        vm.toggleEdit()
        vm.setLabel(1, "Morning")
        vm.toggleEdit()
        advanceTimeBy(1)
        assertThat(repo.prayers.value[0].savedLabel).isEqualTo("Morning")
    }

    @Test fun removeThenUndoRestoresLabel() = runTest(dispatcher) {
        val repo = FakeLibraryRepo(listOf(testPrayer(1, saved = true, label = "Mine")))
        val vm = setup(repo)
        backgroundScope.launch { vm.state.collect {} }
        val events = mutableListOf<SavedEvent>()
        backgroundScope.launch { vm.events.collect { events += it } }
        vm.remove(1)
        advanceUntilIdle()
        val e = events.single() as SavedEvent.Removed
        assertThat(repo.prayers.value[0].isSaved).isFalse()
        vm.undoRemove(e.id, e.previousLabel)
        advanceUntilIdle()
        assertThat(repo.prayers.value[0].isSaved).isTrue()
        assertThat(repo.prayers.value[0].savedLabel).isEqualTo("Mine")
    }

    @Test fun playPauseDelegatesWithSavedContext() = runTest(dispatcher) {
        val vm = setup(FakeLibraryRepo(listOf(testPrayer(1, saved = true))))
        vm.playPause(1, "Saved")
        assertThat(player.calls).containsExactly("playOrToggle(1,Saved)")
    }

    @Test fun isPlayingFollowsPlayerState() = runTest(dispatcher) {
        val vm = setup(FakeLibraryRepo(listOf(testPrayer(1, saved = true), testPrayer(2, saved = true))))
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
