package com.codingwithtashi.dailyprayer.reminders

import android.content.Context
import androidx.core.app.NotificationManagerCompat
import com.codingwithtashi.dailyprayer.core.notification.NotificationChannels
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Why a reminder may not behave as the user expects. */
enum class ReminderIssue {
    /** App notifications are disabled, or the routine reminder channel is blocked. The reminder will not be seen. */
    NOTIFICATIONS_OFF,

    /** Exact alarms are not allowed, so the reminder may arrive later than its set time. */
    EXACT_ALARMS_OFF,
}

/** Snapshot of the device state that decides whether a routine reminder is shown on time. */
data class ReminderHealth(
    /** App notifications are on and the routine reminder channel is not blocked. */
    val notificationsUsable: Boolean,
    val exactAlarmsAllowed: Boolean,
) {
    /** The most important problem, or null when reminders are fine. Blocked notifications outrank imprecise timing. */
    val issue: ReminderIssue?
        get() = when {
            !notificationsUsable -> ReminderIssue.NOTIFICATIONS_OFF
            !exactAlarmsAllowed -> ReminderIssue.EXACT_ALARMS_OFF
            else -> null
        }

    companion object {
        /**
         * Builds a snapshot from raw platform values. A null [channelImportance] means the channel does not
         * exist yet (it is created on demand), which counts as enabled.
         */
        fun of(notificationsEnabled: Boolean, channelImportance: Int?, exactAlarmsAllowed: Boolean) = ReminderHealth(
            notificationsUsable = notificationsEnabled && channelImportance != NotificationManagerCompat.IMPORTANCE_NONE,
            exactAlarmsAllowed = exactAlarmsAllowed,
        )
    }
}

/** Reads the current [ReminderHealth]; injectable so screens can be tested without a device. */
interface ReminderHealthChecker {
    fun check(): ReminderHealth
}

/**
 * Single app-wide source of the current [ReminderHealth]. It is refreshed when the app returns to the
 * foreground (a fix made in system settings resumes the activity), so screens just observe [health].
 */
interface ReminderHealthMonitor {
    val health: StateFlow<ReminderHealth>

    /** Re-reads the device state. */
    fun refresh()
}

/** [ReminderHealthMonitor] backed by a [ReminderHealthChecker]; checks once at construction. */
class DefaultReminderHealthMonitor(private val checker: ReminderHealthChecker) : ReminderHealthMonitor {
    private val _health = MutableStateFlow(checker.check())
    override val health: StateFlow<ReminderHealth> = _health.asStateFlow()

    override fun refresh() {
        _health.value = checker.check()
    }
}

/** Platform-backed [ReminderHealthChecker]. */
class AndroidReminderHealthChecker(private val context: Context) : ReminderHealthChecker {
    override fun check(): ReminderHealth {
        val nm = NotificationManagerCompat.from(context)
        return ReminderHealth.of(
            notificationsEnabled = nm.areNotificationsEnabled(),
            channelImportance = nm.getNotificationChannelCompat(NotificationChannels.ROUTINE_REMINDERS)?.importance,
            exactAlarmsAllowed = ExactAlarmPermission.canScheduleExact(context),
        )
    }
}
