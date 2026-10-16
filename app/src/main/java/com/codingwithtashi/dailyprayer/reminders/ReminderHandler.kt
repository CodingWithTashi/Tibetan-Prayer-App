package com.codingwithtashi.dailyprayer.reminders

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.codingwithtashi.dailyprayer.MainActivity
import com.codingwithtashi.dailyprayer.R
import com.codingwithtashi.dailyprayer.core.error.ErrorReporter
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
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException

/**
 * Handles a fired routine alarm: re-arms the next occurrence first, then posts the notification and
 * logs it to the inbox. Every step after the re-arm is isolated, so a failure while posting or
 * logging can never leave the routine without its next alarm; failures go to [ErrorReporter].
 */
class ReminderHandler @Inject constructor(
    @ApplicationContext private val context: Context,
    private val routineRepository: RoutineRepository,
    private val prefs: UserPreferencesRepository,
    private val notificationRepository: NotificationRepository,
    private val scheduler: ReminderScheduler,
    private val clock: AppClock,
    private val errors: ErrorReporter,
) {
    suspend fun handle(routineId: Long) {
        val routine = routineRepository.getRoutine(routineId)
        if (routine == null) {
            scheduler.cancel(routineId)
            return
        }
        // Self-guards disabled / empty routines, so it also cancels a stale alarm.
        guarded { scheduler.schedule(routine) }
        val snapshot = prefs.snapshot()
        if (!routine.reminderEnabled || !snapshot.remindersEnabled || routine.prayers.isEmpty()) return

        val count = routine.prayers.size
        val text = AppLocale.localizedContext(context, snapshot.appLanguage).resources.getQuantityString(
            R.plurals.reminder_notif_text, count, TibetanNumerals.formatCount(count, snapshot.appLanguage),
        )
        guarded { post(routine, text) }
        guarded {
            notificationRepository.insert(
                AppNotification(
                    title = routine.name, content = text, receivedAt = clock.now().toEpochMilli(),
                    link = DeepLinks.routineLink(routine.id),
                ),
            )
        }
    }

    @SuppressLint("MissingPermission") // guarded by NotificationAccess.canPost()
    private fun post(routine: Routine, text: String) {
        if (!NotificationAccess.canPost(context)) return
        NotificationChannels.ensure(context)
        NotificationManagerCompat.from(context).notify(routine.id.toInt(), buildNotification(routine, text))
    }

    private fun buildNotification(routine: Routine, text: String) =
        NotificationCompat.Builder(context, NotificationChannels.ROUTINE_REMINDERS)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(routine.name)
            .setContentText(text)
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(contentIntent(context, routine.id))
            .build()

    private suspend inline fun guarded(block: () -> Unit) {
        try {
            block()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            errors.record(e)
        }
    }

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
