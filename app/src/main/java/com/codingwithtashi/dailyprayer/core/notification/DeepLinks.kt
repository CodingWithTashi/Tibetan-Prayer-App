package com.codingwithtashi.dailyprayer.core.notification

import android.content.Intent

sealed interface DeepLink {
    data class OpenRoutine(val routineId: Long) : DeepLink
    data object OpenPlayer : DeepLink
    data object OpenLibrary : DeepLink
    data object OpenListen : DeepLink
    data class OpenPrayer(val prayerId: Int) : DeepLink
    data object OpenNotifications : DeepLink
}

object DeepLinks {
    const val EXTRA_DESTINATION = "destination"
    const val EXTRA_ROUTINE_ID = "routine_id"
    const val EXTRA_PRAYER_ID = "prayer_id"

    const val DEST_ROUTINE = "routine"
    const val DEST_PLAYER = "player"
    const val DEST_LIBRARY = "library"
    const val DEST_LISTEN = "listen"
    const val DEST_PRAYER = "prayer"
    const val DEST_NOTIFICATIONS = "notifications"

    fun parse(intent: Intent?): DeepLink? {
        intent ?: return null
        return when (intent.getStringExtra(EXTRA_DESTINATION)) {
            DEST_ROUTINE -> intent.getLongExtra(EXTRA_ROUTINE_ID, -1L).takeIf { it > 0 }?.let { DeepLink.OpenRoutine(it) }
            DEST_PLAYER -> DeepLink.OpenPlayer
            DEST_LIBRARY -> DeepLink.OpenLibrary
            DEST_LISTEN -> DeepLink.OpenListen
            DEST_PRAYER -> intent.getIntExtra(EXTRA_PRAYER_ID, -1).takeIf { it > 0 }?.let { DeepLink.OpenPrayer(it) }
            DEST_NOTIFICATIONS -> DeepLink.OpenNotifications
            else -> null
        }
    }
}
