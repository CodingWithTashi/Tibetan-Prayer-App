package com.codingwithtashi.dailyprayer.playback

import android.content.Context
import androidx.core.net.toUri
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.ContentMetadata
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import java.io.File

interface AudioCache {
    /** True when every byte of [url] is on disk, so it can play or be copied offline. */
    fun isFullyCached(url: String): Boolean
}

/**
 * Disk cache shared by streaming playback and downloads, so each remote file is fetched at most once.
 * The underlying [SimpleCache] is a process-lifetime singleton and is never released in production
 * (a second instance on the same folder throws).
 */
@androidx.annotation.OptIn(markerClass = [UnstableApi::class])
class Media3AudioCache(
    private val context: Context,
    directory: File,
    private val upstream: DataSource.Factory,
    maxBytes: Long,
) : AudioCache {

    private val cache = SimpleCache(directory, LeastRecentlyUsedCacheEvictor(maxBytes), StandaloneDatabaseProvider(context))

    private val cacheReadWriteFactory = CacheDataSource.Factory()
        .setCache(cache)
        .setUpstreamDataSourceFactory(upstream)
        .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)

    /** For the player. Wrapping in [DefaultDataSource] keeps `file://` downloads out of the cache. */
    fun playerDataSourceFactory(): DataSource.Factory = DefaultDataSource.Factory(context, cacheReadWriteFactory)

    /** Read+write cache data source, used by the downloader. */
    fun readThroughDataSource(): CacheDataSource = cacheReadWriteFactory.createDataSource()

    override fun isFullyCached(url: String): Boolean {
        if (url.isBlank()) return false
        val key = url.toUri().toString()
        val len = ContentMetadata.getContentLength(cache.getContentMetadata(key))
        return len != C.LENGTH_UNSET.toLong() && cache.isCached(key, 0, len)
    }

    internal fun release() = cache.release()
}
