package com.codingwithtashi.dailyprayer.data.seed

import android.content.Context
import androidx.room.withTransaction
import com.codingwithtashi.dailyprayer.R
import com.codingwithtashi.dailyprayer.core.time.AppClock
import com.codingwithtashi.dailyprayer.data.local.PrayerDatabase
import com.codingwithtashi.dailyprayer.data.local.dao.NotificationDao
import com.codingwithtashi.dailyprayer.data.local.dao.PrayerDao
import com.codingwithtashi.dailyprayer.data.local.entity.NotificationEntity
import com.codingwithtashi.dailyprayer.data.local.entity.PrayerEntity
import com.codingwithtashi.dailyprayer.data.prefs.UserPreferencesRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PrayerSeeder @Inject constructor(
    @ApplicationContext private val context: Context,
    private val db: PrayerDatabase,
    private val prayerDao: PrayerDao,
    private val notificationDao: NotificationDao,
    private val prefs: UserPreferencesRepository,
    private val clock: AppClock,
) {
    /** Idempotent: safe to run on every start. Only static fields of catalogue rows are touched. */
    suspend fun seed() {
        val now = clock.now().toEpochMilli()
        db.withTransaction {
            val existing = prayerDao.getAllOnce()
            val bySlug = existing.filter { it.slug != null }.associateBy { it.slug }
            val unslugged = existing.filter { it.slug == null }.sortedBy { it.id }.toMutableList()
            for (s in PrayerCatalog.all) {
                val row = bySlug[s.slug]
                    ?: unslugged.firstOrNull { it.title.trim() == s.title }?.also { unslugged.remove(it) }
                if (row == null) {
                    prayerDao.insert(s.toEntity(now))
                } else if (row.differsFrom(s)) {
                    prayerDao.updateStatic(
                        row.id, s.slug, s.title, s.titleEn, s.content, s.category.key, s.sortOrder, s.audioUrl,
                    )
                }
            }
        }
        insertWelcomeNotificationOnce(now)
    }

    /** Clears download flags for rows whose audio file is gone. */
    suspend fun validateAudioFiles() {
        for (row in prayerDao.getDownloaded()) {
            val path = row.audioPath
            if (path == null || !File(path).exists()) prayerDao.clearDownload(row.id)
        }
    }

    private suspend fun insertWelcomeNotificationOnce(now: Long) {
        if (notificationDao.count() == 0 && !prefs.snapshot().welcomeNotificationInserted) {
            notificationDao.insert(
                NotificationEntity(
                    title = context.getString(R.string.welcome_notification_title),
                    content = context.getString(R.string.welcome_notification_body),
                    receivedAt = now,
                ),
            )
            prefs.setWelcomeNotificationInserted(true)
        }
    }

    private fun StaticPrayer.toEntity(now: Long) = PrayerEntity(
        slug = slug,
        title = title,
        titleEn = titleEn,
        content = content,
        category = category.key,
        sortOrder = sortOrder,
        audioUrl = audioUrl,
        createdAt = now,
    )

    private fun PrayerEntity.differsFrom(s: StaticPrayer) =
        slug != s.slug || title != s.title || titleEn != s.titleEn || content != s.content ||
            category != s.category.key || sortOrder != s.sortOrder || audioUrl != s.audioUrl
}
