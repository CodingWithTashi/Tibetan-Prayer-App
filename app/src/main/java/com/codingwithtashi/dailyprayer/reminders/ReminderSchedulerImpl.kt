package com.codingwithtashi.dailyprayer.reminders

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.codingwithtashi.dailyprayer.core.notification.DeepLinks
import com.codingwithtashi.dailyprayer.core.time.AppClock
import com.codingwithtashi.dailyprayer.data.model.Routine
import com.codingwithtashi.dailyprayer.data.prefs.UserPreferencesRepository
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
) : ReminderScheduler {

    override suspend fun schedule(routine: Routine) {
        cancel(routine.id)
        if (!prefs.snapshot().remindersEnabled || !routine.reminderEnabled || routine.prayers.isEmpty()) return
        val now = ZonedDateTime.ofInstant(clock.now(), clock.zone())
        val trigger = NextTriggerCalculator.next(now, routine.timeMinutes, DaysMask.fromSet(routine.days)) ?: return
        val millis = trigger.toInstant().toEpochMilli()
        val pi = pendingIntent(routine.id, PendingIntent.FLAG_UPDATE_CURRENT) ?: return
        val exact = Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()
        if (exact) {
            try {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, millis, pi)
                return
            } catch (_: SecurityException) {
                // permission revoked in between: fall through to the inexact window
            }
        }
        alarmManager.setWindow(AlarmManager.RTC_WAKEUP, millis, WINDOW_MS, pi)
    }

    override fun cancel(routineId: Long) {
        val pi = pendingIntent(routineId, PendingIntent.FLAG_NO_CREATE) ?: return
        alarmManager.cancel(pi)
        pi.cancel()
    }

    override suspend fun rescheduleAll() {
        routineRepository.get().getRoutines().forEach { schedule(it) }
    }

    private fun pendingIntent(routineId: Long, flag: Int): PendingIntent? {
        val intent = Intent(context, ReminderReceiver::class.java)
            .putExtra(DeepLinks.EXTRA_ROUTINE_ID, routineId)
        return PendingIntent.getBroadcast(context, routineId.toInt(), intent, flag or PendingIntent.FLAG_IMMUTABLE)
    }

    private companion object {
        const val WINDOW_MS = 10 * 60_000L
    }
}
