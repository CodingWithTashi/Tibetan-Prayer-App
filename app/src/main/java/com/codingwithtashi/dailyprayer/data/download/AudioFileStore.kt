package com.codingwithtashi.dailyprayer.data.download

import android.content.Context
import android.media.MediaMetadataRetriever
import com.codingwithtashi.dailyprayer.data.model.Prayer
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/** Owns the on-disk layout of downloaded audio: `filesDir/audio/<slug ?: "p$id">.<ext>`. */
@Singleton
class AudioFileStore @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    val directory: File get() = File(context.filesDir, DIR_NAME)

    fun fileFor(prayer: Prayer): File = File(directory, fileName(prayer.id, prayer.slug, prayer.audioUrl))

    fun sizeOf(file: File): Long = if (file.isFile) file.length() else 0L

    fun delete(file: File): Boolean = !file.exists() || file.delete()

    fun totalBytes(paths: List<String>): Long = paths.sumOf { sizeOf(File(it)) }

    fun probeDurationMs(file: File): Long? {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(file.absolutePath)
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull()?.takeIf { it > 0 }
        } catch (_: Exception) {
            null
        } finally {
            runCatching { retriever.release() }
        }
    }

    companion object {
        const val DIR_NAME = "audio"

        fun fileName(id: Int, slug: String?, url: String): String =
            "${slug?.takeIf { it.isNotBlank() } ?: "p$id"}.${extensionOf(url)}"

        /** Extension of the URL path (before '?'): `m4a` or `mp3`; anything else defaults to `mp3`. */
        fun extensionOf(url: String): String {
            val path = url.substringBefore('#').substringBefore('?')
            val last = path.substringAfterLast('/')
            val ext = if (last.contains('.')) last.substringAfterLast('.').lowercase() else ""
            return if (ext == "m4a") "m4a" else "mp3"
        }
    }
}
