package com.codingwithtashi.dailyprayer.data.repository

import com.codingwithtashi.dailyprayer.data.model.Routine
import com.codingwithtashi.dailyprayer.data.model.RoutineCompletion
import com.codingwithtashi.dailyprayer.data.model.RoutineDraft
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow

interface RoutineRepository {
    fun observeRoutines(): Flow<List<Routine>>
    fun observeRoutine(id: Long): Flow<Routine?>

    /** One-shot reads for receivers / schedulers (not in the Part D contract). */
    suspend fun getRoutine(id: Long): Routine?
    suspend fun getRoutines(): List<Routine>

    suspend fun save(draft: RoutineDraft): Long
    suspend fun delete(id: Long)
    suspend fun restore(routine: Routine)
    fun observeCompletions(date: LocalDate): Flow<List<RoutineCompletion>>
    fun observeCompletionDates(): Flow<Set<LocalDate>>
    suspend fun markDone(routineId: Long, prayerId: Int, date: LocalDate? = null)
}
