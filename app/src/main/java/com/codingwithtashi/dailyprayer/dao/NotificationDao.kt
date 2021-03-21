package com.codingwithtashi.dailyprayer.dao

import androidx.room.*
import com.codingwithtashi.dailyprayer.model.Prayer
import com.codingwithtashi.dailyprayer.model.PrayerNotification
import kotlinx.coroutines.flow.Flow

@Dao
interface NotificationDao {
    @Query("SELECT * FROM PRAYER_NOTIFICATION_TABLE")
    fun getNotifications(): Flow<List<PrayerNotification>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(notification: PrayerNotification)

    @Delete
    suspend fun delete(notification: PrayerNotification)
}