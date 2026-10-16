package com.codingwithtashi.dailyprayer.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import com.codingwithtashi.dailyprayer.data.local.entity.PrayerEntity
import com.codingwithtashi.dailyprayer.data.model.PrayerCategory
import kotlinx.coroutines.flow.Flow

@Dao
abstract class PrayerDao {
    @Query("SELECT * FROM prayers ORDER BY sort_order, id")
    abstract fun observeAll(): Flow<List<PrayerEntity>>

    @Query("SELECT * FROM prayers WHERE is_saved = 1 ORDER BY saved_at, id")
    abstract fun observeSaved(): Flow<List<PrayerEntity>>

    @Query("SELECT * FROM prayers WHERE audio_url != '' ORDER BY sort_order, id")
    abstract fun observeWithAudio(): Flow<List<PrayerEntity>>

    @Query("SELECT * FROM prayers WHERE id = :id")
    abstract fun observe(id: Int): Flow<PrayerEntity?>

    @Query("SELECT * FROM prayers WHERE id = :id")
    abstract suspend fun get(id: Int): PrayerEntity?

    @Query("SELECT * FROM prayers WHERE id IN (:ids)")
    abstract suspend fun getByIds(ids: List<Int>): List<PrayerEntity>

    @Query(
        "SELECT * FROM prayers WHERE last_read_progress BETWEEN 0.03 AND 0.97 " +
            "ORDER BY last_read_at DESC LIMIT 1",
    )
    abstract fun observeContinueReading(): Flow<PrayerEntity?>

    @Query("SELECT * FROM prayers ORDER BY sort_order, id")
    abstract suspend fun getAllOnce(): List<PrayerEntity>

    @Query("SELECT * FROM prayers WHERE is_downloaded = 1")
    abstract suspend fun getDownloaded(): List<PrayerEntity>

    @Query("SELECT * FROM prayers WHERE TRIM(title) = :title ORDER BY id LIMIT 1")
    abstract suspend fun findByTitle(title: String): PrayerEntity?

    @Insert
    abstract suspend fun insert(entity: PrayerEntity): Long

    @Query(
        "UPDATE prayers SET slug = :slug, title = :title, title_en = :titleEn, content = :content, " +
            "category = :category, sort_order = :sortOrder, audio_url = :audioUrl WHERE id = :id",
    )
    abstract suspend fun updateStatic(
        id: Int,
        slug: String?,
        title: String,
        titleEn: String,
        content: String,
        category: String,
        sortOrder: Int,
        audioUrl: String,
    )

    @Query(
        "UPDATE prayers SET is_saved = :saved, " +
            "saved_at = CASE WHEN :saved = 1 THEN COALESCE(saved_at, :now) ELSE NULL END, " +
            "saved_label = CASE WHEN :saved = 1 THEN saved_label ELSE NULL END WHERE id = :id",
    )
    abstract suspend fun setSaved(id: Int, saved: Boolean, now: Long)

    @Query("UPDATE prayers SET saved_label = :label WHERE id = :id")
    abstract suspend fun setSavedLabel(id: Int, label: String?)

    @Query("UPDATE prayers SET count = :count WHERE id = :id")
    abstract suspend fun setCount(id: Int, count: Int)

    @Query("UPDATE prayers SET count = count + 1 WHERE id = :id")
    protected abstract suspend fun rawIncrement(id: Int)

    @Query("UPDATE prayers SET count = MAX(count - 1, 0) WHERE id = :id")
    protected abstract suspend fun rawDecrement(id: Int)

    @Query("SELECT count FROM prayers WHERE id = :id")
    abstract suspend fun getCount(id: Int): Int?

    @Transaction
    open suspend fun increment(id: Int): Int {
        rawIncrement(id)
        return getCount(id) ?: 0
    }

    @Transaction
    open suspend fun decrement(id: Int): Int {
        rawDecrement(id)
        return getCount(id) ?: 0
    }

    @Query("UPDATE prayers SET last_read_progress = :progress, last_read_at = :now WHERE id = :id")
    abstract suspend fun setReadProgress(id: Int, progress: Float, now: Long)

    @Query("UPDATE prayers SET audio_duration_ms = :ms WHERE id = :id")
    abstract suspend fun updateAudioDuration(id: Int, ms: Long)

    @Query("UPDATE prayers SET is_downloaded = 1, audio_path = :path WHERE id = :id")
    abstract suspend fun markDownloaded(id: Int, path: String)

    @Query(
        "UPDATE prayers SET is_downloaded = 1, audio_path = :path, " +
            "audio_duration_ms = COALESCE(:durationMs, audio_duration_ms) WHERE id = :id",
    )
    abstract suspend fun markDownloadedWithDuration(id: Int, path: String, durationMs: Long?)

    @Query("UPDATE prayers SET is_downloaded = 0, audio_path = NULL WHERE id = :id")
    abstract suspend fun clearDownload(id: Int)

    @Query(
        "UPDATE prayers SET content = :content, audio_url = COALESCE(:audioUrl, audio_url), " +
            "title_en = COALESCE(:titleEn, title_en), category = COALESCE(:category, category) WHERE id = :id",
    )
    protected abstract suspend fun updateRemote(
        id: Int,
        content: String,
        audioUrl: String?,
        titleEn: String?,
        category: String?,
    )

    /** FCM prayer: match by trimmed title (update) or insert as an unslugged "other" row. */
    @Transaction
    open suspend fun upsertRemote(
        title: String,
        content: String,
        audioUrl: String?,
        titleEn: String?,
        category: String?,
        now: Long,
    ): Int {
        val trimmed = title.trim()
        val existing = findByTitle(trimmed)
        val cat = category?.let { PrayerCategory.fromKey(it).key }
        val en = titleEn?.takeIf { it.isNotBlank() }
        val url = audioUrl?.takeIf { it.isNotBlank() }
        return if (existing != null) {
            val unslugged = existing.slug == null
            updateRemote(existing.id, content, url, if (unslugged) en else null, if (unslugged) cat else null)
            existing.id
        } else {
            insert(
                PrayerEntity(
                    slug = null,
                    title = trimmed,
                    titleEn = en ?: "",
                    content = content,
                    category = cat ?: PrayerCategory.OTHER.key,
                    audioUrl = url ?: "",
                    createdAt = now,
                ),
            ).toInt()
        }
    }
}
