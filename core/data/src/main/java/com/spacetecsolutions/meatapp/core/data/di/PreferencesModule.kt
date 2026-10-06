package com.spacetecsolutions.meatapp.core.data.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.spacetecsolutions.meatapp.core.data.preferences.AppPreferences
import com.spacetecsolutions.meatapp.core.data.preferences.DataStoreAppPreferences
import com.spacetecsolutions.meatapp.core.data.preferences.DataStoreFeatureConfigCache
import com.spacetecsolutions.meatapp.core.data.preferences.FeatureConfigCache
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class PreferencesBindings {
    @Binds
    @Singleton
    internal abstract fun bindFeatureConfigCache(
        implementation: DataStoreFeatureConfigCache,
    ): FeatureConfigCache

    @Binds
    @Singleton
    internal abstract fun bindAppPreferences(implementation: DataStoreAppPreferences): AppPreferences
}

@Module
@InstallIn(SingletonComponent::class)
object PreferencesModule {
    @Provides
    @Singleton
    fun providePreferencesDataStore(
        @ApplicationContext context: Context,
    ): DataStore<Preferences> = PreferenceDataStoreFactory.create(
        produceFile = { context.preferencesDataStoreFile("app_preferences") },
    )
}
