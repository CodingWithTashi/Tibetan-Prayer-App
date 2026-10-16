package com.codingwithtashi.dailyprayer.reminders

import com.codingwithtashi.dailyprayer.data.model.Routine

/** Arms and cancels one alarm per routine reminder. */
interface ReminderScheduler {
    /** Replaces the routine's alarm; arms nothing if reminders or the routine reminder are off or it has no prayers. */
    suspend fun schedule(routine: Routine)
    /** Cancels the routine's alarm and forgets its armed state. */
    suspend fun cancel(routineId: Long)

    /** Cancels and re-arms every routine. Used after boot, clock/zone changes, updates and permission grants. */
    suspend fun rescheduleAll()

    /**
     * App-start check, per routine. A routine is re-armed unless its alarm is verifiably healthy: a live
     * PendingIntent, a recorded [ArmedAlarm] armed with the current exact-alarm capability, and a trigger
     * time that is not stale (see [ReminderSchedulerImpl.STALE_GRACE_MS]).
     *
     * Two cases need this. Revoking SCHEDULE_EXACT_ALARM cancels the app's exact alarms without sending a
     * broadcast, so a recorded `exact` that no longer matches the capability must be repaired here. And a
     * fired alarm's PendingIntent stays alive, so a live PendingIntent alone does not prove an alarm is
     * armed; a routine whose re-arm failed is told apart by its stale trigger time. A full reschedule on
     * every start would instead drop a late-but-pending alarm when the process was started by that very alarm.
     */
    suspend fun ensureScheduled()
}
