package com.codingwithtashi.dailyprayer.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "routine_prayers",
    primaryKeys = ["routine_id", "prayer_id"],
    foreignKeys = [
        ForeignKey(RoutineEntity::class, ["id"], ["routine_id"], onDelete = ForeignKey.CASCADE),
        ForeignKey(PrayerEntity::class, ["id"], ["prayer_id"], onDelete = ForeignKey.CASCADE),
    ],
    indices = [Index("prayer_id")],
)
data class RoutinePrayerCrossRef(
    @ColumnInfo(name = "routine_id") val routineId: Long,
    @ColumnInfo(name = "prayer_id") val prayerId: Int,
    val position: Int,
)
