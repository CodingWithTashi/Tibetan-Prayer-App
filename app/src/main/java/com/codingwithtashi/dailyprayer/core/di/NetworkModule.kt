package com.codingwithtashi.dailyprayer.core.di

import com.codingwithtashi.dailyprayer.core.network.ConnectivityChecker
import com.codingwithtashi.dailyprayer.core.network.SystemConnectivityChecker
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import java.util.concurrent.TimeUnit
import javax.inject.Singleton
import okhttp3.OkHttpClient

@Module
@InstallIn(SingletonComponent::class)
abstract class NetworkBindings {
    @Binds @Singleton abstract fun bindConnectivity(impl: SystemConnectivityChecker): ConnectivityChecker
}

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()
}
