package com.codingwithtashi.dailyprayer.core.update

import java.util.concurrent.TimeUnit

enum class UpdateDecision { NONE, FLEXIBLE, IMMEDIATE }

/** Pure decision for whether and how to offer a Play in-app update. */
object UpdatePolicy {
    /** Play update priority is 0..5; 4 and above forces an immediate update. */
    const val HIGH_PRIORITY = 4

    val DECLINE_SNOOZE_MS = TimeUnit.DAYS.toMillis(3)

    fun decide(
        updateAvailable: Boolean,
        flexibleAllowed: Boolean,
        immediateAllowed: Boolean,
        priority: Int,
        availableVersionCode: Int,
        declinedVersionCode: Int?,
        declinedAtMs: Long?,
        nowMs: Long,
    ): UpdateDecision {
        if (!updateAvailable) return UpdateDecision.NONE
        if (priority >= HIGH_PRIORITY && immediateAllowed) return UpdateDecision.IMMEDIATE
        if (!flexibleAllowed) return UpdateDecision.NONE
        val snoozed = declinedVersionCode == availableVersionCode &&
            declinedAtMs != null && nowMs - declinedAtMs < DECLINE_SNOOZE_MS
        return if (snoozed) UpdateDecision.NONE else UpdateDecision.FLEXIBLE
    }
}
