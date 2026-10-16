package com.codingwithtashi.dailyprayer.reminders

import androidx.core.app.NotificationManagerCompat
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class ReminderHealthTest {
    @Test fun fineWhenEverythingAllowed() {
        assertThat(ReminderHealth(true, true).issue).isNull()
    }

    @Test fun appNotificationsOffIsNotificationsIssue() {
        assertThat(ReminderHealth(false, true).issue).isEqualTo(ReminderIssue.NOTIFICATIONS_OFF)
    }

    @Test fun exactAlarmsOffAlone() {
        assertThat(ReminderHealth(true, false).issue).isEqualTo(ReminderIssue.EXACT_ALARMS_OFF)
    }

    @Test fun notificationsOutrankExactAlarms() {
        assertThat(ReminderHealth(false, false).issue).isEqualTo(ReminderIssue.NOTIFICATIONS_OFF)
    }

    @Test fun missingChannelCountsAsEnabled() {
        assertThat(ReminderHealth.of(true, null, true).notificationsUsable).isTrue()
        assertThat(ReminderHealth.of(true, null, true).issue).isNull()
    }

    @Test fun channelImportanceNoneIsBlocked() {
        val h = ReminderHealth.of(true, NotificationManagerCompat.IMPORTANCE_NONE, true)
        assertThat(h.notificationsUsable).isFalse()
        assertThat(h.issue).isEqualTo(ReminderIssue.NOTIFICATIONS_OFF)
        assertThat(ReminderHealth.of(true, NotificationManagerCompat.IMPORTANCE_HIGH, true).notificationsUsable).isTrue()
    }

    @Test fun appNotificationsOffWinsOverEnabledChannel() {
        assertThat(ReminderHealth.of(false, NotificationManagerCompat.IMPORTANCE_HIGH, true).notificationsUsable).isFalse()
    }

    @Test fun monitorRefreshRereadsChecker() {
        var current = ReminderHealth(false, true)
        val monitor = DefaultReminderHealthMonitor(object : ReminderHealthChecker {
            override fun check() = current
        })
        assertThat(monitor.health.value).isEqualTo(ReminderHealth(false, true))
        current = ReminderHealth(true, true)
        assertThat(monitor.health.value.issue).isEqualTo(ReminderIssue.NOTIFICATIONS_OFF)
        monitor.refresh()
        assertThat(monitor.health.value.issue).isNull()
    }
}
