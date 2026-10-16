package com.codingwithtashi.dailyprayer.core.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import com.codingwithtashi.dailyprayer.R

object NotificationChannels {
    const val ROUTINE_REMINDERS = "routine_reminders"
    const val UPDATES = "updates"

    fun ensure(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val nm = context.getSystemService(NotificationManager::class.java) ?: return
        nm.createNotificationChannel(
            NotificationChannel(
                ROUTINE_REMINDERS,
                context.getString(R.string.reminder),
                NotificationManager.IMPORTANCE_HIGH,
            ),
        )
        nm.createNotificationChannel(
            NotificationChannel(
                UPDATES,
                context.getString(R.string.notifications),
                NotificationManager.IMPORTANCE_DEFAULT,
            ),
        )
    }
}
