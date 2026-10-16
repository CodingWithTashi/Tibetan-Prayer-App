package com.codingwithtashi.dailyprayer.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.codingwithtashi.dailyprayer.data.local.entity.RoutineCompletionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CompletionDao {
    @Query("SELECT * FROM routine_completions WHERE date = :date")
    fun observeForDate(date: String): Flow<List<RoutineCompletionEntity>>

    @Query("SELECT * FROM routine_completions WHERE routine_id = :routineId AND date = :date")
    suspend fun getForRoutineOnDate(routineId: Long, date: String): List<RoutineCompletionEntity>

    @Query("SELECT DISTINCT date FROM routine_completions")
    fun observeDates(): Flow<List<String>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIgnore(completion: RoutineCompletionEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAllIgnore(completions: List<RoutineCompletionEntity>)
}
