package com.codingwithtashi.dailyprayer.reminders

import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import androidx.core.net.toUri
import android.os.Build
import android.provider.Settings

/** Helpers for the "Alarms & reminders" special access (API 31+). */
object ExactAlarmPermission {
    fun canScheduleExact(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return true
        val am = context.getSystemService(AlarmManager::class.java) ?: return true
        return am.canScheduleExactAlarms()
    }

    /** Settings screen for this app's exact-alarm access, or null below API 31. */
    fun requestIntent(context: Context): Intent? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return null
        return Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, "package:${context.packageName}".toUri())
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
}
