package com.codingwithtashi.dailyprayer.reminders

/** An alarm the scheduler armed for one routine. */
data class ArmedAlarm(
    /** Epoch millis the alarm was set to fire at. */
    val triggerAtMillis: Long,
    /** True when it was armed as an exact alarm, false for the inexact fallback. */
    val exact: Boolean,
)

/**
 * Scheduler-internal bookkeeping of which alarm is armed for each routine. Not user preferences: it only
 * lets [ReminderScheduler.ensureScheduled] tell a live alarm from one that fired and was never re-armed.
 */
interface ReminderStateStore {
    /** The alarm last armed for [routineId], or null when none is recorded. */
    suspend fun get(routineId: Long): ArmedAlarm?

    /** Every recorded alarm keyed by routine id, in one read. */
    suspend fun getAll(): Map<Long, ArmedAlarm>

    /** Records [alarm] as the armed alarm for [routineId]. */
    suspend fun put(routineId: Long, alarm: ArmedAlarm)

    /** Forgets the alarm recorded for [routineId]. */
    suspend fun remove(routineId: Long)
}
