package com.codingwithtashi.dailyprayer.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "routines")
data class RoutineEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    @ColumnInfo(name = "time_minutes") val timeMinutes: Int,
    @ColumnInfo(name = "days_mask") val daysMask: Int,
    @ColumnInfo(name = "reminder_enabled", defaultValue = "1") val reminderEnabled: Boolean = true,
    @ColumnInfo(name = "sort_order", defaultValue = "0") val sortOrder: Int = 0,
    @ColumnInfo(name = "created_at") val createdAt: Long,
)
