package com.codingwithtashi.dailyprayer.data.model

import java.time.DayOfWeek

data class RoutineDraft(
    val id: Long?,
    val name: String,
    val timeMinutes: Int,
    val days: Set<DayOfWeek>,
    val reminderEnabled: Boolean,
    val prayerIds: List<Int>,
)
