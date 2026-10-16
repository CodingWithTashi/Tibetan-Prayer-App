package com.codingwithtashi.dailyprayer.reminders

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.codingwithtashi.dailyprayer.core.di.ApplicationScope
import com.codingwithtashi.dailyprayer.core.error.ErrorReporter
import com.codingwithtashi.dailyprayer.core.notification.DeepLinks
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/** Fires at a routine's reminder time and delegates to [ReminderHandler] within the broadcast time budget. */
@AndroidEntryPoint
class ReminderReceiver : BroadcastReceiver() {
    @Inject lateinit var handler: ReminderHandler
    @Inject lateinit var errors: ErrorReporter
    @Inject @ApplicationScope lateinit var applicationScope: CoroutineScope

    override fun onReceive(context: Context, intent: Intent) {
        val routineId = intent.getLongExtra(DeepLinks.EXTRA_ROUTINE_ID, -1L)
        if (routineId <= 0) return
        val pending = goAsync()
        applicationScope.launch {
            try {
                runReceiverWork(errors) { handler.handle(routineId) }
            } finally {
                pending.finish()
            }
        }
    }
}
