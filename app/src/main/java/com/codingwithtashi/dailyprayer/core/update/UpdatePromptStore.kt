package com.codingwithtashi.dailyprayer.core.update

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.first

/** Process-wide instance: one DataStore per file for the whole process. */
private val Context.appUpdateDataStore: DataStore<Preferences> by preferencesDataStore(name = "app_update")

/** Remembers the last update the user declined, so [UpdatePolicy] can snooze the prompt. */
@Singleton
class UpdatePromptStore(private val store: DataStore<Preferences>) {
    @Inject constructor(@ApplicationContext context: Context) : this(context.appUpdateDataStore)

    /** (versionCode, declinedAtMs) of the last declined update, or null if none. */
    suspend fun declined(): Pair<Int, Long>? {
        val prefs = store.data.first()
        val code = prefs[DECLINED_VERSION_CODE] ?: return null
        val at = prefs[DECLINED_AT_MS] ?: return null
        return code to at
    }

    suspend fun recordDecline(versionCode: Int, atMs: Long) {
        store.edit {
            it[DECLINED_VERSION_CODE] = versionCode
            it[DECLINED_AT_MS] = atMs
        }
    }

    private companion object {
        val DECLINED_VERSION_CODE = intPreferencesKey("declined_version_code")
        val DECLINED_AT_MS = longPreferencesKey("declined_at_ms")
    }
}
