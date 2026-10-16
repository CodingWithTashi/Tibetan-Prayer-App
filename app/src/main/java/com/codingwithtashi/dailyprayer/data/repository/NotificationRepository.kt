package com.codingwithtashi.dailyprayer.data.repository

import com.codingwithtashi.dailyprayer.data.model.AppNotification
import kotlinx.coroutines.flow.Flow

interface NotificationRepository {
    fun observeAll(): Flow<List<AppNotification>>
    fun unreadCount(): Flow<Int>
    suspend fun insert(notification: AppNotification): Long
    suspend fun delete(id: Int)
    suspend fun restore(notification: AppNotification)
    suspend fun markAllRead()
}
