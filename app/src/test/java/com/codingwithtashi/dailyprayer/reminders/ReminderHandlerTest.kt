package com.codingwithtashi.dailyprayer.reminders

import android.Manifest
import android.app.Application
import android.app.NotificationManager
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.codingwithtashi.dailyprayer.core.locale.AppLanguage
import com.codingwithtashi.dailyprayer.core.notification.DeepLink
import com.codingwithtashi.dailyprayer.core.notification.DeepLinks
import com.codingwithtashi.dailyprayer.ui.FakeClock
import com.codingwithtashi.dailyprayer.ui.RecordingScheduler
import com.codingwithtashi.dailyprayer.data.prefs.UserPrefs
import com.codingwithtashi.dailyprayer.ui.testRoutine
import com.codingwithtashi.dailyprayer.ui.FakeNotificationRepo
import com.codingwithtashi.dailyprayer.ui.FakePrefs
import com.codingwithtashi.dailyprayer.ui.FakeRoutineRepo
import com.codingwithtashi.dailyprayer.ui.RecordingErrorReporter
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf

@RunWith(RobolectricTestRunner::class)
class ReminderHandlerTest {
    private val app = ApplicationProvider.getApplicationContext<Application>()
    private val notifications = shadowOf(app.getSystemService(NotificationManager::class.java))
    private val routines = FakeRoutineRepo()
    private val prefs = FakePrefs(UserPrefs(remindersEnabled = true))
    private val inbox = FakeNotificationRepo()
    private val scheduler = RecordingScheduler()
    private val errors = RecordingErrorReporter()
    private val handler = ReminderHandler(app, routines, prefs, inbox, scheduler, FakeClock(), errors)

    private fun grant() = shadowOf(app).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
    private fun deny() = shadowOf(app).denyPermissions(Manifest.permission.POST_NOTIFICATIONS)

    @Test fun postsNotificationWithNameAndCount() = runTest {
        grant()
        routines.routines.value = listOf(testRoutine(id = 5, name = "Morning"))
        handler.handle(5)
        val n = notifications.allNotifications.single()
        assertThat(n.extras.getString("android.title")).isEqualTo("Morning")
        assertThat(n.extras.getCharSequence("android.text").toString()).contains("3")
        assertThat(inbox.inserted.single().title).isEqualTo("Morning")
    }

    @Test fun tibetanCountUsesTibetanDigits() = runTest {
        grant()
        prefs.setAppLanguage(AppLanguage.BO)
        routines.routines.value = listOf(testRoutine(id = 5))
        handler.handle(5)
        assertThat(notifications.allNotifications.single().extras.getCharSequence("android.text").toString()).contains("༣")
    }

    @Test fun reArmsBeforePosting() = runTest {
        grant()
        routines.routines.value = listOf(testRoutine(id = 5))
        var scheduledWhenInserted = false
        val repo = object : com.codingwithtashi.dailyprayer.data.repository.NotificationRepository by inbox {
            override suspend fun insert(notification: com.codingwithtashi.dailyprayer.data.model.AppNotification): Long {
                scheduledWhenInserted = scheduler.scheduled.isNotEmpty()
                return inbox.insert(notification)
            }
        }
        ReminderHandler(app, routines, prefs, repo, scheduler, FakeClock(), errors).handle(5)
        assertThat(scheduledWhenInserted).isTrue()
        assertThat(scheduler.calls.first()).isEqualTo("schedule:5")
    }

    @Test fun nothingPostedWhenRoutineReminderOff() = runTest {
        grant()
        routines.routines.value = listOf(testRoutine(id = 5, reminder = false))
        handler.handle(5)
        assertSilentButRearmed()
    }

    @Test fun nothingPostedWhenGloballyOff() = runTest {
        grant()
        prefs.setRemindersEnabled(false)
        routines.routines.value = listOf(testRoutine(id = 5))
        handler.handle(5)
        assertSilentButRearmed()
    }

    @Test fun nothingPostedWhenNoPrayers() = runTest {
        grant()
        routines.routines.value = listOf(testRoutine(id = 5, prayers = emptyList()))
        handler.handle(5)
        assertSilentButRearmed()
    }

    private fun assertSilentButRearmed() {
        assertThat(notifications.allNotifications).isEmpty()
        assertThat(inbox.inserted).isEmpty()
        assertThat(scheduler.scheduled).containsExactly(5L)
    }

    @Test fun missingRoutineIsCancelled() = runTest {
        handler.handle(9)
        assertThat(scheduler.cancelled).containsExactly(9L)
        assertThat(scheduler.scheduled).isEmpty()
    }

    @Test fun permissionDeniedStillLogsAndReArms() = runTest {
        deny()
        routines.routines.value = listOf(testRoutine(id = 5))
        handler.handle(5)
        assertThat(notifications.allNotifications).isEmpty()
        assertThat(inbox.inserted).hasSize(1)
        assertThat(scheduler.scheduled).containsExactly(5L)
    }

    @Test fun inboxFailureIsReportedAfterReArm() = runTest {
        grant()
        routines.routines.value = listOf(testRoutine(id = 5))
        val boom = IllegalStateException("db")
        inbox.insertFailure = boom
        handler.handle(5)
        assertThat(scheduler.scheduled).containsExactly(5L)
        assertThat(notifications.allNotifications).hasSize(1)
        assertThat(errors.errors).containsExactly(boom)
    }

    @Test fun inboxRowLinkOpensTheRoutine() = runTest {
        grant()
        routines.routines.value = listOf(testRoutine(id = 5))
        handler.handle(5)
        assertThat(DeepLinks.parseLink(inbox.inserted.single().link)).isEqualTo(DeepLink.OpenRoutine(5))
    }

    @Test fun notificationIntentRoundTripsThroughDeepLinks() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val intent = ReminderHandler.openRoutineIntent(context, 42L)
        assertThat(DeepLinks.parse(intent)).isEqualTo(DeepLink.OpenRoutine(42L))
    }
}
