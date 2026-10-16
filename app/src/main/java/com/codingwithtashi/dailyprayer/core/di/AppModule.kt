package com.codingwithtashi.dailyprayer.core.di

import com.codingwithtashi.dailyprayer.core.time.AppClock
import com.codingwithtashi.dailyprayer.core.time.SystemAppClock
import com.codingwithtashi.dailyprayer.data.prefs.UserPreferencesRepository
import com.codingwithtashi.dailyprayer.data.prefs.UserPreferencesRepositoryImpl
import com.codingwithtashi.dailyprayer.data.repository.DownloadRepository
import com.codingwithtashi.dailyprayer.data.repository.DownloadRepositoryImpl
import com.codingwithtashi.dailyprayer.data.repository.NotificationRepository
import com.codingwithtashi.dailyprayer.data.repository.NotificationRepositoryImpl
import com.codingwithtashi.dailyprayer.data.repository.PrayerRepository
import com.codingwithtashi.dailyprayer.data.repository.PrayerRepositoryImpl
import com.codingwithtashi.dailyprayer.data.repository.RoutineRepository
import com.codingwithtashi.dailyprayer.data.repository.RoutineRepositoryImpl
import com.codingwithtashi.dailyprayer.reminders.ReminderSchedulerImpl
import com.codingwithtashi.dailyprayer.reminders.ReminderScheduler
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AppModule {
    @Binds @Singleton abstract fun bindPrayerRepository(impl: PrayerRepositoryImpl): PrayerRepository
    @Binds @Singleton abstract fun bindRoutineRepository(impl: RoutineRepositoryImpl): RoutineRepository
    @Binds @Singleton abstract fun bindNotificationRepository(impl: NotificationRepositoryImpl): NotificationRepository
    @Binds @Singleton abstract fun bindUserPreferences(impl: UserPreferencesRepositoryImpl): UserPreferencesRepository
    @Binds @Singleton abstract fun bindDownloadRepository(impl: DownloadRepositoryImpl): DownloadRepository
    @Binds abstract fun bindPlayerController(impl: com.codingwithtashi.dailyprayer.playback.PlayerController): com.codingwithtashi.dailyprayer.playback.PlayerControllerApi
    @Binds abstract fun bindAudioDownloader(impl: com.codingwithtashi.dailyprayer.data.download.OkHttpAudioDownloader): com.codingwithtashi.dailyprayer.data.download.AudioDownloader
    @Binds @Singleton abstract fun bindClock(impl: SystemAppClock): AppClock

    @Binds @Singleton abstract fun bindReminderScheduler(impl: ReminderSchedulerImpl): ReminderScheduler
}
