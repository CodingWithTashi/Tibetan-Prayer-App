package com.codingwithtashi.dailyprayer.reminders

import com.codingwithtashi.dailyprayer.data.prefs.UserPrefs
import com.codingwithtashi.dailyprayer.ui.FakePrefs
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Test

class NotificationPermissionPolicyTest {
    private val prefs = FakePrefs(UserPrefs())
    private val policy = NotificationPermissionPolicy(prefs)

    @Test fun canRequestWhenNeverAsked() = runTest {
        assertThat(policy.claimRequest(shouldShowRationale = false)).isTrue()
    }

    @Test fun cannotRequestAfterAskedWithoutRationale() = runTest {
        policy.claimRequest(shouldShowRationale = false)
        assertThat(policy.claimRequest(shouldShowRationale = false)).isFalse()
    }

    @Test fun rationaleAllowsRequestEvenAfterAsked() = runTest {
        policy.claimRequest(shouldShowRationale = false)
        assertThat(policy.claimRequest(shouldShowRationale = true)).isTrue()
    }

    @Test fun claimMarksAskedOnlyWhenGranted() = runTest {
        assertThat(policy.claimRequest(false)).isTrue()
        assertThat(prefs.state.value.notificationPermissionAsked).isTrue()
        assertThat(policy.claimRequest(false)).isFalse()
    }

    @Test fun deniedClaimDoesNotTouchPrefs() = runTest {
        val asked = FakePrefs(UserPrefs(notificationPermissionAsked = true))
        assertThat(NotificationPermissionPolicy(asked).claimRequest(false)).isFalse()
        assertThat(asked.state.value.notificationPermissionAsked).isTrue()
    }
}
