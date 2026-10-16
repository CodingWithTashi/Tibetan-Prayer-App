package com.codingwithtashi.dailyprayer.messaging

import com.codingwithtashi.dailyprayer.data.model.AppNotification
import com.codingwithtashi.dailyprayer.data.repository.NotificationRepository
import com.codingwithtashi.dailyprayer.data.repository.PrayerRepository

/** Pure FCM payload handling: stores the inbox row and, for prayer payloads, upserts the prayer. */
class FcmMessageHandler(
    private val notifications: NotificationRepository,
    private val prayers: PrayerRepository,
) {
    data class Result(val notification: AppNotification, val prayerUpserted: Boolean)

    /**
     * @param data the FCM data map; @param fallbackTitle/@param fallbackBody come from the notification
     * payload; @param appName is the last-resort title.
     */
    suspend fun handle(
        data: Map<String, String>,
        fallbackTitle: String?,
        fallbackBody: String?,
        appName: String,
        now: Long,
    ): Result {
        val prayerTitle = data[KEY_PRAYER_TITLE]?.trim().orEmpty()
        val prayerBody = data[KEY_PRAYER_BODY].orEmpty()
        val link = data[KEY_LINK]?.takeIf { it.isNotBlank() }
        val notification = AppNotification(
            title = data[KEY_TITLE]?.takeIf { it.isNotBlank() } ?: fallbackTitle?.takeIf { it.isNotBlank() } ?: appName,
            content = prayerTitle.ifBlank { fallbackBody.orEmpty() },
            receivedAt = now,
            link = link,
        )
        notifications.insert(notification)
        val upsert = link == null && prayerTitle.isNotBlank() && prayerBody.isNotBlank()
        if (upsert) {
            prayers.upsertRemote(
                prayerTitle,
                prayerBody,
                data[KEY_DOWNLOAD_LINK]?.takeIf { it.isNotBlank() },
                data[KEY_TITLE_EN]?.takeIf { it.isNotBlank() },
                data[KEY_CATEGORY]?.takeIf { it.isNotBlank() },
            )
        }
        return Result(notification, upsert)
    }

    companion object {
        const val KEY_TITLE = "title"
        const val KEY_PRAYER_TITLE = "prayer_title"
        const val KEY_PRAYER_BODY = "prayer_body"
        const val KEY_DOWNLOAD_LINK = "download_link"
        const val KEY_LINK = "link"
        const val KEY_TITLE_EN = "title_en"
        const val KEY_CATEGORY = "category"
    }
}
