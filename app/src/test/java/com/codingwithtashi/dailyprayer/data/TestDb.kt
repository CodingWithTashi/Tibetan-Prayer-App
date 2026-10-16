package com.codingwithtashi.dailyprayer.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.room.Room
import com.codingwithtashi.dailyprayer.core.time.AppClock
import com.codingwithtashi.dailyprayer.data.local.PrayerDatabase
import com.codingwithtashi.dailyprayer.reminders.ReminderScheduler
import com.codingwithtashi.dailyprayer.data.model.Routine
import java.io.File
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

fun inMemoryDb(context: Context): PrayerDatabase =
    Room.inMemoryDatabaseBuilder(context, PrayerDatabase::class.java).allowMainThreadQueries().build()

fun tempDataStore(): DataStore<Preferences> {
    val file = File.createTempFile("user_prefs", ".preferences_pb").also { it.delete(); it.deleteOnExit() }
    return PreferenceDataStoreFactory.create(scope = CoroutineScope(Dispatchers.IO + SupervisorJob())) { file }
}

class FakeClock(
    var instant: Instant = Instant.parse("2026-03-04T08:00:00Z"),
    private val zone: ZoneId = ZoneId.of("UTC"),
) : AppClock {
    override fun now(): Instant = instant
    override fun today(): LocalDate = LocalDate.ofInstant(instant, zone)
    override fun zone(): ZoneId = zone
}

class RecordingScheduler : ReminderScheduler {
    val scheduled = mutableListOf<Long>()
    val cancelled = mutableListOf<Long>()
    override suspend fun schedule(routine: Routine) { scheduled += routine.id }
    override fun cancel(routineId: Long) { cancelled += routineId }
    override suspend fun rescheduleAll() = Unit
}
