package com.codingwithtashi.dailyprayer.ui.listen

import com.codingwithtashi.dailyprayer.core.locale.AppLanguage
import com.codingwithtashi.dailyprayer.core.text.PrayerText
import com.codingwithtashi.dailyprayer.data.model.DownloadError
import com.codingwithtashi.dailyprayer.data.model.DownloadState
import com.codingwithtashi.dailyprayer.data.prefs.UserPrefs
import com.codingwithtashi.dailyprayer.data.repository.PrayerRepository
import com.codingwithtashi.dailyprayer.navigation.ListenFilter
import com.codingwithtashi.dailyprayer.playback.PlayerState
import com.codingwithtashi.dailyprayer.ui.FakeDownloadRepository
import com.codingwithtashi.dailyprayer.ui.FakePlayerControllerApi
import com.codingwithtashi.dailyprayer.ui.components.DownloadUiState
import com.codingwithtashi.dailyprayer.ui.routine.testPrayer
import com.codingwithtashi.dailyprayer.ui.today.FakePrefs
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
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
class ListenViewModelTest {
    private val dispatcher = UnconfinedTestDispatcher()
    private val library = MutableStateFlow(
        listOf(
            testPrayer(1, hasAudio = true).copy(audioDurationMs = 222_000L),
            testPrayer(2, hasAudio = true),
            testPrayer(3, hasAudio = true),
        ),
    )
    private val prayers = mockk<PrayerRepository>()
    private val downloads = FakeDownloadRepository()
    private val player = FakePlayerControllerApi()
    private val prefs = FakePrefs()

    @Before fun setUp() {
        Dispatchers.setMain(dispatcher)
        every { prayers.observeWithAudio() } returns library
        coEvery { prayers.text(any()) } returns PrayerText(emptyList(), emptyList(), 75, 2)
    }

    @After fun tearDown() = Dispatchers.resetMain()

    private class Env(val vm: ListenViewModel, val effects: MutableList<ListenEffect>)

    private fun TestScope.vm(): Env {
        val vm = ListenViewModel(prayers, downloads, player, prefs)
        val effects = mutableListOf<ListenEffect>()
        backgroundScope.launch { vm.state.collect { } }
        backgroundScope.launch { vm.effects.collect { effects += it } }
        return Env(vm, effects)
    }

    @Test fun metaUsesRealDurationThenEstimateAndTibetanTitle() = runTest(dispatcher) {
        val items = vm().vm.state.value.items
        assertThat(items.map { it.durationLabel }).containsExactly("3:42", "1:15", "1:15").inOrder()
        assertThat(items.map { it.metaKind }).containsExactly(MetaKind.TIBETAN, MetaKind.TIBETAN, MetaKind.TIBETAN)
        assertThat(items[0].tib).isEqualTo("tib1")
    }

    @Test fun metaKindsFollowDownloadStateWithLocalisedPercent() = runTest(dispatcher) {
        val e = vm()
        downloads.stateFlow(1).value = DownloadState.Downloaded
        downloads.stateFlow(2).value = DownloadState.Downloading(0.42f, 42, 100)
        downloads.stateFlow(3).value = DownloadState.Failed(DownloadError.NETWORK)
        val items = e.vm.state.value.items
        assertThat(items.map { it.metaKind })
            .containsExactly(MetaKind.DOWNLOADED, MetaKind.DOWNLOADING, MetaKind.TIBETAN).inOrder()
        assertThat(items[1].percentLabel).isEqualTo("42%")
        assertThat(items[1].download).isEqualTo(DownloadUiState.Downloading(0.42f))
        assertThat(items[2].download).isEqualTo(DownloadUiState.NotDownloaded)
        prefs.state.value = UserPrefs(appLanguage = AppLanguage.BO)
        assertThat(e.vm.state.value.items[1].percentLabel).isEqualTo("༤༢%")
    }

