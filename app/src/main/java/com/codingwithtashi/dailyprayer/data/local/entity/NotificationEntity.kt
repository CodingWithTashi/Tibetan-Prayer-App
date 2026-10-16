package com.codingwithtashi.dailyprayer.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "notifications")
data class NotificationEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val content: String,
    @ColumnInfo(name = "received_at") val receivedAt: Long,
    val link: String? = null,
    @ColumnInfo(name = "is_read", defaultValue = "0") val isRead: Boolean = false,
)
