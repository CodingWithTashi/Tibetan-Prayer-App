package com.codingwithtashi.dailyprayer.core.init

import android.util.Log
import androidx.appcompat.app.AppCompatDelegate
import com.codingwithtashi.dailyprayer.core.di.ApplicationScope
import com.codingwithtashi.dailyprayer.core.di.IoDispatcher
import com.codingwithtashi.dailyprayer.core.error.ErrorReporter
import com.codingwithtashi.dailyprayer.core.locale.AppLanguage
import com.codingwithtashi.dailyprayer.data.prefs.UserPreferencesRepository
import com.codingwithtashi.dailyprayer.data.seed.PrayerSeeder
import com.codingwithtashi.dailyprayer.reminders.ReminderScheduler
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

@Singleton
class AppInitializer @Inject constructor(
    private val legacyCleanup: LegacyCleanup,
    private val seeder: PrayerSeeder,
    private val reminderScheduler: ReminderScheduler,
    private val prefs: UserPreferencesRepository,
    private val errors: ErrorReporter,
    @ApplicationScope private val scope: CoroutineScope,
    @IoDispatcher private val io: CoroutineDispatcher,
) {
    private val _ready = MutableStateFlow(false)
    val ready: StateFlow<Boolean> = _ready

    private var started = false

    @Synchronized
    fun start() {
        if (started) return
        started = true
        scope.launch(io) {
            step("legacyCleanup") { legacyCleanup.runOnce() }
            step("seed") { seeder.seed() }
            step("validateAudioFiles") { seeder.validateAudioFiles() }
            _ready.value = true
            // After ready: a DataStore read/write must not delay the splash; it only affects later reminder notifications.
            step("syncLanguage") { syncLanguage() }
            step("ensureReminders") { reminderScheduler.ensureScheduled() }
        }
    }

    /** Copies a per-app locale chosen in Android settings into prefs so reminders use the UI language. */
    private suspend fun syncLanguage() {
        val tag = AppCompatDelegate.getApplicationLocales().takeIf { !it.isEmpty }?.get(0)?.toLanguageTag()
        AppLanguage.syncTarget(tag, prefs.snapshot().appLanguage)?.let { prefs.setAppLanguage(it) }
    }

    private inline fun step(name: String, block: () -> Unit) {
        runCatching(block).onFailure {
            Log.e(TAG, "Init step failed: $name", it)
            errors.record(it)
        }
    }

    private companion object {
        const val TAG = "AppInitializer"
    }
}
