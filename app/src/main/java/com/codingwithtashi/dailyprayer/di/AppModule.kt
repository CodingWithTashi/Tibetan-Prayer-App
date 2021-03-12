package com.codingwithtashi.dailyprayer.di

import android.content.Context
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import com.codingwithtashi.dailyprayer.R
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
class AppModule {
    @Singleton
    @Provides
    fun provideGlideInstance(
            @ApplicationContext context: Context
    ) = Glide.with(context).setDefaultRequestOptions(
            RequestOptions()
                    .placeholder(R.drawable.bhudha)
                    .error(R.drawable.bhudha)
                    .diskCacheStrategy(DiskCacheStrategy.DATA)
    )
}