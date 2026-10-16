package com.codingwithtashi.dailyprayer.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "routine_completions",
    primaryKeys = ["routine_id", "prayer_id", "date"],
    foreignKeys = [
        ForeignKey(RoutineEntity::class, ["id"], ["routine_id"], onDelete = ForeignKey.CASCADE),
        ForeignKey(PrayerEntity::class, ["id"], ["prayer_id"], onDelete = ForeignKey.CASCADE),
    ],
    indices = [Index("date"), Index("prayer_id")],
)
data class RoutineCompletionEntity(
    @ColumnInfo(name = "routine_id") val routineId: Long,
    @ColumnInfo(name = "prayer_id") val prayerId: Int,
    /** Local date, "yyyy-MM-dd". */
    val date: String,
    @ColumnInfo(name = "completed_at") val completedAt: Long,
)
