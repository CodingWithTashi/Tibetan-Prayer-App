package com.codingwithtashi.dailyprayer.data.repository

import com.codingwithtashi.dailyprayer.core.text.PrayerText
import com.codingwithtashi.dailyprayer.data.model.Prayer
import kotlinx.coroutines.flow.Flow

interface PrayerRepository {
    fun observeAll(): Flow<List<Prayer>>
    fun observeSaved(): Flow<List<Prayer>>
    fun observeWithAudio(): Flow<List<Prayer>>
    fun observe(id: Int): Flow<Prayer?>
    suspend fun get(id: Int): Prayer?

    /** Returned in the order of [ids]; unknown ids are skipped. */
    suspend fun getByIds(ids: List<Int>): List<Prayer>
    fun observeContinueReading(): Flow<Prayer?>
    suspend fun setSaved(id: Int, saved: Boolean)
    suspend fun setSavedLabel(id: Int, label: String?)
    suspend fun setCount(id: Int, count: Int)
    suspend fun incrementCount(id: Int): Int
    suspend fun decrementCount(id: Int): Int
    suspend fun setReadProgress(id: Int, progress: Float)
    suspend fun text(id: Int): PrayerText
    suspend fun updateAudioDuration(id: Int, ms: Long)

    /** Marks the prayer's audio as downloaded at [path]; [durationMs] (if known) is stored too. */
    suspend fun setDownloaded(id: Int, path: String, durationMs: Long?) = Unit

    /** Clears the downloaded flag and path. */
    suspend fun clearDownloaded(id: Int) = Unit
    suspend fun upsertRemote(
        title: String,
        content: String,
        audioUrl: String? = null,
        titleEn: String? = null,
        category: String? = null,
    ): Int
}
