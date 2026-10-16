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

    private const val ROUTINE_LINK_PREFIX = "dailyprayer://routine/"

    /** Inbox link that opens the routine; round-trips through [parseLink]. */
    fun routineLink(routineId: Long): String = ROUTINE_LINK_PREFIX + routineId

    /** Parses an in-app link stored on an inbox row; null for web URLs and anything unknown. */
    fun parseLink(link: String?): DeepLink? {
        if (link == null || !link.startsWith(ROUTINE_LINK_PREFIX)) return null
        return link.removePrefix(ROUTINE_LINK_PREFIX).toLongOrNull()?.takeIf { it > 0 }?.let { DeepLink.OpenRoutine(it) }
    }

    /** True when the system re-delivered an old intent (relaunch from Recents) that must not replay. */
    fun isReplay(intent: Intent?): Boolean =
        intent != null && intent.flags and Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY != 0

    /** Copy of [intent] without deep-link extras, so a recreate cannot replay the link. */
    fun stripped(intent: Intent): Intent = Intent(intent).apply {
        removeExtra(EXTRA_DESTINATION)
        removeExtra(EXTRA_ROUTINE_ID)
        removeExtra(EXTRA_PRAYER_ID)
    }

    /** True when navigating to [link] would just re-open the screen already showing. */
    fun isRedundant(link: DeepLink, currentRoutineId: Long?): Boolean =
        link is DeepLink.OpenRoutine && link.routineId == currentRoutineId

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
