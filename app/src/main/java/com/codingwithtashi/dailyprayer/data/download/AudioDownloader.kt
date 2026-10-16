package com.codingwithtashi.dailyprayer.data.download

import com.codingwithtashi.dailyprayer.data.model.DownloadError
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
import okhttp3.OkHttpClient
import okhttp3.Request

data class DownloadProgress(val bytes: Long, val total: Long)

class DownloadException(val error: DownloadError, cause: Throwable? = null) : Exception(error.name, cause)

interface AudioDownloader {
    /** Downloads [url] to [dest]; completes normally only when [dest] is fully written. */
    fun download(url: String, dest: File): Flow<DownloadProgress>
}

@Singleton
class OkHttpAudioDownloader @Inject constructor(
    private val client: OkHttpClient,
) : AudioDownloader {

    override fun download(url: String, dest: File): Flow<DownloadProgress> = flow {
        val part = File(dest.parentFile, dest.name + ".part")
        part.parentFile?.mkdirs()
        val call = client.newCall(Request.Builder().url(url).build())
        val handle = currentCoroutineContext().job.invokeOnCompletion { if (it != null) call.cancel() }
        var success = false
        try {
            call.execute().use { response ->
                if (!response.isSuccessful) {
                    throw DownloadException(if (response.code >= 500) DownloadError.NETWORK else DownloadError.UNKNOWN)
                }
                val body = response.body
                val total = body.contentLength().takeIf { it > 0 } ?: -1L
                var written = 0L
                var lastEmit = 0L
                emit(DownloadProgress(0, total))
                body.byteStream().use { input ->
                    part.outputStream().use { out ->
                        val buf = ByteArray(BUFFER)
                        while (true) {
                            currentCoroutineContext().ensureActive()
                            val n = input.read(buf)
                            if (n < 0) break
                            out.write(buf, 0, n)
                            written += n
                            val now = System.nanoTime() / 1_000_000
                            if (now - lastEmit >= EMIT_INTERVAL_MS) {
                                lastEmit = now
                                emit(DownloadProgress(written, total))
                            }
                        }
                    }
                }
                if (total > 0 && written != total) throw DownloadException(DownloadError.NETWORK)
                if (dest.exists()) dest.delete()
                if (!part.renameTo(dest)) throw DownloadException(DownloadError.DISK)
                success = true
                emit(DownloadProgress(written, if (total > 0) total else written))
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: DownloadException) {
            throw e
        } catch (e: UnknownHostException) {
            throw DownloadException(DownloadError.NETWORK, e)
        } catch (e: IOException) {
            if (currentCoroutineContext().job.isCancelled) throw CancellationException("cancelled", e)
            throw DownloadException(if (isDiskFull(e)) DownloadError.DISK else DownloadError.NETWORK, e)
        } finally {
            handle.dispose()
            if (!success) part.delete()
        }
    }.flowOn(Dispatchers.IO)

    private fun isDiskFull(e: IOException): Boolean {
        val m = e.message.orEmpty()
        return m.contains("ENOSPC") || m.contains("No space left", ignoreCase = true)
    }

    private companion object {
        const val BUFFER = 64 * 1024
        const val EMIT_INTERVAL_MS = 100L
    }
}
