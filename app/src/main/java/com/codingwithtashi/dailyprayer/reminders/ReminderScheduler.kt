package com.codingwithtashi.dailyprayer.reminders

import com.codingwithtashi.dailyprayer.data.model.Routine

interface ReminderScheduler {
    suspend fun schedule(routine: Routine)
    fun cancel(routineId: Long)
    suspend fun rescheduleAll()
}
