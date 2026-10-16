package com.codingwithtashi.dailyprayer.data.repository

import androidx.test.core.app.ApplicationProvider
import com.codingwithtashi.dailyprayer.data.FakeClock
import com.codingwithtashi.dailyprayer.data.RecordingScheduler
import com.codingwithtashi.dailyprayer.data.inMemoryDb
import com.codingwithtashi.dailyprayer.data.local.PrayerDatabase
import com.codingwithtashi.dailyprayer.data.local.entity.PrayerEntity
import com.codingwithtashi.dailyprayer.data.model.RoutineDraft
import com.google.common.truth.Truth.assertThat
import java.time.DayOfWeek
import java.time.LocalDate
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class RoutineRepositoryImplTest {
    private lateinit var db: PrayerDatabase
    private lateinit var repo: RoutineRepositoryImpl
    private lateinit var scheduler: RecordingScheduler
    private val ids = mutableListOf<Int>()

    @Before fun setUp() = runTest {
        db = inMemoryDb(ApplicationProvider.getApplicationContext())
        scheduler = RecordingScheduler()
        repo = RoutineRepositoryImpl(db.routineDao(), db.completionDao(), FakeClock(), scheduler)
        repeat(4) { ids += db.prayerDao().insert(PrayerEntity(slug = "p$it", title = "t$it")).toInt() }
    }

    @After fun tearDown() = db.close()

    private fun draft(id: Long? = null, name: String = "Morning", time: Int = 390, prayers: List<Int>) =
        RoutineDraft(id, name, time, setOf(DayOfWeek.MONDAY, DayOfWeek.SUNDAY), true, prayers)

    @Test fun saveKeepsPositionOrderAndDays() = runTest {
        val id = repo.save(draft(prayers = listOf(ids[2], ids[0], ids[1])))
        val r = repo.observeRoutine(id).first()!!
        assertThat(r.prayers.map { it.id }).containsExactly(ids[2], ids[0], ids[1]).inOrder()
        assertThat(r.days).containsExactly(DayOfWeek.MONDAY, DayOfWeek.SUNDAY)
        assertThat(scheduler.scheduled).containsExactly(id)
    }

    @Test fun reSaveReplacesPrayersAndKeepsCreatedAt() = runTest {
        val id = repo.save(draft(prayers = listOf(ids[0], ids[1])))
        val created = repo.getRoutine(id)!!.createdAt
        repo.save(draft(id = id, name = "Renamed", prayers = listOf(ids[3])))
        val r = repo.getRoutine(id)!!
        assertThat(r.name).isEqualTo("Renamed")
        assertThat(r.prayers.map { it.id }).containsExactly(ids[3])
        assertThat(r.createdAt).isEqualTo(created)
        assertThat(repo.getRoutines()).hasSize(1)
    }

    @Test fun deleteCascadesAndRestoreBringsBackSameId() = runTest {
        val id = repo.save(draft(prayers = listOf(ids[0], ids[1])))
        repo.markDone(id, ids[0])
        val routine = repo.getRoutine(id)!!
        repo.delete(id)
        assertThat(repo.getRoutine(id)).isNull()
        assertThat(db.openHelper.readableDatabase.query("SELECT COUNT(*) FROM routine_prayers").use { it.moveToFirst(); it.getInt(0) }).isEqualTo(0)
        assertThat(repo.observeCompletions(LocalDate.of(2026, 3, 4)).first()).isEmpty()
        assertThat(scheduler.cancelled).contains(id)

        repo.restore(routine)
        val back = repo.getRoutine(id)!!
        assertThat(back.id).isEqualTo(id)
        assertThat(back.prayers.map { it.id }).containsExactly(ids[0], ids[1]).inOrder()
        assertThat(repo.observeCompletions(LocalDate.of(2026, 3, 4)).first()).hasSize(1)
    }

    @Test fun markDoneIsIdempotent() = runTest {
        val id = repo.save(draft(prayers = listOf(ids[0])))
        val day = LocalDate.of(2026, 3, 4)
        repo.markDone(id, ids[0], day)
        repo.markDone(id, ids[0], day)
        assertThat(repo.observeCompletions(day).first()).hasSize(1)
        assertThat(repo.observeCompletionDates().first()).containsExactly(day)
    }

    @Test fun markDoneForMissingRoutineDoesNotThrow() = runTest {
        repo.markDone(999, ids[0])
        assertThat(repo.observeCompletionDates().first()).isEmpty()
    }

    @Test fun routinesOrderedByTime() = runTest {
        val late = repo.save(draft(name = "Late", time = 1200, prayers = listOf(ids[0])))
        val early = repo.save(draft(name = "Early", time = 360, prayers = listOf(ids[0])))
        assertThat(repo.observeRoutines().first().map { it.id }).containsExactly(early, late).inOrder()
    }
}
