package com.codingwithtashi.dailyprayer.utils

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AudioCacheManager @Inject constructor(
    private val context: Context
) {
    companion object {
        private const val TAG = "AudioCacheManager"
        private const val CACHE_DIR_NAME = "prayer_audio"
    }

    private val cacheDir: File by lazy {
        File(context.cacheDir, CACHE_DIR_NAME).apply {
            if (!exists()) {
                mkdirs()
            }
        }
    }

    /**
     * Check if audio file is cached
     */
    fun isCached(prayerId: Int, audioUrl: String?): Boolean {
        if (audioUrl.isNullOrEmpty()) return false
        val fileName = generateFileName(prayerId, audioUrl)
        val file = File(cacheDir, fileName)
        return file.exists() && file.length() > 0
    }

    /**
     * Get cached file path
     */
    fun getCachedFilePath(prayerId: Int, audioUrl: String?): String? {
        if (audioUrl.isNullOrEmpty()) return null
        val fileName = generateFileName(prayerId, audioUrl)
        val file = File(cacheDir, fileName)
        return if (file.exists()) file.absolutePath else null
    }

    /**
     * Download and cache audio file
     */
    suspend fun downloadAndCache(prayerId: Int, audioUrl: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val fileName = generateFileName(prayerId, audioUrl)
            val outputFile = File(cacheDir, fileName)

            // If already exists, return path
            if (outputFile.exists() && outputFile.length() > 0) {
                return@withContext Result.success(outputFile.absolutePath)
            }

            Log.d(TAG, "Downloading audio from: $audioUrl")

            val url = URL(audioUrl)
            val connection = url.openConnection()
            connection.connect()

            val contentLength = connection.contentLength
            Log.d(TAG, "File size: $contentLength bytes")

            connection.getInputStream().use { input ->
                FileOutputStream(outputFile).use { output ->
                    val buffer = ByteArray(8192)
                    var bytesRead: Int
                    var totalBytesRead = 0L

                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        output.write(buffer, 0, bytesRead)
                        totalBytesRead += bytesRead

                        // Log progress for large files
                        if (contentLength > 0 && totalBytesRead % (1024 * 1024) == 0L) {
                            val progress = (totalBytesRead * 100 / contentLength)
                            Log.d(TAG, "Download progress: $progress%")
                        }
                    }
                    output.flush()
                }
            }

            Log.d(TAG, "Download completed: ${outputFile.absolutePath}")
            Result.success(outputFile.absolutePath)

        } catch (e: Exception) {
            Log.e(TAG, "Error downloading audio", e)
            Result.failure(e)
        }
    }

    /**
     * Delete cached file
     */
    fun deleteCachedFile(prayerId: Int, audioUrl: String?): Boolean {
        if (audioUrl.isNullOrEmpty()) return false
        val fileName = generateFileName(prayerId, audioUrl)
        val file = File(cacheDir, fileName)
        return if (file.exists()) {
            file.delete()
        } else {
            false
        }
    }

    /**
     * Clear all cached audio files
     */
    fun clearAllCache(): Boolean {
        return try {
            cacheDir.listFiles()?.forEach { it.delete() }
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error clearing cache", e)
            false
        }
    }

    /**
     * Get total cache size in bytes
     */
    fun getCacheSize(): Long {
        return cacheDir.listFiles()?.sumOf { it.length() } ?: 0L
    }

    /**
     * Get cache size in human-readable format
     */
    fun getCacheSizeFormatted(): String {
        val bytes = getCacheSize()
        return when {
            bytes < 1024 -> "$bytes B"
            bytes < 1024 * 1024 -> "${bytes / 1024} KB"
            else -> String.format("%.2f MB", bytes / (1024.0 * 1024.0))
        }
    }

    private fun generateFileName(prayerId: Int, audioUrl: String): String {
        val extension = getAudioExtension(audioUrl)
        return "prayer_${prayerId}.$extension"
    }

    private fun getAudioExtension(url: String): String {
        return when {
            url.contains(".mp3") -> "mp3"
            url.contains(".m4a") -> "m4a"
            url.contains(".wav") -> "wav"
            url.contains(".ogg") -> "ogg"
            else -> "mp3"
        }
    }
}