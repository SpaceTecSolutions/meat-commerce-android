package com.spacetecsolutions.meatapp.core.data.firebase.realtime

import com.google.firebase.FirebaseApp
import com.google.firebase.database.FirebaseDatabase
import com.spacetecsolutions.meatapp.core.data.firebase.di.FirebaseRegionConfig
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Provider
import javax.inject.Qualifier
import javax.inject.Singleton

@Qualifier
@Retention(AnnotationRetention.BINARY)
internal annotation class LiveTrackingDatabase

class RealtimeTrackingDatabaseAccessor @javax.inject.Inject constructor(
    @param:LiveTrackingDatabase private val databaseProvider: Provider<FirebaseDatabase>,
) {
    fun getWhenTrackingIsEnabled(): FirebaseDatabase = databaseProvider.get()
}

@Module
@InstallIn(SingletonComponent::class)
internal object RealtimeDatabaseModule {
    @Provides
    @Singleton
    @LiveTrackingDatabase
    fun provideLiveTrackingDatabase(
        app: FirebaseApp,
        region: FirebaseRegionConfig,
    ): FirebaseDatabase = FirebaseDatabase.getInstance(app, region.realtimeDatabaseUrl)
}
