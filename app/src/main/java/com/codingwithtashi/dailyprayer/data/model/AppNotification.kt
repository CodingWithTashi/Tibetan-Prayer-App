package com.codingwithtashi.dailyprayer.data.model

data class AppNotification(
    val id: Int = 0,
    val title: String,
    val content: String,
    val receivedAt: Long,
    val link: String? = null,
    val isRead: Boolean = false,
)
