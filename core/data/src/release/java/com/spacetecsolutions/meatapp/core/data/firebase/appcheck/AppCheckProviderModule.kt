package com.spacetecsolutions.meatapp.core.data.firebase.appcheck

import com.google.firebase.appcheck.AppCheckProviderFactory
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object AppCheckProviderModule {
    @Provides
    @Singleton
    fun provideAppCheckProviderFactory(): AppCheckProviderFactory =
        PlayIntegrityAppCheckProviderFactory.getInstance()
}
