package com.codingwithtashi.dailyprayer.reminders

import com.codingwithtashi.dailyprayer.data.tempDataStore
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Test

class DataStoreReminderStateStoreTest {
    private val store = DataStoreReminderStateStore(tempDataStore())

    @Test fun putGetRemoveRoundTrips() = runTest {
        assertThat(store.get(1)).isNull()
        store.put(1, ArmedAlarm(1_700_000_000_000L, exact = false))
        store.put(2, ArmedAlarm(5L, exact = true))
        assertThat(store.get(1)).isEqualTo(ArmedAlarm(1_700_000_000_000L, exact = false))
        assertThat(store.get(2)).isEqualTo(ArmedAlarm(5L, exact = true))
        store.remove(1)
        assertThat(store.get(1)).isNull()
        assertThat(store.get(2)).isNotNull()
    }
}
