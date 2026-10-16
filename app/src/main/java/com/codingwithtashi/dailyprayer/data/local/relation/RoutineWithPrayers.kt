package com.codingwithtashi.dailyprayer.data.local.relation

import androidx.room.Embedded
import androidx.room.Junction
import androidx.room.Relation
import com.codingwithtashi.dailyprayer.data.local.entity.PrayerEntity
import com.codingwithtashi.dailyprayer.data.local.entity.RoutineEntity
import com.codingwithtashi.dailyprayer.data.local.entity.RoutinePrayerCrossRef

data class RoutineWithPrayers(
    @Embedded val routine: RoutineEntity,
    @Relation(parentColumn = "id", entityColumn = "routine_id")
    val refs: List<RoutinePrayerCrossRef>,
    @Relation(
        parentColumn = "id",
        entityColumn = "id",
        associateBy = Junction(RoutinePrayerCrossRef::class, parentColumn = "routine_id", entityColumn = "prayer_id"),
    )
    val prayers: List<PrayerEntity>,
) {
    /** Prayers ordered by cross-ref position. */
    fun orderedPrayers(): List<PrayerEntity> {
        val byId = prayers.associateBy { it.id }
        return refs.sortedBy { it.position }.mapNotNull { byId[it.prayerId] }
    }
}
