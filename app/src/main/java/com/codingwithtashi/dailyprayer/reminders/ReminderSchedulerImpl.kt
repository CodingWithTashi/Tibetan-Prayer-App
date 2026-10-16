package com.codingwithtashi.dailyprayer.reminders

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.AlarmManagerCompat
import com.codingwithtashi.dailyprayer.core.notification.DeepLinks
import com.codingwithtashi.dailyprayer.core.time.AppClock
import com.codingwithtashi.dailyprayer.data.model.Routine
import com.codingwithtashi.dailyprayer.data.prefs.UserPreferencesRepository
import com.codingwithtashi.dailyprayer.data.prefs.UserPrefs
import com.codingwithtashi.dailyprayer.data.repository.RoutineRepository
import dagger.Lazy
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.ZonedDateTime
import javax.inject.Inject
import javax.inject.Singleton

/**
 * AlarmManager-backed [ReminderScheduler]. [RoutineRepository] is injected lazily because the
 * repository itself depends on the scheduler.
 */
@Singleton
class ReminderSchedulerImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val alarmManager: AlarmManager,
    private val routineRepository: Lazy<RoutineRepository>,
    private val prefs: UserPreferencesRepository,
    private val clock: AppClock,
    private val state: ReminderStateStore,
) : ReminderScheduler {

    override suspend fun schedule(routine: Routine) = schedule(routine, prefs.snapshot())

    /** Replaces the routine's alarm; the stored state is overwritten when re-armed and removed otherwise. */
    private suspend fun schedule(routine: Routine, snapshot: UserPrefs) {
        cancelAlarm(routine.id)
        val armed = arm(routine, snapshot)
        if (armed != null) state.put(routine.id, armed) else state.remove(routine.id)
    }

    /** Arms the next occurrence, or returns null when the routine should have no alarm. */
    private fun arm(routine: Routine, snapshot: UserPrefs): ArmedAlarm? {
        if (!snapshot.remindersEnabled || !routine.reminderEnabled || routine.prayers.isEmpty()) return null
        val now = ZonedDateTime.ofInstant(clock.now(), clock.zone())
        val trigger = NextTriggerCalculator.next(now, routine.timeMinutes, DaysMask.fromSet(routine.days)) ?: return null
        val millis = trigger.toInstant().toEpochMilli()
        val pi = pendingIntent(routine.id, PendingIntent.FLAG_UPDATE_CURRENT) ?: return null
        var exact = ExactAlarmPermission.canScheduleExact(context)
        if (exact) {
            try {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, millis, pi)
            } catch (_: SecurityException) {
                // permission revoked in between: fall through to the inexact alarm
                exact = false
            }
        }
        // Inexact but still fires in Doze (unlike setWindow).
        if (!exact) AlarmManagerCompat.setAndAllowWhileIdle(alarmManager, AlarmManager.RTC_WAKEUP, millis, pi)
        return ArmedAlarm(millis, exact)
    }

    override suspend fun cancel(routineId: Long) {
        cancelAlarm(routineId)
        state.remove(routineId)
    }

    private fun cancelAlarm(routineId: Long) {
        pendingIntent(routineId, PendingIntent.FLAG_NO_CREATE)?.let {
            alarmManager.cancel(it)
            it.cancel()
        }
    }

    override suspend fun rescheduleAll() {
        val snapshot = prefs.snapshot()
        routineRepository.get().getRoutines().forEach { schedule(it, snapshot) }
    }

    override suspend fun ensureScheduled() {
        val exactNow = ExactAlarmPermission.canScheduleExact(context)
        val cutoff = clock.now().toEpochMilli() - STALE_GRACE_MS
        val snapshot = prefs.snapshot()
        val armedAlarms = state.getAll()
        routineRepository.get().getRoutines().forEach { routine ->
            val armed = armedAlarms[routine.id]
            val healthy = pendingIntent(routine.id, PendingIntent.FLAG_NO_CREATE) != null &&
                armed != null && armed.exact == exactNow && armed.triggerAtMillis > cutoff
            if (!healthy) schedule(routine, snapshot)
        }
    }

    private fun pendingIntent(routineId: Long, flag: Int): PendingIntent? {
        val intent = Intent(context, ReminderReceiver::class.java)
            .putExtra(DeepLinks.EXTRA_ROUTINE_ID, routineId)
        return PendingIntent.getBroadcast(context, routineId.toInt(), intent, flag or PendingIntent.FLAG_IMMUTABLE)
    }

    companion object {
        /**
         * How far past its trigger time an armed alarm is still trusted. An inexact alarm can fire late, so
         * a pending one slightly overdue is kept; one overdue by more than this has fired without being
         * re-armed (its PendingIntent outlives the firing) and is repaired.
         */
        const val STALE_GRACE_MS = 2 * 60 * 60 * 1000L
    }
}
