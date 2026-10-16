package com.codingwithtashi.dailyprayer.data.repository

import com.codingwithtashi.dailyprayer.data.local.dao.NotificationDao
import com.codingwithtashi.dailyprayer.data.model.AppNotification
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Singleton
class NotificationRepositoryImpl @Inject constructor(
    private val dao: NotificationDao,
) : NotificationRepository {
    override fun observeAll(): Flow<List<AppNotification>> = dao.observeAll().map { l -> l.map { it.toDomain() } }
    override fun unreadCount(): Flow<Int> = dao.observeUnreadCount()
    override suspend fun insert(notification: AppNotification): Long = dao.insert(notification.toEntity())
    override suspend fun delete(id: Int) = dao.delete(id)
    override suspend fun restore(notification: AppNotification) {
        dao.insert(notification.toEntity())
    }
    override suspend fun markAllRead() = dao.markAllRead()
}
