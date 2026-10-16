package com.codingwithtashi.dailyprayer.core.di

import android.content.Context
import androidx.room.Room
import com.codingwithtashi.dailyprayer.data.local.PrayerDatabase
import com.codingwithtashi.dailyprayer.data.local.dao.CompletionDao
import com.codingwithtashi.dailyprayer.data.local.dao.NotificationDao
import com.codingwithtashi.dailyprayer.data.local.dao.PrayerDao
import com.codingwithtashi.dailyprayer.data.local.dao.RoutineDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): PrayerDatabase =
        Room.databaseBuilder(context, PrayerDatabase::class.java, PrayerDatabase.DB_NAME).build()

    @Provides fun providePrayerDao(db: PrayerDatabase): PrayerDao = db.prayerDao()
    @Provides fun provideRoutineDao(db: PrayerDatabase): RoutineDao = db.routineDao()
    @Provides fun provideCompletionDao(db: PrayerDatabase): CompletionDao = db.completionDao()
    @Provides fun provideNotificationDao(db: PrayerDatabase): NotificationDao = db.notificationDao()
}
