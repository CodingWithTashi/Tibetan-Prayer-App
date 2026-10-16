package com.codingwithtashi.dailyprayer.reminders

import android.app.AlarmManager
import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ReminderModule {
    @Provides
    fun provideAlarmManager(@ApplicationContext context: Context): AlarmManager =
        context.getSystemService(AlarmManager::class.java)

    @Provides
    fun provideReminderHealthChecker(@ApplicationContext context: Context): ReminderHealthChecker =
        AndroidReminderHealthChecker(context)

    @Provides
    @Singleton
    fun provideReminderHealthMonitor(checker: ReminderHealthChecker): ReminderHealthMonitor =
        DefaultReminderHealthMonitor(checker)
}
