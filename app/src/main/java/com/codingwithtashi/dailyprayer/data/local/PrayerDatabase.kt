package com.codingwithtashi.dailyprayer.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.codingwithtashi.dailyprayer.data.local.dao.CompletionDao
import com.codingwithtashi.dailyprayer.data.local.dao.NotificationDao
import com.codingwithtashi.dailyprayer.data.local.dao.PrayerDao
import com.codingwithtashi.dailyprayer.data.local.dao.RoutineDao
import com.codingwithtashi.dailyprayer.data.local.entity.NotificationEntity
import com.codingwithtashi.dailyprayer.data.local.entity.PrayerEntity
import com.codingwithtashi.dailyprayer.data.local.entity.RoutineCompletionEntity
import com.codingwithtashi.dailyprayer.data.local.entity.RoutineEntity
import com.codingwithtashi.dailyprayer.data.local.entity.RoutinePrayerCrossRef

@Database(
    entities = [
        PrayerEntity::class,
        RoutineEntity::class,
        RoutinePrayerCrossRef::class,
        RoutineCompletionEntity::class,
        NotificationEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
abstract class PrayerDatabase : RoomDatabase() {
    abstract fun prayerDao(): PrayerDao
    abstract fun routineDao(): RoutineDao
    abstract fun completionDao(): CompletionDao
    abstract fun notificationDao(): NotificationDao

    companion object {
        const val DB_NAME = "daily_prayer.db"
    }
}
