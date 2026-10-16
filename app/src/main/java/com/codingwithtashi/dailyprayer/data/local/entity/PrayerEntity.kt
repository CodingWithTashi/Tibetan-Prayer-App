package com.codingwithtashi.dailyprayer.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "prayers",
    indices = [Index("slug", unique = true), Index("category"), Index("is_saved"), Index("sort_order")],
)
data class PrayerEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val slug: String?,
    val title: String,
    @ColumnInfo(name = "title_en", defaultValue = "") val titleEn: String = "",
    @ColumnInfo(defaultValue = "") val content: String = "",
    @ColumnInfo(defaultValue = "other") val category: String = "other",
    @ColumnInfo(name = "sort_order", defaultValue = "1000") val sortOrder: Int = 1000,
    @ColumnInfo(name = "audio_url", defaultValue = "") val audioUrl: String = "",
    @ColumnInfo(name = "audio_path") val audioPath: String? = null,
    @ColumnInfo(name = "audio_duration_ms") val audioDurationMs: Long? = null,
    @ColumnInfo(name = "is_downloaded", defaultValue = "0") val isDownloaded: Boolean = false,
    @ColumnInfo(name = "is_saved", defaultValue = "0") val isSaved: Boolean = false,
    @ColumnInfo(name = "saved_label") val savedLabel: String? = null,
    @ColumnInfo(name = "saved_at") val savedAt: Long? = null,
    @ColumnInfo(defaultValue = "0") val count: Int = 0,
    @ColumnInfo(name = "last_read_progress", defaultValue = "0") val lastReadProgress: Float = 0f,
    @ColumnInfo(name = "last_read_at") val lastReadAt: Long? = null,
    @ColumnInfo(name = "created_at", defaultValue = "0") val createdAt: Long = 0,
)
