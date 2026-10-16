package com.codingwithtashi.dailyprayer.reminders

import android.Manifest
import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.codingwithtashi.dailyprayer.MainActivity
import com.codingwithtashi.dailyprayer.R
import com.codingwithtashi.dailyprayer.core.di.ApplicationScope
import com.codingwithtashi.dailyprayer.core.locale.AppLocale
import com.codingwithtashi.dailyprayer.core.notification.DeepLinks
import com.codingwithtashi.dailyprayer.core.notification.NotificationChannels
import com.codingwithtashi.dailyprayer.core.text.TibetanNumerals
import com.codingwithtashi.dailyprayer.core.time.AppClock
import com.codingwithtashi.dailyprayer.data.model.AppNotification
import com.codingwithtashi.dailyprayer.data.model.Routine
import com.codingwithtashi.dailyprayer.data.prefs.UserPreferencesRepository
import com.codingwithtashi.dailyprayer.data.repository.NotificationRepository
import com.codingwithtashi.dailyprayer.data.repository.RoutineRepository
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/** Fires at a routine's reminder time: posts the notification, logs it to the inbox, re-arms the next one. */
@AndroidEntryPoint
class ReminderReceiver : BroadcastReceiver() {
    @Inject lateinit var routineRepository: RoutineRepository
    @Inject lateinit var notificationRepository: NotificationRepository
    @Inject lateinit var prefs: UserPreferencesRepository
    @Inject lateinit var scheduler: ReminderScheduler
    @Inject lateinit var clock: AppClock
    @Inject @ApplicationScope lateinit var applicationScope: CoroutineScope

    override fun onReceive(context: Context, intent: Intent) {
        val routineId = intent.getLongExtra(DeepLinks.EXTRA_ROUTINE_ID, -1L)
        if (routineId <= 0) return
        val pending = goAsync()
        applicationScope.launch {
            try {
                handle(context.applicationContext, routineId)
            } finally {
                pending.finish()
            }
        }
    }

    @SuppressLint("MissingPermission") // guarded by canPostNotifications()
    private suspend fun handle(context: Context, routineId: Long) {
        val routine = routineRepository.getRoutine(routineId) ?: return
        val snapshot = prefs.snapshot()
        if (!routine.reminderEnabled || !snapshot.remindersEnabled) return
        val localized = AppLocale.localizedContext(context, snapshot.appLanguage)
        val n = routine.prayers.size
        val text = localized.resources.getQuantityString(
            R.plurals.reminder_notif_text, n, TibetanNumerals.formatCount(n, snapshot.appLanguage),
        )
        if (canPostNotifications(context)) {
            NotificationChannels.ensure(context)
            runCatching {
                NotificationManagerCompat.from(context).notify(routine.id.toInt(), buildNotification(context, routine, text))
            }
        }
        notificationRepository.insert(
            AppNotification(title = routine.name, content = text, receivedAt = clock.now().toEpochMilli(), link = null),
        )
        scheduler.schedule(routine)
    }

    private fun buildNotification(context: Context, routine: Routine, text: String) =
        NotificationCompat.Builder(context, NotificationChannels.ROUTINE_REMINDERS)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(routine.name)
            .setContentText(text)
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(contentIntent(context, routine.id))
            .build()

    private fun canPostNotifications(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    companion object {
        /** The intent the notification opens; round-trips through [DeepLinks.parse]. */
        fun openRoutineIntent(context: Context, routineId: Long): Intent =
            Intent(context, MainActivity::class.java)
                .setAction(Intent.ACTION_VIEW)
                .putExtra(DeepLinks.EXTRA_DESTINATION, DeepLinks.DEST_ROUTINE)
                .putExtra(DeepLinks.EXTRA_ROUTINE_ID, routineId)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)

        private fun contentIntent(context: Context, routineId: Long): PendingIntent =
            PendingIntent.getActivity(
                context, routineId.toInt(), openRoutineIntent(context, routineId),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
    }
}
