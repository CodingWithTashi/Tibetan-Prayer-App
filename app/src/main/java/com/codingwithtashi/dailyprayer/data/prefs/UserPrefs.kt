package com.codingwithtashi.dailyprayer.data.prefs

import com.codingwithtashi.dailyprayer.core.locale.AppLanguage
import com.codingwithtashi.dailyprayer.data.model.AppTextSize
import com.codingwithtashi.dailyprayer.data.model.Appearance

data class UserPrefs(
    val onboarded: Boolean = false,
    val userName: String = "",
    val appLanguage: AppLanguage = AppLanguage.EN,
    val appearance: Appearance = Appearance.SYSTEM,
    val appTextSize: AppTextSize = AppTextSize.DEFAULT,
    val textSize: Int = ReaderTextSize.DEFAULT,
    val recitationGoal: Int = 108,
    val remindersEnabled: Boolean = true,
    val keepScreenOn: Boolean = true,
    val lastReadPrayerId: Int? = null,
    val legacyCleanupDone: Boolean = false,
    val notificationPermissionAsked: Boolean = false,
    val welcomeNotificationInserted: Boolean = false,
)
