package com.codingwithtashi.dailyprayer.reminders

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.codingwithtashi.dailyprayer.core.notification.DeepLink
import com.codingwithtashi.dailyprayer.core.notification.DeepLinks
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ReminderReceiverTest {
    @Test fun notificationIntentRoundTripsThroughDeepLinks() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val intent = ReminderReceiver.openRoutineIntent(context, 42L)
        assertThat(DeepLinks.parse(intent)).isEqualTo(DeepLink.OpenRoutine(42L))
    }
}
