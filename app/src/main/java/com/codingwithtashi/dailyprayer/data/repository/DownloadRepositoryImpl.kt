package com.codingwithtashi.dailyprayer.data.repository

import com.codingwithtashi.dailyprayer.core.di.ApplicationScope
import com.codingwithtashi.dailyprayer.core.di.IoDispatcher
import com.codingwithtashi.dailyprayer.core.network.ConnectivityChecker
import com.codingwithtashi.dailyprayer.data.download.AudioDownloader
import com.codingwithtashi.dailyprayer.data.download.AudioFileStore
import com.codingwithtashi.dailyprayer.data.download.DownloadException
import com.codingwithtashi.dailyprayer.data.model.DownloadError
import com.codingwithtashi.dailyprayer.data.model.DownloadState
import com.codingwithtashi.dailyprayer.playback.AudioCache
import java.io.File
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Singleton
class DownloadRepositoryImpl @Inject constructor(
    private val prayers: PrayerRepository,
    private val downloader: AudioDownloader,
    private val files: AudioFileStore,
    private val connectivity: ConnectivityChecker,
    private val audioCache: AudioCache,
    @ApplicationScope private val scope: CoroutineScope,
    @IoDispatcher private val io: CoroutineDispatcher,
) : DownloadRepository {

    private val transient = MutableStateFlow<Map<Int, DownloadState>>(emptyMap())
    private val jobs = HashMap<Int, Job>()

    override fun state(prayerId: Int): Flow<DownloadState> = combine(
        prayers.observe(prayerId).map { it?.isDownloaded == true }.distinctUntilChanged(),
        transient.map { it[prayerId] }.distinctUntilChanged(),
    ) { downloaded, temp ->
        temp ?: if (downloaded) DownloadState.Downloaded else DownloadState.NotDownloaded
    }

    override fun download(prayerId: Int) {
        synchronized(jobs) {
            if (jobs[prayerId]?.isActive == true) return
            jobs[prayerId] = scope.launch { runDownload(prayerId) }
        }
    }

    private suspend fun runDownload(id: Int) {
        val prayer = prayers.get(id)
        if (prayer == null || prayer.audioUrl.isBlank()) {
            setTransient(id, DownloadState.Failed(DownloadError.UNKNOWN))
            return
        }
        if (prayer.isDownloaded && prayer.audioPath?.let { File(it).exists() } == true) return
        if (!connectivity.isOnline() && !withContext(io) { audioCache.isFullyCached(prayer.audioUrl) }) {
            setTransient(id, DownloadState.Failed(DownloadError.OFFLINE))
            return
        }
        val dest = files.fileFor(prayer)
        setTransient(id, DownloadState.Downloading(0f, 0L, 0L))
        try {
            downloader.download(prayer.audioUrl, dest).collect { p ->
                val progress = if (p.total > 0) (p.bytes.toFloat() / p.total).coerceIn(0f, 1f) else 0f
                setTransient(id, DownloadState.Downloading(progress, p.bytes, p.total))
            }
            val duration = withContext(io) { files.probeDurationMs(dest) }
            prayers.setDownloaded(id, dest.absolutePath, duration)
            clearTransient(id)
        } catch (e: CancellationException) {
            clearTransient(id)
            throw e
        } catch (e: DownloadException) {
            setTransient(id, DownloadState.Failed(e.error))
        } catch (e: Exception) {
            setTransient(id, DownloadState.Failed(DownloadError.UNKNOWN))
        }
    }

    override fun cancel(prayerId: Int) {
        val job = synchronized(jobs) { jobs.remove(prayerId) }
        job?.cancel()
        clearTransient(prayerId)
    }

    override suspend fun delete(prayerId: Int) {
        cancel(prayerId)
        val prayer = prayers.get(prayerId)
        withContext(io) {
            prayer?.audioPath?.let { files.delete(File(it)) }
            prayer?.let { files.delete(files.fileFor(it)) }
        }
        prayers.clearDownloaded(prayerId)
        clearTransient(prayerId)
    }

    override fun downloadedCount(): Flow<Int> =
        prayers.observeAll().map { l -> l.count { it.isDownloaded } }.distinctUntilChanged()

    override fun totalBytes(): Flow<Long> = prayers.observeAll()
        .map { l -> files.totalBytes(l.filter { it.isDownloaded }.mapNotNull { it.audioPath }) }
        .distinctUntilChanged()
        .flowOn(io)

    override fun storageLabel(bytes: Long): String =
        String.format(Locale.US, "%.1f MB", bytes / (1024.0 * 1024.0))

    private fun setTransient(id: Int, s: DownloadState) = transient.update { it + (id to s) }
    private fun clearTransient(id: Int) = transient.update { it - id }
}
