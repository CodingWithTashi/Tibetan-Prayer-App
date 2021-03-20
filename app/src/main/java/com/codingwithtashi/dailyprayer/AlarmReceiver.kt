package com.codingwithtashi.dailyprayer

import android.R.id.message
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.TaskStackBuilder
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.preference.PreferenceManager


/**
 * Created by kunchok on 19/03/2021
 */
class AlarmReceiver : BroadcastReceiver() {
    companion object{
        val NOTIFICATION_ID = 234

    }
    override fun onReceive(ctx: Context, intent: Intent) {
        Log.e("TAG", "onReceive: Called")
        val notificationManager = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_HIGH
            val mChannel = NotificationChannel("my_channel_01", "my_channel", importance)
            mChannel.description = "Description"
            mChannel.enableLights(true)
            mChannel.lightColor = Color.RED
            mChannel.enableVibration(true)
            mChannel.vibrationPattern = longArrayOf(100, 200, 300, 400, 500, 400, 300, 200, 400)
            mChannel.setShowBadge(false)
            notificationManager.createNotificationChannel(mChannel)
        }

        val prefs: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(ctx)
        var name = prefs.getString("signature", "");

        val builder = NotificationCompat.Builder(ctx, "my_channel_01")
            .setSmallIcon(R.drawable.bhudha)
            .setContentTitle("Prayer Time")
            .setContentText("Hey $name, It is Prayer time now")

        val resultIntent = Intent(ctx, MainActivity::class.java)
        val stackBuilder: TaskStackBuilder = TaskStackBuilder.create(ctx)
        stackBuilder.addParentStack(MainActivity::class.java)
        stackBuilder.addNextIntent(resultIntent)
        val resultPendingIntent: PendingIntent =
            stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT)
        builder.setContentIntent(resultPendingIntent)
        notificationManager.notify(NOTIFICATION_ID, builder.build())
    }
}