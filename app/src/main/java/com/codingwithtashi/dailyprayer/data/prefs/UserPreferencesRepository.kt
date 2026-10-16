package com.codingwithtashi.dailyprayer.data.prefs

import com.codingwithtashi.dailyprayer.core.locale.AppLanguage
import com.codingwithtashi.dailyprayer.data.model.AppTextSize
import com.codingwithtashi.dailyprayer.data.model.Appearance
import kotlinx.coroutines.flow.Flow

interface UserPreferencesRepository {
    val prefs: Flow<UserPrefs>
    suspend fun snapshot(): UserPrefs

    suspend fun setOnboarded(value: Boolean)
    suspend fun setUserName(name: String)
    suspend fun setAppLanguage(language: AppLanguage)
    suspend fun setAppearance(appearance: Appearance)
    suspend fun setAppTextSize(size: AppTextSize)
    /** Stores the reader text size. The repository owns the invariant: values are clamped to [ReaderTextSize] on write and on read. */
    suspend fun setTextSize(size: Int)
    suspend fun setRecitationGoal(goal: Int)
    suspend fun setRemindersEnabled(enabled: Boolean)
    suspend fun setKeepScreenOn(enabled: Boolean)
    suspend fun setLastReadPrayerId(id: Int?)
    suspend fun setLegacyCleanupDone(done: Boolean)
    suspend fun setNotificationPermissionAsked(asked: Boolean)
    suspend fun setWelcomeNotificationInserted(inserted: Boolean)
}