    @Test fun downloadedFilterKeepsOnlyDownloadedItems() = runTest(dispatcher) {
        val e = vm()
        downloads.stateFlow(2).value = DownloadState.Downloaded
        downloads.count.value = 1
        downloads.bytes.value = 4_500_000L
        assertThat(e.vm.state.value.items).hasSize(3)
        e.vm.setFilter(ListenFilter.DOWNLOADED)
        assertThat(e.vm.state.value.items.map { it.id }).containsExactly(2)
        assertThat(e.vm.state.value.downloadedCount).isEqualTo(1)
        assertThat(e.vm.state.value.storageLabel).isEqualTo("4.5 MB")
        e.vm.setFilter(ListenFilter.ALL)
        assertThat(e.vm.state.value.items).hasSize(3)
    }

    @Test fun currentAndPlayingFlagsFollowPlayer() = runTest(dispatcher) {
        val e = vm()
        player.mutableState.value = PlayerState(currentPrayerId = 2, queue = listOf(2), isPlaying = true)
        assertThat(e.vm.state.value.items.map { it.isCurrent }).containsExactly(false, true, false).inOrder()
        assertThat(e.vm.state.value.items.map { it.isPlaying }).containsExactly(false, true, false).inOrder()
        e.vm.onEvent(ListenEvent.PlayPause(2, "Listen"))
        assertThat(player.calls).containsExactly("playOrToggle(2,Listen)")
    }

    @Test fun downloadToggleStartsAndCancels() = runTest(dispatcher) {
        val e = vm()
        e.vm.onEvent(ListenEvent.DownloadToggle(1))
        downloads.stateFlow(1).value = DownloadState.Downloading(0.1f, 1, 10)
        e.vm.onEvent(ListenEvent.DownloadToggle(1))
        downloads.stateFlow(2).value = DownloadState.Failed(DownloadError.NETWORK)
        e.vm.onEvent(ListenEvent.DownloadToggle(2))
        assertThat(downloads.calls).containsExactly("download(1)", "cancel(1)", "download(2)").inOrder()
    }

    @Test fun removingADownloadIsDeferredUntilTheToastClosesWithoutUndo() = runTest(dispatcher) {
        val e = vm()
        downloads.stateFlow(1).value = DownloadState.Downloaded
        e.vm.onEvent(ListenEvent.DownloadToggle(1))
        assertThat(e.effects).containsExactly(ListenEffect.ConfirmRemoveDownload(1))
        assertThat(downloads.calls).isEmpty()
        assertThat(e.vm.state.value.items[0].download).isEqualTo(DownloadUiState.NotDownloaded)

        // Undo: row comes back, closing the toast afterwards deletes nothing.
        e.vm.onEvent(ListenEvent.UndoRemove(1))
        e.vm.onEvent(ListenEvent.CommitRemove(1))
        assertThat(downloads.calls).isEmpty()
        assertThat(e.vm.state.value.items[0].download).isEqualTo(DownloadUiState.Downloaded)

        // No undo: the file is deleted when the toast closes.
        e.vm.onEvent(ListenEvent.DownloadToggle(1))
        e.vm.onEvent(ListenEvent.CommitRemove(1))
        assertThat(downloads.calls).containsExactly("delete(1)")
        assertThat(e.vm.state.value.items[0].download).isEqualTo(DownloadUiState.NotDownloaded)
    }

    @Test fun downloadFailuresBecomeToastsButOldOnesAreIgnored() = runTest(dispatcher) {
        downloads.stateFlow(3).value = DownloadState.Failed(DownloadError.NETWORK) // before the screen existed
        val e = vm()
        assertThat(e.effects).isEmpty()
        downloads.stateFlow(1).value = DownloadState.Failed(DownloadError.OFFLINE)
        downloads.stateFlow(2).value = DownloadState.Failed(DownloadError.DISK)
        downloads.stateFlow(3).value = DownloadState.Failed(DownloadError.CANCELLED)
        assertThat(e.effects).containsExactly(
            ListenEffect.Toast(ListenToast.OFFLINE_CANNOT_STREAM),
            ListenEffect.Toast(ListenToast.DOWNLOAD_FAILED),
        ).inOrder()
    }
}
