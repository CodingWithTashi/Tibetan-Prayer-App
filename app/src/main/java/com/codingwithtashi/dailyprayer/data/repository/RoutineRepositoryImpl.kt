package com.codingwithtashi.dailyprayer.data.repository

import android.database.sqlite.SQLiteConstraintException
import com.codingwithtashi.dailyprayer.core.time.AppClock
import com.codingwithtashi.dailyprayer.data.local.dao.CompletionDao
import com.codingwithtashi.dailyprayer.data.local.dao.RoutineDao
import com.codingwithtashi.dailyprayer.data.local.entity.RoutineCompletionEntity
import com.codingwithtashi.dailyprayer.data.local.entity.RoutineEntity
import com.codingwithtashi.dailyprayer.reminders.DaysMask
import com.codingwithtashi.dailyprayer.data.model.Routine
import com.codingwithtashi.dailyprayer.data.model.RoutineCompletion
import com.codingwithtashi.dailyprayer.data.model.RoutineDraft
import com.codingwithtashi.dailyprayer.reminders.ReminderScheduler
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Singleton
class RoutineRepositoryImpl @Inject constructor(
    private val dao: RoutineDao,
    private val completionDao: CompletionDao,
    private val clock: AppClock,
    private val scheduler: ReminderScheduler,
) : RoutineRepository {

    /** Today's completions of the most recently deleted routines, so Undo can restore them. */
    private val deletedCompletions = HashMap<Long, List<RoutineCompletionEntity>>()

    override fun observeRoutines(): Flow<List<Routine>> = dao.observeAll().map { l -> l.map { it.toDomain() } }
    override fun observeRoutine(id: Long): Flow<Routine?> = dao.observe(id).map { it?.toDomain() }
    override suspend fun getRoutine(id: Long): Routine? = dao.get(id)?.toDomain()
    override suspend fun getRoutines(): List<Routine> = dao.getAll().map { it.toDomain() }

    override suspend fun save(draft: RoutineDraft): Long {
        val existing = draft.id?.let { dao.getEntity(it) }
        val entity = RoutineEntity(
            id = existing?.id ?: 0,
            name = draft.name.trim(),
            timeMinutes = draft.timeMinutes.coerceIn(0, 1439),
            daysMask = DaysMask.fromSet(draft.days),
            reminderEnabled = draft.reminderEnabled,
            sortOrder = existing?.sortOrder ?: 0,
            createdAt = existing?.createdAt ?: clock.now().toEpochMilli(),
        )
        val id = dao.save(entity, draft.prayerIds)
        dao.get(id)?.toDomain()?.let { scheduler.schedule(it) }
        return id
    }

    override suspend fun delete(id: Long) {
        deletedCompletions[id] = completionDao.getForRoutineOnDate(id, clock.today().toString())
        scheduler.cancel(id)
        dao.delete(id)
    }

    override suspend fun restore(routine: Routine) {
        val entity = RoutineEntity(
            id = routine.id,
            name = routine.name,
            timeMinutes = routine.timeMinutes,
            daysMask = DaysMask.fromSet(routine.days),
            reminderEnabled = routine.reminderEnabled,
            createdAt = routine.createdAt,
        )
        dao.save(entity, routine.prayers.map { it.id })
        deletedCompletions.remove(routine.id)?.let { completionDao.insertAllIgnore(it) }
        dao.get(routine.id)?.toDomain()?.let { scheduler.schedule(it) }
    }

    override fun observeCompletions(date: LocalDate): Flow<List<RoutineCompletion>> =
        completionDao.observeForDate(date.toString()).map { l -> l.map { it.toDomain() } }

    override fun observeCompletionDates(): Flow<Set<LocalDate>> =
        completionDao.observeDates().map { l -> l.map { LocalDate.parse(it) }.toSet() }

    override suspend fun markDone(routineId: Long, prayerId: Int, date: LocalDate?) {
        val day = date ?: clock.today()
        try {
            completionDao.insertIgnore(
                RoutineCompletionEntity(routineId, prayerId, day.toString(), clock.now().toEpochMilli()),
            )
        } catch (_: SQLiteConstraintException) {
            // Routine or prayer was deleted while it was playing; nothing to record.
        }
    }
}
