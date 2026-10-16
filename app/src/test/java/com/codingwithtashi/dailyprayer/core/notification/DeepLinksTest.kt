package com.codingwithtashi.dailyprayer.core.notification

import android.content.Intent
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class DeepLinksTest {
    @Test fun routineLinkRoundTrips() {
        assertThat(DeepLinks.parseLink(DeepLinks.routineLink(7))).isEqualTo(DeepLink.OpenRoutine(7))
    }

    @Test fun parseLinkIgnoresWebAndGarbage() {
        assertThat(DeepLinks.parseLink("https://example.com")).isNull()
        assertThat(DeepLinks.parseLink(null)).isNull()
        assertThat(DeepLinks.parseLink("dailyprayer://routine/abc")).isNull()
        assertThat(DeepLinks.parseLink("dailyprayer://routine/0")).isNull()
    }

    @Test fun redundantOnlyForSameRoutine() {
        assertThat(DeepLinks.isRedundant(DeepLink.OpenRoutine(3), 3)).isTrue()
        assertThat(DeepLinks.isRedundant(DeepLink.OpenRoutine(3), 4)).isFalse()
        assertThat(DeepLinks.isRedundant(DeepLink.OpenRoutine(3), null)).isFalse()
        assertThat(DeepLinks.isRedundant(DeepLink.OpenNotifications, 3)).isFalse()
    }

    @Test fun replayDetectedFromHistoryFlag() {
        assertThat(DeepLinks.isReplay(Intent().addFlags(Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY))).isTrue()
        assertThat(DeepLinks.isReplay(Intent())).isFalse()
        assertThat(DeepLinks.isReplay(null)).isFalse()
    }

    @Test fun strippedRemovesLinkExtras() {
        val original = Intent()
            .putExtra(DeepLinks.EXTRA_DESTINATION, DeepLinks.DEST_ROUTINE)
            .putExtra(DeepLinks.EXTRA_ROUTINE_ID, 5L)
        val stripped = DeepLinks.stripped(original)
        assertThat(DeepLinks.parse(stripped)).isNull()
        assertThat(DeepLinks.parse(original)).isEqualTo(DeepLink.OpenRoutine(5))
    }
}
