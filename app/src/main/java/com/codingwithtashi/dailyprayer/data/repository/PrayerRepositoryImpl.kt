package com.codingwithtashi.dailyprayer.data.repository

import com.codingwithtashi.dailyprayer.core.text.PrayerText
import com.codingwithtashi.dailyprayer.core.text.PrayerTextCache
import com.codingwithtashi.dailyprayer.core.time.AppClock
import com.codingwithtashi.dailyprayer.data.local.dao.PrayerDao
import com.codingwithtashi.dailyprayer.data.model.Prayer
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Singleton
class PrayerRepositoryImpl @Inject constructor(
    private val dao: PrayerDao,
    private val clock: AppClock,
    private val textCache: PrayerTextCache,
) : PrayerRepository {

    override fun observeAll(): Flow<List<Prayer>> = dao.observeAll().map { l -> l.map { it.toDomain() } }
    override fun observeSaved(): Flow<List<Prayer>> = dao.observeSaved().map { l -> l.map { it.toDomain() } }
    override fun observeWithAudio(): Flow<List<Prayer>> = dao.observeWithAudio().map { l -> l.map { it.toDomain() } }
    override fun observe(id: Int): Flow<Prayer?> = dao.observe(id).map { it?.toDomain() }
    override suspend fun get(id: Int): Prayer? = dao.get(id)?.toDomain()

    override suspend fun getByIds(ids: List<Int>): List<Prayer> {
        if (ids.isEmpty()) return emptyList()
        val byId = dao.getByIds(ids.distinct()).associateBy { it.id }
        return ids.mapNotNull { byId[it]?.toDomain() }
    }

    override fun observeContinueReading(): Flow<Prayer?> = dao.observeContinueReading().map { it?.toDomain() }

    override suspend fun setSaved(id: Int, saved: Boolean) = dao.setSaved(id, saved, clock.now().toEpochMilli())

    override suspend fun setSavedLabel(id: Int, label: String?) =
        dao.setSavedLabel(id, label?.trim()?.take(MAX_LABEL)?.takeIf { it.isNotEmpty() })

    override suspend fun setCount(id: Int, count: Int) = dao.setCount(id, count.coerceAtLeast(0))
    override suspend fun incrementCount(id: Int): Int = dao.increment(id)
    override suspend fun decrementCount(id: Int): Int = dao.decrement(id)

    override suspend fun setReadProgress(id: Int, progress: Float) =
        dao.setReadProgress(id, progress.coerceIn(0f, 1f), clock.now().toEpochMilli())

    override suspend fun text(id: Int): PrayerText {
        val content = dao.get(id)?.content.orEmpty()
        return textCache.get(id, content)
    }

    override suspend fun updateAudioDuration(id: Int, ms: Long) = dao.updateAudioDuration(id, ms)

    override suspend fun setDownloaded(id: Int, path: String, durationMs: Long?) =
        dao.markDownloadedWithDuration(id, path, durationMs)

    override suspend fun clearDownloaded(id: Int) = dao.clearDownload(id)

    override suspend fun upsertRemote(
        title: String,
        content: String,
        audioUrl: String?,
        titleEn: String?,
        category: String?,
    ): Int = dao.upsertRemote(title, content, audioUrl, titleEn, category, clock.now().toEpochMilli())

    private companion object {
        const val MAX_LABEL = 28
    }
}
