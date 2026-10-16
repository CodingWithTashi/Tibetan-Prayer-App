package com.codingwithtashi.dailyprayer

import android.app.Application
import com.codingwithtashi.dailyprayer.core.init.AppInitializer
import com.codingwithtashi.dailyprayer.core.notification.NotificationChannels
import com.google.firebase.messaging.FirebaseMessaging
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class PrayerApp : Application() {

    @Inject lateinit var initializer: AppInitializer

    override fun onCreate() {
        super.onCreate()
        NotificationChannels.ensure(this)
        initializer.start()
        FirebaseMessaging.getInstance().subscribeToTopic("all")
    }
}
