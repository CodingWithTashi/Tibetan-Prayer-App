package com.codingwithtashi.dailyprayer.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.room.Room
import com.codingwithtashi.dailyprayer.data.local.PrayerDatabase
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

fun inMemoryDb(context: Context): PrayerDatabase =
    Room.inMemoryDatabaseBuilder(context, PrayerDatabase::class.java).allowMainThreadQueries().build()

fun tempDataStore(): DataStore<Preferences> {
    val file = File.createTempFile("user_prefs", ".preferences_pb").also { it.delete(); it.deleteOnExit() }
    return PreferenceDataStoreFactory.create(scope = CoroutineScope(Dispatchers.IO + SupervisorJob())) { file }
}
