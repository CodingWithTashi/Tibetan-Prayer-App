package com.codingwithtashi.dailyprayer.core.di

import android.content.Context
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultHttpDataSource
import com.codingwithtashi.dailyprayer.playback.AudioCache
import com.codingwithtashi.dailyprayer.playback.Media3AudioCache
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.io.File
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AudioCacheModule {

    /**
     * Lives in noBackupFilesDir: not auto-backed-up and not wiped by the OS like cacheDir.
     * The 256 MB cap is a safety net only; the whole catalogue is ~28 MB, so nothing is evicted in practice.
     */
    @Provides
    @Singleton
    @androidx.annotation.OptIn(markerClass = [UnstableApi::class])
    fun provideMedia3AudioCache(@ApplicationContext context: Context): Media3AudioCache = Media3AudioCache(
        context,
        File(context.noBackupFilesDir, "audio_cache"),
        DefaultHttpDataSource.Factory()
            .setConnectTimeoutMs(20_000)
            .setReadTimeoutMs(60_000)
            .setAllowCrossProtocolRedirects(true),
        256L * 1024 * 1024,
    )

    @Provides
    fun provideAudioCache(impl: Media3AudioCache): AudioCache = impl
}
