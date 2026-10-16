package com.codingwithtashi.dailyprayer.data.prefs

import com.codingwithtashi.dailyprayer.core.locale.AppLanguage
import com.codingwithtashi.dailyprayer.data.model.Appearance
import com.codingwithtashi.dailyprayer.data.tempDataStore
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Test

class UserPreferencesRepositoryImplTest {
    @Test fun defaults() = runTest {
        val p = UserPreferencesRepositoryImpl(tempDataStore()).snapshot()
        assertThat(p).isEqualTo(UserPrefs())
        assertThat(p.appearance).isEqualTo(Appearance.SYSTEM)
        assertThat(p.textSize).isEqualTo(22)
        assertThat(p.recitationGoal).isEqualTo(108)
        assertThat(p.remindersEnabled).isTrue()
        assertThat(p.keepScreenOn).isTrue()
        assertThat(p.lastReadPrayerId).isNull()
    }

    @Test fun roundTrip() = runTest {
        val r = UserPreferencesRepositoryImpl(tempDataStore())
        r.setOnboarded(true)
        r.setUserName("  " + "n".repeat(30))
        r.setAppLanguage(AppLanguage.BO)
        r.setAppearance(Appearance.DARK)
        r.setTextSize(99)
        r.setRecitationGoal(1000)
        r.setRemindersEnabled(false)
        r.setKeepScreenOn(false)
        r.setLastReadPrayerId(5)
        r.setLegacyCleanupDone(true)
        r.setExactAlarmPromptShown(true)
        r.setNotificationPermissionAsked(true)
        r.setWelcomeNotificationInserted(true)
        val p = r.snapshot()
        assertThat(p).isEqualTo(
            UserPrefs(
                onboarded = true, userName = "n".repeat(24), appLanguage = AppLanguage.BO,
                appearance = Appearance.DARK, textSize = 32, recitationGoal = 1000,
                remindersEnabled = false, keepScreenOn = false, lastReadPrayerId = 5,
                legacyCleanupDone = true, exactAlarmPromptShown = true,
                notificationPermissionAsked = true, welcomeNotificationInserted = true,
            ),
        )
        r.setLastReadPrayerId(null)
        assertThat(r.snapshot().lastReadPrayerId).isNull()
    }
}
