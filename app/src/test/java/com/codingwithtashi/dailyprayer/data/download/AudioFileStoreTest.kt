package com.codingwithtashi.dailyprayer.data.download

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.codingwithtashi.dailyprayer.ui.routine.testPrayer
import com.google.common.truth.Truth.assertThat
import java.io.File
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AudioFileStoreTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val store = AudioFileStore(context)
    private val base = "https://firebasestorage.googleapis.com/v0/b/x/o/prayer%2F"

    @Test fun extensionsFromRealUrls() {
        assertThat(AudioFileStore.extensionOf("${base}gyuchak.m4a?alt=media&token=abc")).isEqualTo("m4a")
        assertThat(AudioFileStore.extensionOf("${base}kapsumpa.mp3?alt=media&token=abc")).isEqualTo("mp3")
        assertThat(AudioFileStore.extensionOf("https://x/o/desheringa.mp3?alt=media")).isEqualTo("mp3")
    }

    @Test fun noOrUnknownExtensionDefaultsToMp3() {
        assertThat(AudioFileStore.extensionOf("https://x/audio?alt=media.m4a")).isEqualTo("mp3")
        assertThat(AudioFileStore.extensionOf("https://x/audio.ogg")).isEqualTo("mp3")
        assertThat(AudioFileStore.extensionOf("")).isEqualTo("mp3")
    }

    @Test fun fileNameUsesSlugOrId() {
        assertThat(AudioFileStore.fileName(3, "gyuchak", "https://x/a.m4a")).isEqualTo("gyuchak.m4a")
        assertThat(AudioFileStore.fileName(9, null, "https://x/a.mp3")).isEqualTo("p9.mp3")
    }

    @Test fun fileForLivesInFilesDirAudio() {
        val f = store.fileFor(testPrayer(2, hasAudio = true).copy(slug = "s", audioUrl = "https://x/a.m4a?x=1"))
        assertThat(f.parentFile).isEqualTo(File(context.filesDir, "audio"))
        assertThat(f.name).isEqualTo("s.m4a")
    }

    @Test fun sizeDeleteAndTotals() {
        store.directory.mkdirs()
        val a = File(store.directory, "a.mp3").apply { writeBytes(ByteArray(10)) }
        val b = File(store.directory, "b.mp3").apply { writeBytes(ByteArray(5)) }
        assertThat(store.sizeOf(a)).isEqualTo(10)
        assertThat(store.totalBytes(listOf(a.path, b.path, "/missing"))).isEqualTo(15)
        assertThat(store.delete(a)).isTrue()
        assertThat(a.exists()).isFalse()
        assertThat(store.delete(a)).isTrue()
    }
}
