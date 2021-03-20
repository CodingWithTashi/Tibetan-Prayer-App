package com.codingwithtashi.dailyprayer

import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class MessagingService : FirebaseMessagingService() {
    override fun onMessageReceived(rm: RemoteMessage) {
        Log.e("TAG", "onMessageReceived: "+rm.notification?.title+rm.notification?.body, )
        super.onMessageReceived(rm);
        showNotification(rm.notification?.title,rm.notification?.body)
    }

    private fun showNotification(title: String?, body: String?) {

        if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.O){
            var channel = NotificationChannel("MyNotification","MyNotification",NotificationManager.IMPORTANCE_DEFAULT)
            var manager = getSystemService(NotificationManager::class.java) as NotificationManager;
            manager.createNotificationChannel(channel)
        }

        var builder = NotificationCompat.Builder(this,"MyNotification")
            .setContentTitle("New Prayer Notification")
            .setSmallIcon(R.drawable.prayer)
            .setContentText("New Prayer has been added in prayer")
        var manager = NotificationManagerCompat.from(this)
        manager.apply {
            notify(123,builder.build())
        }
    }
}