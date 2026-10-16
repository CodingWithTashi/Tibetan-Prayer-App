package com.codingwithtashi.dailyprayer.data.repository

import com.codingwithtashi.dailyprayer.core.text.VerseSplitter
import com.codingwithtashi.dailyprayer.data.local.entity.NotificationEntity
import com.codingwithtashi.dailyprayer.data.local.entity.PrayerEntity
import com.codingwithtashi.dailyprayer.data.local.entity.RoutineCompletionEntity
import com.codingwithtashi.dailyprayer.data.local.relation.RoutineWithPrayers
import com.codingwithtashi.dailyprayer.data.model.AppNotification
import com.codingwithtashi.dailyprayer.reminders.DaysMask
import com.codingwithtashi.dailyprayer.data.model.Prayer
import com.codingwithtashi.dailyprayer.data.model.PrayerCategory
import com.codingwithtashi.dailyprayer.data.model.Routine
import com.codingwithtashi.dailyprayer.data.model.RoutineCompletion
import java.time.LocalDate

internal fun PrayerEntity.toDomain() = Prayer(
    id = id,
    slug = slug,
    title = title,
    titleEn = titleEn,
    content = content,
    category = PrayerCategory.fromKey(category),
    sortOrder = sortOrder,
    audioUrl = audioUrl,
    hasAudio = audioUrl.isNotBlank(),
    isDownloaded = isDownloaded,
    audioPath = audioPath,
    audioDurationMs = audioDurationMs,
    isSaved = isSaved,
    savedLabel = savedLabel,
    savedAt = savedAt,
    count = count,
    lastReadProgress = lastReadProgress,
    lastReadAt = lastReadAt,
    syllable = VerseSplitter.syllable(title),
)

internal fun RoutineWithPrayers.toDomain() = Routine(
    id = routine.id,
    name = routine.name,
    timeMinutes = routine.timeMinutes,
    days = DaysMask.toSet(routine.daysMask),
    reminderEnabled = routine.reminderEnabled,
    prayers = orderedPrayers().map { it.toDomain() },
    createdAt = routine.createdAt,
)

internal fun NotificationEntity.toDomain() = AppNotification(id, title, content, receivedAt, link, isRead)

internal fun AppNotification.toEntity() = NotificationEntity(id, title, content, receivedAt, link, isRead)

internal fun RoutineCompletionEntity.toDomain() =
    RoutineCompletion(routineId, prayerId, LocalDate.parse(date), completedAt)
