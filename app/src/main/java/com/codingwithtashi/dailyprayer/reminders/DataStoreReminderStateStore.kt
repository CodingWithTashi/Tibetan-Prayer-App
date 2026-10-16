package com.codingwithtashi.dailyprayer.reminders

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.flow.first

/** Process-wide instance, see the note in `DataStoreModule`: one DataStore per file for the whole process. */
private val Context.reminderStateDataStore: DataStore<Preferences> by preferencesDataStore(name = "reminder_state")

/** [ReminderStateStore] persisted in its own DataStore file, separate from user preferences. */
class DataStoreReminderStateStore(private val store: DataStore<Preferences>) : ReminderStateStore {
    @Inject constructor(@ApplicationContext context: Context) : this(context.reminderStateDataStore)

    override suspend fun get(routineId: Long): ArmedAlarm? =
        store.data.first()[key(routineId)]?.let(::decode)

    override suspend fun getAll(): Map<Long, ArmedAlarm> = buildMap {
        for ((key, value) in store.data.first().asMap()) {
            val id = key.name.removePrefix(KEY_PREFIX).toLongOrNull() ?: continue
            val alarm = (value as? String)?.let(::decode) ?: continue
            put(id, alarm)
        }
    }

    override suspend fun put(routineId: Long, alarm: ArmedAlarm) {
        store.edit { it[key(routineId)] = "${alarm.triggerAtMillis}:${alarm.exact}" }
    }

    override suspend fun remove(routineId: Long) {
        store.edit { it.remove(key(routineId)) }
    }

    private fun key(routineId: Long) = stringPreferencesKey("$KEY_PREFIX$routineId")

    private companion object {
        const val KEY_PREFIX = "armed_"
    }

    private fun decode(raw: String): ArmedAlarm? {
        val parts = raw.split(':')
        if (parts.size != 2) return null
        val millis = parts[0].toLongOrNull() ?: return null
        return ArmedAlarm(millis, parts[1].toBooleanStrictOrNull() ?: return null)
    }
}
