package com.codingwithtashi.dailyprayer.reminders

import android.app.AlarmManager
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.codingwithtashi.dailyprayer.ui.FakeClock
import com.codingwithtashi.dailyprayer.ui.FakeReminderStateStore
import com.codingwithtashi.dailyprayer.data.prefs.UserPrefs
import com.codingwithtashi.dailyprayer.data.repository.RoutineRepository
import com.codingwithtashi.dailyprayer.ui.testRoutine
import com.codingwithtashi.dailyprayer.ui.FakePrefs
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.mockk
import java.time.Instant
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.shadows.ShadowAlarmManager

@RunWith(RobolectricTestRunner::class)
class ReminderSchedulerImplTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val alarmManager = context.getSystemService(AlarmManager::class.java)
    private val shadow = shadowOf(alarmManager)
    private val routines = mockk<RoutineRepository>()
    private val prefs = FakePrefs(UserPrefs(remindersEnabled = true))
    private val clock = FakeClock() // 2026-03-04T08:00Z (UTC)
    private val state = FakeReminderStateStore()
    private lateinit var scheduler: ReminderSchedulerImpl

    @Before fun setUp() {
        scheduler = ReminderSchedulerImpl(context, alarmManager, dagger.Lazy { routines }, prefs, clock, state)
        ShadowAlarmManager.setCanScheduleExactAlarms(true)
    }

    private val tenAm = Instant.parse("2026-03-04T10:00:00Z").toEpochMilli()

    @Test fun schedulesExactWhenPermitted() = runTest {
        scheduler.schedule(testRoutine(time = 600))
        val alarm = shadow.peekNextScheduledAlarm()!!
        assertThat(alarm.triggerAtMs).isEqualTo(tenAm)
        assertThat(alarm.type).isEqualTo(AlarmManager.RTC_WAKEUP)
        assertThat(alarm.allowWhileIdle).isTrue()
        assertThat(state.armed[1L]).isEqualTo(ArmedAlarm(tenAm, exact = true))
    }

    @Test fun fallsBackToAllowWhileIdleWithoutExactPermission() = runTest {
        ShadowAlarmManager.setCanScheduleExactAlarms(false)
        scheduler.schedule(testRoutine(time = 600))
        val alarm = shadow.peekNextScheduledAlarm()!!
        assertThat(alarm.triggerAtMs).isEqualTo(tenAm)
        assertThat(alarm.allowWhileIdle).isTrue()
        assertThat(alarm.interval).isEqualTo(0L)
        assertThat(state.armed[1L]).isEqualTo(ArmedAlarm(tenAm, exact = false))
    }

    @Test fun nothingWhenGloballyOff() = runTest {
        prefs.setRemindersEnabled(false)
        scheduler.schedule(testRoutine())
        assertThat(shadow.peekNextScheduledAlarm()).isNull()
    }

    @Test fun schedulingWithoutAlarmClearsStoredState() = runTest {
        scheduler.schedule(testRoutine())
        assertThat(state.armed).hasSize(1)
        prefs.setRemindersEnabled(false)
        scheduler.schedule(testRoutine())
        assertThat(state.armed).isEmpty()
    }

    @Test fun nothingWhenRoutineReminderOff() = runTest {
        scheduler.schedule(testRoutine(reminder = false))
        assertThat(shadow.peekNextScheduledAlarm()).isNull()
    }

    @Test fun nothingWithoutPrayers() = runTest {
        scheduler.schedule(testRoutine(prayers = emptyList()))
        assertThat(shadow.peekNextScheduledAlarm()).isNull()
    }

    @Test fun rescheduleReplacesAndCancelRemoves() = runTest {
        scheduler.schedule(testRoutine(time = 600))
        scheduler.schedule(testRoutine(time = 660))
        assertThat(shadow.scheduledAlarms).hasSize(1)
        assertThat(shadow.peekNextScheduledAlarm()!!.triggerAtMs).isEqualTo(Instant.parse("2026-03-04T11:00:00Z").toEpochMilli())
        scheduler.cancel(1)
        assertThat(shadow.peekNextScheduledAlarm()).isNull()
        assertThat(state.armed).isEmpty()
    }

    @Test fun rescheduleAllSchedulesEachRoutine() = runTest {
        coEvery { routines.getRoutines() } returns listOf(testRoutine(id = 1), testRoutine(id = 2, time = 700), testRoutine(id = 3, reminder = false))
        scheduler.rescheduleAll()
        assertThat(shadow.scheduledAlarms).hasSize(2)
    }

    @Test fun rescheduleAllWithRemindersOffCancelsExistingAlarms() = runTest {
        val routine = testRoutine(id = 1)
        coEvery { routines.getRoutines() } returns listOf(routine)
        scheduler.schedule(routine)
        assertThat(shadow.scheduledAlarms).hasSize(1)
        prefs.setRemindersEnabled(false)
        scheduler.rescheduleAll()
        assertThat(shadow.scheduledAlarms).isEmpty()
    }

    @Test fun ensureScheduledLeavesHealthyAlarmUntouched() = runTest {
        val routine = testRoutine(id = 1, time = 600)
        coEvery { routines.getRoutines() } returns listOf(routine, testRoutine(id = 2, time = 700))
        scheduler.schedule(routine)
        val before = shadow.scheduledAlarms.single()
        scheduler.ensureScheduled()
        assertThat(shadow.scheduledAlarms).hasSize(2)
        assertThat(shadow.scheduledAlarms).contains(before)
    }

    @Test fun ensureScheduledRearmsWhenCapabilityChanged() = runTest {
        val routine = testRoutine(id = 1, time = 600)
        coEvery { routines.getRoutines() } returns listOf(routine)
        scheduler.schedule(routine)
        ShadowAlarmManager.setCanScheduleExactAlarms(false)
        scheduler.ensureScheduled()
        assertThat(shadow.scheduledAlarms).hasSize(1)
        assertThat(state.armed[1L]).isEqualTo(ArmedAlarm(tenAm, exact = false))
    }

    @Test fun ensureScheduledArmsRoutineWithNoRecordedState() = runTest {
        coEvery { routines.getRoutines() } returns listOf(testRoutine(id = 1))
        scheduler.ensureScheduled()
        assertThat(shadow.scheduledAlarms).hasSize(1)
        assertThat(state.armed[1L]).isEqualTo(ArmedAlarm(tenAm, exact = true))
    }

    @Test fun ensureScheduledKeepsLateAlarmWithinGrace() = runTest {
        val routine = testRoutine(id = 1, time = 600)
        coEvery { routines.getRoutines() } returns listOf(routine)
        scheduler.schedule(routine)
        val before = shadow.scheduledAlarms.single()
        clock.instant = Instant.parse("2026-03-04T11:30:00Z") // 1.5h past trigger, inexact alarms may be this late
        scheduler.ensureScheduled()
        assertThat(shadow.scheduledAlarms).containsExactly(before)
    }

    @Test fun ensureScheduledRepairsAlarmThatFiredWithoutRearm() = runTest {
        val routine = testRoutine(id = 1, time = 600)
        coEvery { routines.getRoutines() } returns listOf(routine)
        scheduler.schedule(routine)
        // The PendingIntent is still alive after the alarm fired, but the re-arm never happened.
        clock.instant = Instant.parse("2026-03-04T12:30:00Z")
        scheduler.ensureScheduled()
        assertThat(shadow.scheduledAlarms).hasSize(1)
        assertThat(shadow.peekNextScheduledAlarm()!!.triggerAtMs).isEqualTo(Instant.parse("2026-03-05T10:00:00Z").toEpochMilli())
        assertThat(state.armed[1L]!!.triggerAtMillis).isEqualTo(Instant.parse("2026-03-05T10:00:00Z").toEpochMilli())
    }

    @Test fun rescheduleAllKeepsPerRoutineExactFlag() = runTest {
        coEvery { routines.getRoutines() } returns listOf(testRoutine(id = 1), testRoutine(id = 2, time = 700))
        scheduler.rescheduleAll()
        assertThat(state.armed.values.map { it.exact }).containsExactly(true, true)
        ShadowAlarmManager.setCanScheduleExactAlarms(false)
        scheduler.schedule(testRoutine(id = 2, time = 700))
        assertThat(state.armed[1L]!!.exact).isTrue()
        assertThat(state.armed[2L]!!.exact).isFalse()
    }
}
