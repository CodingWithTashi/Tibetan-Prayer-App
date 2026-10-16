package com.codingwithtashi.dailyprayer.playback

import android.content.Context
import android.net.Uri
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSpec
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import java.io.File
import java.nio.file.Files
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@androidx.annotation.OptIn(markerClass = [UnstableApi::class])
@RunWith(RobolectricTestRunner::class)
class Media3AudioCacheTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val data = ByteArray(300_000) { (it * 31).toByte() }
    private val upstream = FakeUpstream(data)
    private val dir: File = Files.createTempDirectory("audiocache").toFile()
    private lateinit var cache: Media3AudioCache
    private val url = "https://example.com/a.mp3?alt=media&token=t"

    @Before fun setUp() { cache = Media3AudioCache(context, dir, upstream, 10L * 1024 * 1024) }

    @After fun tearDown() { cache.release(); dir.deleteRecursively() }

    private fun read(source: DataSource, uri: Uri, maxBytes: Long = Long.MAX_VALUE): ByteArray {
        val out = java.io.ByteArrayOutputStream()
        try {
            source.open(DataSpec(uri))
            val buf = ByteArray(8 * 1024)
            while (out.size() < maxBytes) {
                val n = source.read(buf, 0, buf.size)
                if (n < 0) break
                out.write(buf, 0, n)
            }
        } finally { source.close() }
        return out.toByteArray()
    }

    @Test fun secondReadServedFromCache() {
        val first = read(cache.playerDataSourceFactory().createDataSource(), Uri.parse(url))
        val served = upstream.bytesServed.get()
        val second = read(cache.playerDataSourceFactory().createDataSource(), Uri.parse(url))
        assertThat(first).isEqualTo(data)
        assertThat(second).isEqualTo(data)
        assertThat(served).isEqualTo(data.size.toLong())
        assertThat(upstream.bytesServed.get()).isEqualTo(served)
    }

    @Test fun isFullyCachedTracksReads() {
        assertThat(cache.isFullyCached(url)).isFalse()
        read(cache.playerDataSourceFactory().createDataSource(), Uri.parse(url))
        assertThat(cache.isFullyCached(url)).isTrue()
        assertThat(cache.isFullyCached("")).isFalse()
    }

    @Test fun halfReadIsNotFullyCached() {
        read(cache.playerDataSourceFactory().createDataSource(), Uri.parse(url), maxBytes = data.size / 2L)
        assertThat(cache.isFullyCached(url)).isFalse()
    }

    @Test fun fileUriBypassesCache() {
        val f = File(dir.parentFile, "local-${System.nanoTime()}.mp3").apply { writeBytes(data) }
        try {
            val bytes = read(cache.playerDataSourceFactory().createDataSource(), Uri.fromFile(f))
            assertThat(bytes).isEqualTo(data)
            assertThat(upstream.opens.get()).isEqualTo(0)
            assertThat(cache.isFullyCached(Uri.fromFile(f).toString())).isFalse()
        } finally { f.delete() }
    }
}
