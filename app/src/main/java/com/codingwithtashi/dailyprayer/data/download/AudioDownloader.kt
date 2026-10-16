package com.codingwithtashi.dailyprayer.data.download

import com.codingwithtashi.dailyprayer.data.model.DownloadError
import androidx.core.net.toUri
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.HttpDataSource
import com.codingwithtashi.dailyprayer.playback.Media3AudioCache
import java.io.File
import java.io.IOException
import java.net.UnknownHostException
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.job
import kotlinx.coroutines.Dispatchers

data class DownloadProgress(val bytes: Long, val total: Long)

class DownloadException(val error: DownloadError, cause: Throwable? = null) : Exception(error.name, cause)

interface AudioDownloader {
    /** Downloads [url] to [dest]; completes normally only when [dest] is fully written. */
    fun download(url: String, dest: File): Flow<DownloadProgress>
}

/**
 * Copies [url] to a file by reading through the shared [Media3AudioCache]: bytes already cached are read
 * locally and only missing ones hit the network (and are cached for later streaming).
 */
@Singleton
@androidx.annotation.OptIn(markerClass = [UnstableApi::class])
class CachingAudioDownloader @Inject constructor(
    private val cache: Media3AudioCache,
) : AudioDownloader {

    override fun download(url: String, dest: File): Flow<DownloadProgress> = flow {
        val part = File(dest.parentFile, dest.name + ".part")
        part.parentFile?.mkdirs()
        val source = cache.readThroughDataSource()
        var success = false
        try {
            val opened = source.open(DataSpec(url.toUri()))
            val total = if (opened == C.LENGTH_UNSET.toLong()) -1L else opened
            var written = 0L
            var lastEmit = 0L
            emit(DownloadProgress(0, total))
            part.outputStream().use { out ->
                val buf = ByteArray(BUFFER)
                while (true) {
                    currentCoroutineContext().ensureActive()
                    val n = source.read(buf, 0, buf.size)
                    if (n == C.RESULT_END_OF_INPUT) break
                    out.write(buf, 0, n)
                    written += n
                    val now = System.nanoTime() / 1_000_000
                    if (now - lastEmit >= EMIT_INTERVAL_MS) {
                        lastEmit = now
                        emit(DownloadProgress(written, total))
                    }
                }
            }
            if (total >= 0 && written != total) throw DownloadException(DownloadError.NETWORK)
            if (dest.exists()) dest.delete()
            if (!part.renameTo(dest)) throw DownloadException(DownloadError.DISK)
            success = true
            emit(DownloadProgress(written, if (total >= 0) total else written))
        } catch (e: CancellationException) {
            throw e
        } catch (e: DownloadException) {
            throw e
        } catch (e: IOException) {
            if (currentCoroutineContext().job.isCancelled) throw CancellationException("cancelled", e)
            throw DownloadException(map(e), e)
        } finally {
            runCatching { source.close() }
            if (!success) part.delete()
        }
    }.flowOn(Dispatchers.IO)

    private fun map(e: IOException): DownloadError {
        var t: Throwable? = e
        while (t != null) {
            when {
                t is HttpDataSource.InvalidResponseCodeException ->
                    return if (t.responseCode >= 500) DownloadError.NETWORK else DownloadError.UNKNOWN
                t is HttpDataSource.HttpDataSourceException || t is UnknownHostException -> return DownloadError.NETWORK
                t is IOException && isDiskFull(t) -> return DownloadError.DISK
            }
            t = t.cause
        }
        return DownloadError.NETWORK
    }

    private fun isDiskFull(e: IOException): Boolean {
        val m = e.message.orEmpty()
        return m.contains("ENOSPC") || m.contains("No space left", ignoreCase = true)
    }

    private companion object {
        const val BUFFER = 64 * 1024
        const val EMIT_INTERVAL_MS = 100L
    }
}
