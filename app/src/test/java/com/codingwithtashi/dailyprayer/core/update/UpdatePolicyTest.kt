package com.codingwithtashi.dailyprayer.core.update

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class UpdatePolicyTest {
    private val now = 10_000_000_000L
    private val day = 24L * 60 * 60 * 1000

    private fun decide(
        available: Boolean = true,
        flexible: Boolean = true,
        immediate: Boolean = true,
        priority: Int = 0,
        version: Int = 20,
        declinedVersion: Int? = null,
        declinedAt: Long? = null,
    ) = UpdatePolicy.decide(available, flexible, immediate, priority, version, declinedVersion, declinedAt, now)

    @Test fun `no update available gives none`() {
        assertThat(decide(available = false, priority = 5)).isEqualTo(UpdateDecision.NONE)
    }

    @Test fun `high priority forces immediate even right after a decline`() {
        assertThat(decide(priority = 4, declinedVersion = 20, declinedAt = now)).isEqualTo(UpdateDecision.IMMEDIATE)
    }

    @Test fun `high priority without immediate support falls back to flexible`() {
        assertThat(decide(priority = 5, immediate = false)).isEqualTo(UpdateDecision.FLEXIBLE)
    }

    @Test fun `flexible not allowed gives none`() {
        assertThat(decide(flexible = false)).isEqualTo(UpdateDecision.NONE)
    }

    @Test fun `same version declined a day ago is snoozed`() {
        assertThat(decide(declinedVersion = 20, declinedAt = now - day)).isEqualTo(UpdateDecision.NONE)
    }

    @Test fun `same version declined four days ago prompts again`() {
        assertThat(decide(declinedVersion = 20, declinedAt = now - 4 * day)).isEqualTo(UpdateDecision.FLEXIBLE)
    }

    @Test fun `newer version than the declined one prompts immediately`() {
        assertThat(decide(version = 21, declinedVersion = 20, declinedAt = now)).isEqualTo(UpdateDecision.FLEXIBLE)
    }

    @Test fun `no decline prompts`() {
        assertThat(decide()).isEqualTo(UpdateDecision.FLEXIBLE)
    }
}
