package com.codingwithtashi.dailyprayer.core.text

import android.util.LruCache
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Singleton
class PrayerTextCache @Inject constructor() {
    private data class Key(val id: Int, val contentHash: Int)

    private val cache = LruCache<Key, PrayerText>(MAX_ENTRIES)

    /** Splits on the Default dispatcher; cached per prayer id + content hash. */
    suspend fun get(prayerId: Int, content: String): PrayerText {
        val key = Key(prayerId, content.hashCode())
        synchronized(cache) { cache.get(key) }?.let { return it }
        val text = withContext(Dispatchers.Default) { VerseSplitter.split(content) }
        synchronized(cache) { cache.put(key, text) }
        return text
    }

    private companion object {
        const val MAX_ENTRIES = 32
    }
}
