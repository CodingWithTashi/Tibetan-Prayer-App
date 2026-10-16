package com.codingwithtashi.dailyprayer.data.model

import java.time.LocalDate

data class RoutineCompletion(
    val routineId: Long,
    val prayerId: Int,
    val date: LocalDate,
    val completedAt: Long,
)
