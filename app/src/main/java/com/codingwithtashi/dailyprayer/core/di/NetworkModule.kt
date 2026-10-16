package com.codingwithtashi.dailyprayer.core.di

import com.codingwithtashi.dailyprayer.core.network.ConnectivityChecker
import com.codingwithtashi.dailyprayer.core.network.SystemConnectivityChecker
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class NetworkBindings {
    @Binds @Singleton abstract fun bindConnectivity(impl: SystemConnectivityChecker): ConnectivityChecker
}
