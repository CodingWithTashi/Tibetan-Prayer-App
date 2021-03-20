package com.codingwithtashi.dailyprayer

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import android.util.Log
import com.google.firebase.ktx.Firebase
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.ktx.messaging
import dagger.hilt.android.HiltAndroidApp

/**
 * Created by kunchok on 11/03/2021
 */
@HiltAndroidApp
class PrayerApp : Application() {
    override fun onCreate() {

        Log.e("TAG", "onCreate: CREATED", )

        FirebaseMessaging.getInstance().subscribeToTopic("all")
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Log.e("TAG", "onCreate: subscribeToTopic", )
                }else{
                    Log.e("TAG", "onCreate: subscribeToTopic failed", )
                }
            }
        super.onCreate()
    }
}