package com.codingwithtashi.dailyprayer.data.model

import java.time.DayOfWeek

data class Routine(
    val id: Long,
    val name: String,
    val timeMinutes: Int,
    val days: Set<DayOfWeek>,
    val reminderEnabled: Boolean,
    val prayers: List<Prayer>,
    val createdAt: Long,
)
