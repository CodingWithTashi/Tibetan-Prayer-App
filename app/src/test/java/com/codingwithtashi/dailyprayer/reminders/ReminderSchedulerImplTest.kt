package com.codingwithtashi.dailyprayer.reminders

import android.app.AlarmManager
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.codingwithtashi.dailyprayer.data.FakeClock
import com.codingwithtashi.dailyprayer.data.prefs.UserPreferencesRepository
import com.codingwithtashi.dailyprayer.data.prefs.UserPrefs
import com.codingwithtashi.dailyprayer.data.repository.RoutineRepository
import com.codingwithtashi.dailyprayer.ui.routine.testRoutine
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
    private val prefs = mockk<UserPreferencesRepository>()
    private val clock = FakeClock() // 2026-03-04T08:00Z (UTC)
    private lateinit var scheduler: ReminderSchedulerImpl

    @Before fun setUp() {
        coEvery { prefs.snapshot() } returns UserPrefs(remindersEnabled = true)
        scheduler = ReminderSchedulerImpl(context, alarmManager, dagger.Lazy { routines }, prefs, clock)
        ShadowAlarmManager.setCanScheduleExactAlarms(true)
    }

    private val tenAm = Instant.parse("2026-03-04T10:00:00Z").toEpochMilli()

    @Test fun schedulesExactWhenPermitted() = runTest {
        scheduler.schedule(testRoutine(time = 600))
        val alarm = shadow.peekNextScheduledAlarm()!!
        assertThat(alarm.triggerAtMs).isEqualTo(tenAm)
        assertThat(alarm.type).isEqualTo(AlarmManager.RTC_WAKEUP)
        assertThat(alarm.allowWhileIdle).isTrue()
    }

    @Test fun fallsBackToWindowWithoutExactPermission() = runTest {
        ShadowAlarmManager.setCanScheduleExactAlarms(false)
        scheduler.schedule(testRoutine(time = 600))
        val alarm = shadow.peekNextScheduledAlarm()!!
        assertThat(alarm.triggerAtMs).isEqualTo(tenAm)
        assertThat(alarm.allowWhileIdle).isFalse()
    }

    @Test fun nothingWhenGloballyOff() = runTest {
        coEvery { prefs.snapshot() } returns UserPrefs(remindersEnabled = false)
        scheduler.schedule(testRoutine())
        assertThat(shadow.peekNextScheduledAlarm()).isNull()
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
    }

    @Test fun rescheduleAllSchedulesEachRoutine() = runTest {
        coEvery { routines.getRoutines() } returns listOf(testRoutine(id = 1), testRoutine(id = 2, time = 700), testRoutine(id = 3, reminder = false))
        scheduler.rescheduleAll()
        assertThat(shadow.scheduledAlarms).hasSize(2)
    }
}
