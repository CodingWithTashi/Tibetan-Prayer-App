package com.codingwithtashi.dailyprayer.core.init

import android.util.Log
import com.codingwithtashi.dailyprayer.core.di.ApplicationScope
import com.codingwithtashi.dailyprayer.core.di.IoDispatcher
import com.codingwithtashi.dailyprayer.data.seed.PrayerSeeder
import com.codingwithtashi.dailyprayer.reminders.ReminderScheduler
import com.google.firebase.crashlytics.FirebaseCrashlytics
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
            step("rescheduleReminders") { reminderScheduler.rescheduleAll() }
        }
    }

    private inline fun step(name: String, block: () -> Unit) {
        runCatching(block).onFailure {
            Log.e(TAG, "Init step failed: $name", it)
            runCatching { FirebaseCrashlytics.getInstance().recordException(it) }
        }
    }

    private companion object {
        const val TAG = "AppInitializer"
    }
}
