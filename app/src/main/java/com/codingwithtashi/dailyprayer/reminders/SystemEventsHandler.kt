package com.codingwithtashi.dailyprayer.reminders

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.content.Intent
import javax.inject.Inject

/** Re-arms every reminder after reboot, clock/zone changes, app update or exact-alarm access changes. */
class SystemEventsHandler @Inject constructor(private val scheduler: ReminderScheduler) {
    /** Re-arms every reminder when [action] is one this handler reacts to; ignores any other. */
    suspend fun handle(action: String?) {
        if (action in HANDLED) scheduler.rescheduleAll()
    }

    @SuppressLint("InlinedApi") // the constant is inlined; the broadcast only exists on API 31+
    private companion object {
        val HANDLED = setOf(
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            AlarmManager.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED,
        )
    }
}
