package com.codingwithtashi.dailyprayer

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.preference.PreferenceManager
import com.codingwithtashi.dailyprayer.dao.NotificationDao
import com.codingwithtashi.dailyprayer.dao.PrayerDao
import com.codingwithtashi.dailyprayer.model.Prayer
import com.codingwithtashi.dailyprayer.model.PrayerNotification
import com.codingwithtashi.dailyprayer.utils.CommonUtils
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.*
import javax.inject.Inject

@AndroidEntryPoint
class MessagingService : FirebaseMessagingService() {
    @Inject
    lateinit var notificationDao: NotificationDao
    @Inject
    lateinit var prayerDao: PrayerDao
    override fun onMessageReceived(rm: RemoteMessage) {
        // create notification channel
        if(Build.VERSION.SDK_INT>= Build.VERSION_CODES.O){
            var channel = NotificationChannel("MyNotification","MyNotification",
                NotificationManager.IMPORTANCE_DEFAULT)
            var manager = getSystemService(NotificationManager::class.java) as NotificationManager;
            manager.createNotificationChannel(channel)
        }
        //log data
        Log.e("TAG", "onMessageReceived: "+rm.data["title"]+rm.data["prayer_title"]+rm.data["prayer_body"], )
        var link: String = rm.data["link"].toString();
        showNotification(rm.data["title"],rm.data["prayer_title"],rm.data["prayer_body"],link)


        super.onMessageReceived(rm);
    }

    private fun showNotification(
        title: String?,
        prayer_title: String?,
        prayer_body: String?,
        link: String
    ) {
        if(title!=null && prayer_title!=null && prayer_body!=null){
            val prayerNotification = PrayerNotification(null,title,prayer_title,
                CommonUtils.formatDateFromDate(Date()),link)

            val prayer = Prayer(null,prayer_title,prayer_body,"url",false,0)

            CoroutineScope(Dispatchers.Default).launch {
                if(link.isEmpty()){
                    prayerDao.insert(prayer);
                }
                notificationDao.insert(prayerNotification)
                Log.e("TAG", "onMessageReceived: new notification added", )
            }
        }

        val prefs: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
        var name = prefs.getString("signature", "");

        val notificationIntent =
            Intent(applicationContext, MainActivity::class.java)
        //create pending intent
        val contentIntent = PendingIntent.getActivity(
            this,
            0,
            notificationIntent,
            PendingIntent.FLAG_UPDATE_CURRENT
        )
        //create builder
        var builder = NotificationCompat.Builder(this,"MyNotification")
            .setContentTitle("Hi $name,$title")
            .setSmallIcon(R.drawable.prayer)
            .setContentText(prayer_title)
            .setContentIntent(contentIntent)
            .setAutoCancel(true)
        var manager = NotificationManagerCompat.from(this)
        manager.apply {
            notify(123,builder.build())
        }
    }
}