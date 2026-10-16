package com.codingwithtashi.dailyprayer.reminders

import com.codingwithtashi.dailyprayer.core.error.ErrorReporter
import kotlinx.coroutines.withTimeout

/** Budget for broadcast work held open with `goAsync()` (the system allows roughly 10 s). */
internal const val RECEIVER_TIMEOUT_MS = 8_000L

/**
 * Runs [block] within [timeoutMs]; any failure, including the timeout, is reported to [errors]
 * and never propagates, so the caller can always finish its pending broadcast.
 */
internal suspend fun runReceiverWork(errors: ErrorReporter, timeoutMs: Long = RECEIVER_TIMEOUT_MS, block: suspend () -> Unit) {
    try {
        withTimeout(timeoutMs) { block() }
    } catch (e: Exception) {
        errors.record(e)
    }
}
