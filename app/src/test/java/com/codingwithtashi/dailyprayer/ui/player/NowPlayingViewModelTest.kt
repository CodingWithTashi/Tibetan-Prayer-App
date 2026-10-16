package com.codingwithtashi.dailyprayer.ui.player

import com.codingwithtashi.dailyprayer.core.text.PrayerText
import com.codingwithtashi.dailyprayer.data.model.Prayer
import com.codingwithtashi.dailyprayer.data.repository.PrayerRepository
import com.codingwithtashi.dailyprayer.playback.PlaybackError
import com.codingwithtashi.dailyprayer.playback.PlayerState
import com.codingwithtashi.dailyprayer.playback.RepeatMode
import com.codingwithtashi.dailyprayer.ui.FakePlayerControllerApi
import com.codingwithtashi.dailyprayer.ui.routine.testPrayer
import com.codingwithtashi.dailyprayer.ui.today.FakePrefs
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class NowPlayingViewModelTest {
    private val dispatcher = UnconfinedTestDispatcher()
    private val player = FakePlayerControllerApi()
    private val prayers = mockk<PrayerRepository>()
    private val library = MutableStateFlow(
        listOf(
            testPrayer(1, hasAudio = true).copy(audioDurationMs = 100_000L),
            testPrayer(2, hasAudio = true),
            testPrayer(3, hasAudio = true).copy(audioDurationMs = 65_000L),
        ),
    )
    private var text = PrayerText(listOf("v0", "v1", "v2", "v3"), emptyList(), estDurationSec = 80, estMinutes = 2)

    @Before fun setUp() {
        Dispatchers.setMain(dispatcher)
        every { prayers.observe(any()) } answers { library.let { flow -> kotlinx.coroutines.flow.flow { flow.collect { l -> emit(l.firstOrNull { it.id == firstArg<Int>() }) } } } }
        coEvery { prayers.getByIds(any()) } answers { firstArg<List<Int>>().mapNotNull { id -> library.value.firstOrNull { it.id == id } } }
        coEvery { prayers.text(any()) } answers { text }
        coEvery { prayers.setSaved(any(), any()) } answers {
            val id = firstArg<Int>()
            library.value = library.value.map { if (it.id == id) it.copy(isSaved = secondArg()) else it }
        }
    }

    @After fun tearDown() = Dispatchers.resetMain()

    private fun playing(
        position: Long = 0L,
        duration: Long = 100_000L,
        index: Int = 0,
        queue: List<Int> = listOf(1, 2, 3),
    ) = PlayerState(
        currentPrayerId = queue[index], queue = queue, currentIndex = index, isPlaying = true,
        positionMs = position, durationMs = duration, contextLabel = "Morning", connected = true,
    )

    private class Env(val vm: NowPlayingViewModel, val effects: MutableList<NowPlayingEffect>)

    private fun TestScope.vm(initial: PlayerState = playing()): Env {
        player.mutableState.value = initial
        val vm = NowPlayingViewModel(prayers, player, FakePrefs())
        val effects = mutableListOf<NowPlayingEffect>()
        backgroundScope.launch { vm.state.collect { } }
        backgroundScope.launch { vm.effects.collect { effects += it } }
        return Env(vm, effects)
    }

    @Test fun mapsPlayerStateAndLabels() = runTest(dispatcher) {
        val s = vm(playing(position = 60_000)).vm.state.value
        assertThat(s.prayer?.id).isEqualTo(1)
        assertThat(s.contextLabel).isEqualTo("Morning")
        assertThat(s.verses).containsExactly("v0", "v1", "v2", "v3").inOrder()
        assertThat(s.posLabel).isEqualTo("1:00")
        assertThat(s.durLabel).isEqualTo("1:40")
        assertThat(s.progress).isWithin(0.001f).of(0.6f)
        assertThat(s.isPlaying).isTrue()
    }

    @Test fun verseIndexFollowsPosition() = runTest(dispatcher) {
        val e = vm(playing(position = 0))
        assertThat(e.vm.state.value.verseIndex).isEqualTo(0)
        player.mutableState.value = playing(position = 60_000)
        assertThat(e.vm.state.value.verseIndex).isEqualTo(2)
        player.mutableState.value = playing(position = 99_999)
        assertThat(e.vm.state.value.verseIndex).isEqualTo(3)
    }

    @Test fun durationFallsBackToPrayerThenEstimate() = runTest(dispatcher) {
        val e = vm(playing(duration = 0))
        assertThat(e.vm.state.value.durLabel).isEqualTo("1:40") // prayer 1 audioDurationMs
        player.mutableState.value = playing(duration = 0, index = 1)
        assertThat(e.vm.state.value.durLabel).isEqualTo("1:20") // estimate 80 s
    }

    @Test fun queueMappingMarksCurrent() = runTest(dispatcher) {
        val e = vm(playing(index = 1))
        val q = e.vm.state.value.queue
        assertThat(q.map { it.id }).containsExactly(1, 2, 3).inOrder()
        assertThat(q.map { it.index }).containsExactly(0, 1, 2).inOrder()
        assertThat(q.map { it.isCurrent }).containsExactly(false, true, false).inOrder()
        assertThat(q.map { it.durLabel }).containsExactly("1:40", "1:20", "1:05").inOrder()
        assertThat(q[0].en).isEqualTo("En 1")
        assertThat(q[0].tib).isEqualTo("tib1")
    }

    @Test fun controlsDelegateToPlayer() = runTest(dispatcher) {
        val e = vm()
        with(e.vm) {
            onEvent(NowPlayingEvent.PlayPause)
            onEvent(NowPlayingEvent.Prev)
            onEvent(NowPlayingEvent.Next)
            onEvent(NowPlayingEvent.CycleSpeed)
            onEvent(NowPlayingEvent.CycleRepeat)
            onEvent(NowPlayingEvent.Seek(0.5f))
        }
        assertThat(player.calls).containsExactly(
            "togglePlayPause", "previous", "next", "cycleSpeed", "cycleRepeat", "seekToFraction(0.5)",
        ).inOrder()
    }

    @Test fun queueSheetOpensAndPlayingAnItemClosesIt() = runTest(dispatcher) {
        val e = vm()
        e.vm.onEvent(NowPlayingEvent.OpenQueue)
        assertThat(e.vm.state.value.showQueue).isTrue()
        e.vm.onEvent(NowPlayingEvent.PlayQueueItem(2))
        assertThat(player.calls).containsExactly("skipToQueueIndex(2)")
        assertThat(e.vm.state.value.showQueue).isFalse()
        e.vm.onEvent(NowPlayingEvent.OpenQueue)
        e.vm.onEvent(NowPlayingEvent.CloseQueue)
        assertThat(e.vm.state.value.showQueue).isFalse()
    }

    @Test fun speedAndRepeatLabels() {
        assertThat(listOf(0.8f, 1f, 1.25f).map { speedLabel(it) }).containsExactly("0.8×", "1×", "1.25×").inOrder()
        assertThat(RepeatMode.entries.map { repeatLabel(it) }).containsExactly(null, "×3", "×7", "∞").inOrder()
    }

    @Test fun stateExposesSpeedAndRepeat() = runTest(dispatcher) {
        val e = vm(playing().copy(speed = 1.25f, repeatMode = RepeatMode.SEVEN, repeatsLeft = 5))
        assertThat(e.vm.state.value.speedLabel).isEqualTo("1.25×")
        assertThat(e.vm.state.value.repeatLabel).isEqualTo("×7")
        assertThat(e.vm.state.value.repeatsLeft).isEqualTo(5)
    }

    @Test fun seekToVerseSpreadsEvenlyWhenNoTimestamps() = runTest(dispatcher) {
        val e = vm(playing(duration = 100_000))
        e.vm.onEvent(NowPlayingEvent.SeekToVerse(2))
        assertThat(player.calls).containsExactly("seekTo(50500)")
    }

    @Test fun seekToVerseUsesTimestampsWhenAvailable() = runTest(dispatcher) {
        text = text.copy(verseStartMs = listOf(0L, 10_000L, 33_000L, 70_000L))
        val e = vm()
        e.vm.onEvent(NowPlayingEvent.SeekToVerse(3))
        e.vm.onEvent(NowPlayingEvent.SeekToVerse(9)) // out of range: ignored
        assertThat(player.calls).containsExactly("seekTo(70000)")
    }

    @Test fun toggleSaveFlipsAndOffersUndo() = runTest(dispatcher) {
        val e = vm()
        e.vm.onEvent(NowPlayingEvent.ToggleSave)
        coVerify { prayers.setSaved(1, true) }
        assertThat(e.vm.state.value.isSaved).isTrue()
        val effect = e.effects.single() as NowPlayingEffect.Saved
        assertThat(effect.saved).isTrue()
        effect.undo()
        coVerify { prayers.setSaved(1, false) }
        assertThat(e.vm.state.value.isSaved).isFalse()
    }

    @Test fun errorsAndClearedPlaybackBecomeEffects() = runTest(dispatcher) {
        val e = vm()
        player.mutableErrors.emit(PlaybackError.OFFLINE)
        assertThat(e.effects).containsExactly(NowPlayingEffect.Error(PlaybackError.OFFLINE))
        player.mutableState.value = PlayerState()
        assertThat(e.effects.last()).isEqualTo(NowPlayingEffect.Close)
    }

    @Test fun neverOpenedWithMediaDoesNotAutoClose() = runTest(dispatcher) {
        val e = vm(PlayerState())
        assertThat(e.effects).isEmpty()
        assertThat(e.vm.state.value.prayer).isNull()
    }
}
