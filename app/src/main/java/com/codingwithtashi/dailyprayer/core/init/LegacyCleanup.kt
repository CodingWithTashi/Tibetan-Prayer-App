package com.codingwithtashi.dailyprayer.core.init

import android.app.AlarmManager
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.codingwithtashi.dailyprayer.data.prefs.UserPreferencesRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import androidx.core.content.edit
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/** Fresh-start wipe of everything the pre-3.0 app left behind. Runs once. */
@Singleton
class LegacyCleanup @Inject constructor(
    @ApplicationContext private val context: Context,
    private val prefs: UserPreferencesRepository,
) {
    suspend fun runOnce() {
        if (prefs.snapshot().legacyCleanupDone) return
        context.deleteDatabase(LEGACY_DB) // also removes -journal / -shm / -wal
        for (suffix in listOf("-shm", "-wal")) {
            context.getDatabasePath(LEGACY_DB + suffix).delete()
        }
        deletePrefs("${context.packageName}_preferences")
        deletePrefs("PrayerPreference")
        context.getExternalFilesDir(null)?.let { File(it, "prayer_audio").deleteRecursively() }
        cancelLegacyAlarm()
        context.getSystemService(NotificationManager::class.java)?.let {
            it.cancel(0)
            it.cancel(123)
        }
        prefs.setLegacyCleanupDone(true)
    }

    private fun deletePrefs(name: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            context.deleteSharedPreferences(name)
        } else {
            // commit = true: the file is deleted right after, so the write must be synchronous.
            context.getSharedPreferences(name, Context.MODE_PRIVATE).edit(commit = true) { clear() }
            File(context.applicationInfo.dataDir, "shared_prefs/$name.xml").delete()
        }
    }

    private fun cancelLegacyAlarm() {
        val am = context.getSystemService(AlarmManager::class.java) ?: return
        val intent = Intent().setClassName(context, LEGACY_ALARM_RECEIVER)
        val pi = PendingIntent.getBroadcast(
            context, 0, intent, PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE,
        )
        pi?.let {
            am.cancel(it)
            it.cancel()
        }
    }

    private companion object {
        const val LEGACY_DB = "Prayer_Database"
        const val LEGACY_ALARM_RECEIVER = "com.codingwithtashi.dailyprayer.AlarmReceiver"
    }
}
