package com.codingwithtashi.dailyprayer.reminders

import com.codingwithtashi.dailyprayer.data.prefs.UserPreferencesRepository
import javax.inject.Inject

/**
 * The single rule for the runtime notification permission dialog. Android stops showing it after repeated
 * denials without saying so, so the app tracks whether it has launched the dialog before.
 */
class NotificationPermissionPolicy @Inject constructor(private val prefs: UserPreferencesRepository) {
    /**
     * True when the system dialog can still be shown: it was never launched, or Android says a rationale
     * applies ([shouldShowRationale]). Otherwise the only way to enable notifications is the system settings.
     */
    private suspend fun canRequest(shouldShowRationale: Boolean): Boolean =
        shouldShowRationale || !prefs.snapshot().notificationPermissionAsked

    /** Records that the system dialog is being launched. */
    private suspend fun markAsked() = prefs.setNotificationPermissionAsked(true)

    /** [canRequest], and when true also [markAsked]. A true result means the caller must launch the dialog now. */
    suspend fun claimRequest(shouldShowRationale: Boolean): Boolean {
        if (!canRequest(shouldShowRationale)) return false
        markAsked()
        return true
    }
}
