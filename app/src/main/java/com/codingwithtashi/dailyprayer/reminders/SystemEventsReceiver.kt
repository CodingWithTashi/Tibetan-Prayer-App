package com.codingwithtashi.dailyprayer.reminders

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.codingwithtashi.dailyprayer.core.di.ApplicationScope
import com.codingwithtashi.dailyprayer.core.error.ErrorReporter
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/** Entry point for system broadcasts that invalidate alarms; see [SystemEventsHandler]. */
@AndroidEntryPoint
class SystemEventsReceiver : BroadcastReceiver() {
    @Inject lateinit var handler: SystemEventsHandler
    @Inject lateinit var errors: ErrorReporter
    @Inject @ApplicationScope lateinit var applicationScope: CoroutineScope

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        val pending = goAsync()
        applicationScope.launch {
            try {
                runReceiverWork(errors) { handler.handle(action) }
            } finally {
                pending.finish()
            }
        }
    }
}
