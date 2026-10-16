package com.codingwithtashi.dailyprayer.messaging

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.codingwithtashi.dailyprayer.MainActivity
import com.codingwithtashi.dailyprayer.R
import com.codingwithtashi.dailyprayer.core.di.ApplicationScope
import com.codingwithtashi.dailyprayer.core.locale.AppLocale
import com.codingwithtashi.dailyprayer.core.notification.DeepLinks
import com.codingwithtashi.dailyprayer.core.notification.NotificationChannels
import com.codingwithtashi.dailyprayer.data.prefs.UserPreferencesRepository
import com.codingwithtashi.dailyprayer.data.repository.NotificationRepository
import com.codingwithtashi.dailyprayer.data.repository.PrayerRepository
import com.codingwithtashi.dailyprayer.reminders.NotificationAccess
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@AndroidEntryPoint
class PrayerMessagingService : FirebaseMessagingService() {
    @Inject lateinit var notificationRepository: NotificationRepository
    @Inject lateinit var prayerRepository: PrayerRepository
    @Inject lateinit var prefs: UserPreferencesRepository
    @Inject @ApplicationScope lateinit var applicationScope: CoroutineScope

    @SuppressLint("MissingPermission") // guarded by NotificationAccess.canPost()
    override fun onMessageReceived(message: RemoteMessage) {
        val ctx = applicationContext
        applicationScope.launch {
            val localized = AppLocale.localizedContext(ctx, prefs.snapshot().appLanguage)
            val result = FcmMessageHandler(notificationRepository, prayerRepository).handle(
                data = message.data,
                fallbackTitle = message.notification?.title,
                fallbackBody = message.notification?.body,
                appName = localized.getString(R.string.app_name),
                now = System.currentTimeMillis(),
            )
            if (!NotificationAccess.canPost(ctx)) return@launch
            val n = result.notification
            NotificationChannels.ensure(ctx)
            val id = (n.title + n.receivedAt).hashCode()
            val intent = Intent(ctx, MainActivity::class.java)
                .setAction(Intent.ACTION_VIEW)
                .putExtra(DeepLinks.EXTRA_DESTINATION, DeepLinks.DEST_NOTIFICATIONS)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            val pi = PendingIntent.getActivity(
                ctx, id, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
            val notification = NotificationCompat.Builder(ctx, NotificationChannels.UPDATES)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(n.title)
                .setContentText(n.content)
                .setStyle(NotificationCompat.BigTextStyle().bigText(n.content))
                .setAutoCancel(true)
                .setContentIntent(pi)
                .build()
            runCatching { NotificationManagerCompat.from(ctx).notify(id, notification) }
        }
    }

    // Token is not used: the app subscribes to the "all" topic, which FCM re-associates automatically.
    @Suppress("OVERRIDE_DEPRECATION")
    override fun onNewToken(token: String) {
        Log.d(TAG, "onNewToken")
    }

    private companion object {
        const val TAG = "PrayerMessaging"
    }
}
