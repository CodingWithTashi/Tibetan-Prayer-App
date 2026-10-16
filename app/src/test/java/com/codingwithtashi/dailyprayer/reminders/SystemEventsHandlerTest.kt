package com.codingwithtashi.dailyprayer.reminders

import android.content.Intent
import com.codingwithtashi.dailyprayer.ui.RecordingScheduler
import com.codingwithtashi.dailyprayer.ui.RecordingErrorReporter
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.test.runTest
import org.junit.Test

class SystemEventsHandlerTest {
    private val scheduler = RecordingScheduler()
    private val handler = SystemEventsHandler(scheduler)
    private val errors = RecordingErrorReporter()

    @Test fun rescheduleAllOnHandledActions() = runTest {
        handler.handle(Intent.ACTION_BOOT_COMPLETED)
        handler.handle(Intent.ACTION_TIMEZONE_CHANGED)
        assertThat(scheduler.rescheduledAll).isEqualTo(2)
    }

    @Test fun ignoresOtherActions() = runTest {
        handler.handle(Intent.ACTION_BATTERY_LOW)
        handler.handle(null)
        assertThat(scheduler.rescheduledAll).isEqualTo(0)
    }

    @Test fun failureIsReportedNotThrown() = runTest {
        runReceiverWork(errors) { throw IllegalStateException("x") }
        assertThat(errors.errors.single()).hasMessageThat().isEqualTo("x")
    }

    @Test fun timeoutIsReportedNotThrown() = runTest {
        runReceiverWork(errors, timeoutMs = 50) { CompletableDeferred<Unit>().await() }
        assertThat(errors.errors).hasSize(1)
        assertThat(errors.errors.single()).isInstanceOf(kotlinx.coroutines.TimeoutCancellationException::class.java)
    }
}
