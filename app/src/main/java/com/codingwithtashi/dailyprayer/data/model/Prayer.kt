package com.codingwithtashi.dailyprayer.data.model

data class Prayer(
    val id: Int,
    val slug: String?,
    val title: String,
    val titleEn: String,
    val content: String,
    val category: PrayerCategory,
    val sortOrder: Int,
    val audioUrl: String,
    val hasAudio: Boolean,
    val isDownloaded: Boolean,
    val audioPath: String?,
    val audioDurationMs: Long?,
    val isSaved: Boolean,
    val savedLabel: String?,
    val savedAt: Long?,
    val count: Int,
    val lastReadProgress: Float,
    val lastReadAt: Long?,
    val syllable: String,
)
