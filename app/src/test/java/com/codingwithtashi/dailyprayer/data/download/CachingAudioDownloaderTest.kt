package com.codingwithtashi.dailyprayer.data.download

import android.content.Context
import android.net.Uri
import androidx.media3.common.PlaybackException
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.HttpDataSource
import androidx.test.core.app.ApplicationProvider
import com.codingwithtashi.dailyprayer.data.model.DownloadError
import com.codingwithtashi.dailyprayer.playback.FakeUpstream
import com.codingwithtashi.dailyprayer.playback.Media3AudioCache
import com.google.common.truth.Truth.assertThat
import java.io.File
import java.io.IOException
import java.net.UnknownHostException
import java.nio.file.Files
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@androidx.annotation.OptIn(markerClass = [UnstableApi::class])
@RunWith(RobolectricTestRunner::class)
class CachingAudioDownloaderTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val data = ByteArray(400_000) { (it * 7).toByte() }
    private val root: File = Files.createTempDirectory("dl").toFile()
    private val url = "https://example.com/a.mp3?alt=media&token=t"
    private var cache: Media3AudioCache? = null

    @After fun tearDown() { cache?.release(); root.deleteRecursively() }

    private fun setUp(upstream: FakeUpstream) =
        Media3AudioCache(context, File(root, "cache"), upstream, 10L * 1024 * 1024).also { cache = it }

    private fun assertNoLeftovers(dest: File) {
        assertThat(dest.exists()).isFalse()
        assertThat(File(dest.parentFile, dest.name + ".part").exists()).isFalse()
    }

    private fun streamFully(c: Media3AudioCache, maxBytes: Int = Int.MAX_VALUE) {
        val s = c.playerDataSourceFactory().createDataSource()
        try {
            s.open(DataSpec(Uri.parse(url)))
            val buf = ByteArray(8 * 1024)
            var total = 0
            while (total < maxBytes) {
                val n = s.read(buf, 0, buf.size)
                if (n < 0) break
                total += n
            }
        } finally { s.close() }
    }

    @Test fun downloadsAndCaches() = runBlocking {
        val up = FakeUpstream(data)
        val dl = CachingAudioDownloader(setUp(up))
        val dest = File(root, "out/a.mp3")
        val progress = dl.download(url, dest).toList()
        assertThat(dest.readBytes()).isEqualTo(data)
        assertThat(File(dest.parentFile, "a.mp3.part").exists()).isFalse()
        assertThat(progress.last()).isEqualTo(DownloadProgress(data.size.toLong(), data.size.toLong()))
        val served = up.bytesServed.get()
        val dest2 = File(root, "out/b.mp3")
        dl.download(url, dest2).toList()
        assertThat(dest2.readBytes()).isEqualTo(data)
        assertThat(up.bytesServed.get()).isEqualTo(served)
    }

    @Test fun streamedThenDownloadedHitsNoNetwork() = runBlocking {
        val up = FakeUpstream(data)
        val c = setUp(up)
        streamFully(c)
        val served = up.bytesServed.get()
        val dest = File(root, "out/a.mp3")
        CachingAudioDownloader(c).download(url, dest).toList()
        assertThat(dest.readBytes()).isEqualTo(data)
        assertThat(up.bytesServed.get()).isEqualTo(served)
    }

    @Test fun halfStreamedThenDownloadFetchesOnlyRemainder() = runBlocking {
        val up = FakeUpstream(data)
        val c = setUp(up)
        streamFully(c, maxBytes = data.size / 2)
        val before = up.bytesServed.get()
        val dest = File(root, "out/a.mp3")
        CachingAudioDownloader(c).download(url, dest).toList()
        assertThat(dest.readBytes()).isEqualTo(data)
        val extra = up.bytesServed.get() - before
        assertThat(extra).isLessThan(data.size.toLong())
        assertThat(up.bytesServed.get()).isAtMost(data.size.toLong() + 128 * 1024)
    }

    private fun failureOf(e: IOException): DownloadError {
        val dest = File(root, "out/a.mp3")
        val dl = CachingAudioDownloader(setUp(FakeUpstream(data, failure = e)))
        val ex = runCatching { runBlocking { dl.download(url, dest).toList() } }.exceptionOrNull()
        assertThat(ex).isInstanceOf(DownloadException::class.java)
        assertNoLeftovers(dest)
        return (ex as DownloadException).error
    }

    private fun httpError(code: Int) = HttpDataSource.InvalidResponseCodeException(
        code, null, null, emptyMap(), DataSpec(Uri.parse(url)), ByteArray(0),
    )

    @Test fun notFoundIsUnknown() = assertThat(failureOf(httpError(404))).isEqualTo(DownloadError.UNKNOWN)

    @Test fun serverErrorIsNetwork() = assertThat(failureOf(httpError(503))).isEqualTo(DownloadError.NETWORK)

    @Test fun unknownHostIsNetwork() {
        val e = HttpDataSource.HttpDataSourceException(
            UnknownHostException("x"), DataSpec(Uri.parse(url)),
            PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED, HttpDataSource.HttpDataSourceException.TYPE_OPEN,
        )
        assertThat(failureOf(e)).isEqualTo(DownloadError.NETWORK)
    }

    @Test fun cancellationLeavesNoPart() = runBlocking {
        val dest = File(root, "out/a.mp3")
        val dl = CachingAudioDownloader(setUp(FakeUpstream(data, readDelayMs = 20)))
        val started = CompletableDeferred<Unit>()
        val job = launch(Dispatchers.Default) {
            dl.download(url, dest).collect {
                started.complete(Unit)
            }
        }
        withTimeout(10_000) { started.await() }
        job.cancel()
        job.join()
        assertNoLeftovers(dest)
    }
}
