package com.codingwithtashi.dailyprayer.messaging

import com.codingwithtashi.dailyprayer.data.model.AppNotification
import com.codingwithtashi.dailyprayer.data.repository.NotificationRepository
import com.codingwithtashi.dailyprayer.data.repository.PrayerRepository
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import org.junit.Test

class FcmMessageHandlerTest {
    private val notifications = mockk<NotificationRepository>()
    private val prayers = mockk<PrayerRepository>()
    private val inserted = slot<AppNotification>()

    init {
        coEvery { notifications.insert(capture(inserted)) } returns 1L
        coEvery { prayers.upsertRemote(any(), any(), any(), any(), any()) } returns 1
    }

    private val handler = FcmMessageHandler(notifications, prayers)

    private suspend fun run(data: Map<String, String>, t: String? = null, b: String? = null) =
        handler.handle(data, t, b, "Daily Prayer", 42L)

    @Test fun titleOnlyInsertsRowWithoutPrayer() = runTest {
        val r = run(mapOf("title" to "Hello"))
        assertThat(inserted.captured.title).isEqualTo("Hello")
        assertThat(inserted.captured.receivedAt).isEqualTo(42L)
        assertThat(r.prayerUpserted).isFalse()
        coVerify(exactly = 0) { prayers.upsertRemote(any(), any(), any(), any(), any()) }
    }

    @Test fun prayerPayloadUpserts() = runTest {
        val r = run(
            mapOf(
                "title" to "New", "prayer_title" to " Tara ", "prayer_body" to "body",
                "download_link" to "https://x/a.mp3", "title_en" to "Tara EN", "category" to "daily",
            ),
        )
        assertThat(r.prayerUpserted).isTrue()
        assertThat(inserted.captured.content).isEqualTo("Tara")
        coVerify { prayers.upsertRemote("Tara", "body", "https://x/a.mp3", "Tara EN", "daily") }
    }

    @Test fun linkPresentSkipsUpsert() = runTest {
        val r = run(mapOf("title" to "N", "link" to "https://x", "prayer_title" to "P", "prayer_body" to "B"))
        assertThat(r.prayerUpserted).isFalse()
        assertThat(inserted.captured.link).isEqualTo("https://x")
    }

    @Test fun missingTitleFallsBack() = runTest {
        run(emptyMap(), "Notif title", "Notif body")
        assertThat(inserted.captured.title).isEqualTo("Notif title")
        assertThat(inserted.captured.content).isEqualTo("Notif body")
        run(emptyMap())
        assertThat(inserted.captured.title).isEqualTo("Daily Prayer")
        assertThat(inserted.captured.content).isEmpty()
    }
}
