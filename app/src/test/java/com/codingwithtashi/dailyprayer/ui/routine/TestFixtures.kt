package com.codingwithtashi.dailyprayer.ui.routine

import com.codingwithtashi.dailyprayer.data.model.Prayer
import com.codingwithtashi.dailyprayer.data.model.PrayerCategory
import com.codingwithtashi.dailyprayer.data.model.Routine
import java.time.DayOfWeek

fun testPrayer(id: Int, hasAudio: Boolean = false) = Prayer(
    id = id, slug = "p$id", title = "tib$id", titleEn = "En $id", content = "", category = PrayerCategory.DAILY,
    sortOrder = id, audioUrl = if (hasAudio) "https://x/$id.mp3" else "", hasAudio = hasAudio, isDownloaded = false,
    audioPath = null, audioDurationMs = null, isSaved = false, savedLabel = null, savedAt = null, count = 0,
    lastReadProgress = 0f, lastReadAt = null, syllable = "s$id",
)

fun testRoutine(
    id: Long = 1,
    name: String = "Morning",
    time: Int = 600,
    days: Set<DayOfWeek> = DayOfWeek.entries.toSet(),
    reminder: Boolean = true,
    prayers: List<Prayer> = listOf(testPrayer(1), testPrayer(2), testPrayer(3)),
) = Routine(id, name, time, days, reminder, prayers, createdAt = 0L)
