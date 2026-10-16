package com.codingwithtashi.dailyprayer.data.download

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import app.cash.turbine.test
import com.codingwithtashi.dailyprayer.core.network.ConnectivityChecker
import com.codingwithtashi.dailyprayer.data.model.DownloadError
import com.codingwithtashi.dailyprayer.data.model.DownloadState
import com.codingwithtashi.dailyprayer.data.model.Prayer
import com.codingwithtashi.dailyprayer.data.repository.DownloadRepositoryImpl
import com.codingwithtashi.dailyprayer.data.repository.PrayerRepository
import com.codingwithtashi.dailyprayer.ui.routine.testPrayer
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import java.io.File
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

private class FakeDownloader : AudioDownloader {
    var gate = CompletableDeferred<Unit>()
    var failure: DownloadError? = null
    override fun download(url: String, dest: File): Flow<DownloadProgress> = flow {
        emit(DownloadProgress(50, 100))
        gate.await()
        failure?.let { throw DownloadException(it) }
        dest.parentFile?.mkdirs()
        dest.writeBytes(ByteArray(100))
        emit(DownloadProgress(100, 100))
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class DownloadRepositoryImplTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val dispatcher = UnconfinedTestDispatcher()
    private val downloader = FakeDownloader()
    private val prayer = MutableStateFlow<Prayer?>(testPrayer(1, hasAudio = true))
    private var online = true
    private lateinit var repo: DownloadRepositoryImpl

    @Before fun setUp() {
        val prayers = mockk<PrayerRepository>()
        every { prayers.observe(1) } returns prayer
        coEvery { prayers.get(1) } answers { prayer.value }
        coEvery { prayers.setDownloaded(1, any(), any()) } answers {
            prayer.value = prayer.value!!.copy(isDownloaded = true, audioPath = secondArg())
        }
        coEvery { prayers.clearDownloaded(1) } answers {
            prayer.value = prayer.value!!.copy(isDownloaded = false, audioPath = null)
        }
        val connectivity = object : ConnectivityChecker { override fun isOnline() = online }
        repo = DownloadRepositoryImpl(
            prayers, downloader, AudioFileStore(context), connectivity,
            CoroutineScope(SupervisorJob() + dispatcher), dispatcher,
        )
    }

    @Test fun successTransitions() = runTest {
        repo.state(1).test {
            assertThat(awaitItem()).isEqualTo(DownloadState.NotDownloaded)
            repo.download(1)
            var item = awaitItem()
            assertThat(item).isInstanceOf(DownloadState.Downloading::class.java)
            while ((item as DownloadState.Downloading).progress < 0.5f) item = awaitItem()
            assertThat(item.progress).isEqualTo(0.5f)
            downloader.gate.complete(Unit)
            assertThat(awaitItem()).isEqualTo(DownloadState.Downloaded)
            cancelAndIgnoreRemainingEvents()
        }
        assertThat(prayer.value!!.isDownloaded).isTrue()
    }

    @Test fun networkFailure() = runTest {
        downloader.failure = DownloadError.NETWORK
        downloader.gate.complete(Unit)
        repo.download(1)
        assertThat(repo.stateNow()).isEqualTo(DownloadState.Failed(DownloadError.NETWORK))
    }

    @Test fun offlineFails() = runTest {
        online = false
        repo.download(1)
        assertThat(repo.stateNow()).isEqualTo(DownloadState.Failed(DownloadError.OFFLINE))
    }

    @Test fun cancelReturnsToNotDownloaded() = runTest {
        repo.download(1)
        assertThat(repo.stateNow()).isInstanceOf(DownloadState.Downloading::class.java)
        repo.cancel(1)
        assertThat(repo.stateNow()).isEqualTo(DownloadState.NotDownloaded)
    }

    @Test fun deleteClearsDownload() = runTest {
        downloader.gate.complete(Unit)
        repo.download(1)
        assertThat(repo.stateNow()).isEqualTo(DownloadState.Downloaded)
        val path = prayer.value!!.audioPath!!
        assertThat(File(path).exists()).isTrue()
        repo.delete(1)
        assertThat(File(path).exists()).isFalse()
        assertThat(repo.stateNow()).isEqualTo(DownloadState.NotDownloaded)
    }

    @Test fun storageLabelFormatting() {
        assertThat(repo.storageLabel(0)).isEqualTo("0.0 MB")
        assertThat(repo.storageLabel(400_000)).isEqualTo("0.4 MB")
        assertThat(repo.storageLabel((12.3 * 1024 * 1024).toLong())).isEqualTo("12.3 MB")
    }

    private suspend fun DownloadRepositoryImpl.stateNow(): DownloadState {
        var s: DownloadState? = null
        state(1).test { s = awaitItem(); cancelAndIgnoreRemainingEvents() }
        return s!!
    }
}
