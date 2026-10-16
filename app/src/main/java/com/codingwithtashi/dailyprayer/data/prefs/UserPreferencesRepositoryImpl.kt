package com.codingwithtashi.dailyprayer.data.prefs

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.codingwithtashi.dailyprayer.core.locale.AppLanguage
import com.codingwithtashi.dailyprayer.data.model.AppTextSize
import com.codingwithtashi.dailyprayer.data.model.Appearance
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

@Singleton
class UserPreferencesRepositoryImpl @Inject constructor(
    private val store: DataStore<Preferences>,
) : UserPreferencesRepository {

    override val prefs: Flow<UserPrefs> = store.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it.toUserPrefs() }

    override suspend fun snapshot(): UserPrefs = prefs.first()

    override suspend fun setOnboarded(value: Boolean) = put(ONBOARDED, value)
    override suspend fun setUserName(name: String) = put(USER_NAME, name.trim().take(MAX_NAME))
    override suspend fun setAppLanguage(language: AppLanguage) = put(APP_LANGUAGE, language.tag)
    override suspend fun setAppearance(appearance: Appearance) = put(APPEARANCE, appearance.key)
    override suspend fun setTextSize(size: Int) = put(TEXT_SIZE, ReaderTextSize.clamp(size))
    override suspend fun setAppTextSize(size: AppTextSize) = put(APP_TEXT_SIZE, size.name)
    override suspend fun setRecitationGoal(goal: Int) = put(RECITATION_GOAL, goal)
    override suspend fun setRemindersEnabled(enabled: Boolean) = put(REMINDERS_ENABLED, enabled)
    override suspend fun setKeepScreenOn(enabled: Boolean) = put(KEEP_SCREEN_ON, enabled)
    override suspend fun setLastReadPrayerId(id: Int?) {
        store.edit { if (id == null) it.remove(LAST_READ_PRAYER_ID) else it[LAST_READ_PRAYER_ID] = id }
    }
    override suspend fun setLegacyCleanupDone(done: Boolean) = put(LEGACY_CLEANUP_DONE, done)
    override suspend fun setNotificationPermissionAsked(asked: Boolean) = put(NOTIFICATION_PERMISSION_ASKED, asked)
    override suspend fun setWelcomeNotificationInserted(inserted: Boolean) = put(WELCOME_NOTIFICATION_INSERTED, inserted)

    private suspend fun <T> put(key: Preferences.Key<T>, value: T) {
        store.edit { it[key] = value }
    }

    private fun Preferences.toUserPrefs() = UserPrefs(
        onboarded = this[ONBOARDED] ?: false,
        userName = this[USER_NAME] ?: "",
        appLanguage = AppLanguage.fromTag(this[APP_LANGUAGE]),
        appearance = Appearance.fromKey(this[APPEARANCE]),
        appTextSize = AppTextSize.fromName(this[APP_TEXT_SIZE]),
        textSize = ReaderTextSize.clamp(this[TEXT_SIZE] ?: ReaderTextSize.DEFAULT),
        recitationGoal = this[RECITATION_GOAL] ?: 108,
        remindersEnabled = this[REMINDERS_ENABLED] ?: true,
        keepScreenOn = this[KEEP_SCREEN_ON] ?: true,
        lastReadPrayerId = this[LAST_READ_PRAYER_ID],
        legacyCleanupDone = this[LEGACY_CLEANUP_DONE] ?: false,
        notificationPermissionAsked = this[NOTIFICATION_PERMISSION_ASKED] ?: false,
        welcomeNotificationInserted = this[WELCOME_NOTIFICATION_INSERTED] ?: false,
    )

    private companion object {
        const val MAX_NAME = 24
        val ONBOARDED = booleanPreferencesKey("onboarded")
        val USER_NAME = stringPreferencesKey("user_name")
        val APP_LANGUAGE = stringPreferencesKey("app_language")
        val APPEARANCE = stringPreferencesKey("appearance")
        val APP_TEXT_SIZE = stringPreferencesKey("app_text_size")
        val TEXT_SIZE = intPreferencesKey("text_size")
        val RECITATION_GOAL = intPreferencesKey("recitation_goal")
        val REMINDERS_ENABLED = booleanPreferencesKey("reminders_enabled")
        val KEEP_SCREEN_ON = booleanPreferencesKey("keep_screen_on")
        val LAST_READ_PRAYER_ID = intPreferencesKey("last_read_prayer_id")
        val LEGACY_CLEANUP_DONE = booleanPreferencesKey("legacy_cleanup_done")
        val NOTIFICATION_PERMISSION_ASKED = booleanPreferencesKey("notification_permission_asked")
        val WELCOME_NOTIFICATION_INSERTED = booleanPreferencesKey("welcome_notification_inserted")
    }
}
