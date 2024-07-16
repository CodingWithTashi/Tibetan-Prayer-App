package com.codingwithtashi.dailyprayer

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.BitmapFactory
import android.graphics.Color
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.preference.PreferenceManager
import com.codingwithtashi.dailyprayer.ui.activity.AlarmActivity.Companion.NOTIFICATION_CHANNEL_ID
import com.codingwithtashi.dailyprayer.dao.NotificationDao
import com.codingwithtashi.dailyprayer.model.PrayerNotification
import com.codingwithtashi.dailyprayer.ui.activity.AlarmActivity
import com.codingwithtashi.dailyprayer.ui.activity.MainActivity
import com.codingwithtashi.dailyprayer.utils.CommonUtils
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.*
import javax.inject.Inject


/**
 * Created by kunchok on 19/03/2021
 */
@AndroidEntryPoint
 class AlarmReceiver : BroadcastReceiver() {
    @Inject
    lateinit var notificationDao: NotificationDao
    override fun onReceive(context: Context?, intent: Intent?) {
        Log.e("TAG", "onReceive: ::::::::::", )
    if(context!=null){

        val prefs: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
        val name = prefs.getString("signature", "");
        val time = prefs.getString("alarm_time_in_hhmm", "");
        val isAlarmOn = prefs.getBoolean("is_alarm_on", false);
        Log.e("TAG", "onReceive: $isAlarmOn", )
        if(isAlarmOn){
            val intent = Intent(context, MainActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            val resultPendingIntent = PendingIntent.getActivity(
                context,
                0 /* Request code */, intent,
                PendingIntent.FLAG_UPDATE_CURRENT
            )
            val mBuilder: Notification.Builder =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    Notification.Builder(context, NOTIFICATION_CHANNEL_ID)
                } else {
                    Notification.Builder(context)
                }
            val prayerNotification = PrayerNotification(null,"Prayer Time","Hey $name, It is Prayer time now",
                CommonUtils.formatDateFromDate(Date()),"")

            CoroutineScope(Dispatchers.Default).launch {
                notificationDao.insert(prayerNotification)
                Log.e("TAG", "onMessageReceived: new notification added", )
            }
            mBuilder.setContentTitle("Prayer Time")
                .setContentText("Hey $name, It is Prayer time now")
                .setAutoCancel(true)
                .setSmallIcon(R.drawable.prayer)
                .setLargeIcon(BitmapFactory.decodeResource(context?.resources, R.drawable.bhudha))
                .setContentIntent(resultPendingIntent)
            val mNotificationManager =
                context?.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val importance = NotificationManager.IMPORTANCE_HIGH
                val notificationChannel = NotificationChannel(
                    NOTIFICATION_CHANNEL_ID,
                    "NOTIFICATION_CHANNEL_NAME",
                    importance
                )
                notificationChannel.enableLights(true)
                notificationChannel.lightColor = Color.RED
                notificationChannel.enableVibration(true)
                notificationChannel.setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION),null)
                notificationChannel.vibrationPattern = longArrayOf(
                    100,
                    200,
                    300,
                    400,
                    500,
                    400,
                    300,
                    200,
                    400
                )
                assert(mNotificationManager != null)
                mBuilder.setChannelId(NOTIFICATION_CHANNEL_ID)
                mNotificationManager.createNotificationChannel(notificationChannel)
            }
            assert(mNotificationManager != null)
            mNotificationManager.notify(0 /* Request Code */, mBuilder.build())
            if(time?.isNotEmpty()!! && isAlarmOn)
                AlarmActivity.scheduleNotification(context,time,isAlarmOn);
        }

    }
    }

}