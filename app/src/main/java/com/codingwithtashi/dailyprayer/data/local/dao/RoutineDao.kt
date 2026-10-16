package com.codingwithtashi.dailyprayer.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.codingwithtashi.dailyprayer.data.local.entity.RoutineEntity
import com.codingwithtashi.dailyprayer.data.local.entity.RoutinePrayerCrossRef
import com.codingwithtashi.dailyprayer.data.local.relation.RoutineWithPrayers
import kotlinx.coroutines.flow.Flow

@Dao
abstract class RoutineDao {
    @Transaction
    @Query("SELECT * FROM routines ORDER BY time_minutes, id")
    abstract fun observeAll(): Flow<List<RoutineWithPrayers>>

    @Transaction
    @Query("SELECT * FROM routines ORDER BY time_minutes, id")
    abstract suspend fun getAll(): List<RoutineWithPrayers>

    @Transaction
    @Query("SELECT * FROM routines WHERE id = :id")
    abstract fun observe(id: Long): Flow<RoutineWithPrayers?>

    @Transaction
    @Query("SELECT * FROM routines WHERE id = :id")
    abstract suspend fun get(id: Long): RoutineWithPrayers?

    @Query("SELECT * FROM routines WHERE id = :id")
    abstract suspend fun getEntity(id: Long): RoutineEntity?

    @Upsert
    protected abstract suspend fun upsert(entity: RoutineEntity): Long

    @Query("DELETE FROM routine_prayers WHERE routine_id = :routineId")
    protected abstract suspend fun deleteRefs(routineId: Long)

    @Insert
    protected abstract suspend fun insertRefs(refs: List<RoutinePrayerCrossRef>)

    @Query("DELETE FROM routines WHERE id = :id")
    abstract suspend fun delete(id: Long)

    /** Upserts the routine (keeping an explicit id) and replaces its prayers, preserving [prayerIds] order. */
    @Transaction
    open suspend fun save(entity: RoutineEntity, prayerIds: List<Int>): Long {
        val rowId = upsert(entity)
        val id = if (entity.id != 0L) entity.id else rowId
        deleteRefs(id)
        insertRefs(prayerIds.distinct().mapIndexed { index, pid -> RoutinePrayerCrossRef(id, pid, index) })
        return id
    }
}
