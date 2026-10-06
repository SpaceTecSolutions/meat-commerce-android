package com.spacetecsolutions.meatapp.core.data.firebase.di

import android.content.Context
import android.content.ContentResolver
import com.google.firebase.FirebaseApp
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.storage.FirebaseStorage
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object FirebaseModule {
    @Provides
    @Singleton
    fun provideContentResolver(@ApplicationContext context: Context): ContentResolver =
        context.contentResolver

    @Provides
    @Singleton
    fun provideFirebaseApp(): FirebaseApp = FirebaseApp.getInstance()

    @Provides
    @Singleton
    fun provideFirebaseAuth(app: FirebaseApp): FirebaseAuth = FirebaseAuth.getInstance(app)

    @Provides
    @Singleton
    fun provideFirestore(app: FirebaseApp, region: FirebaseRegionConfig): FirebaseFirestore =
        FirebaseFirestore.getInstance(app, region.firestoreDatabaseId)

    @Provides
    @Singleton
    fun provideStorage(app: FirebaseApp): FirebaseStorage = FirebaseStorage.getInstance(app)

    @Provides
    @Singleton
    fun provideMessaging(): FirebaseMessaging = FirebaseMessaging.getInstance()

    @Provides
    @Singleton
    fun provideFunctions(app: FirebaseApp, region: FirebaseRegionConfig): FirebaseFunctions =
        FirebaseFunctions.getInstance(app, region.functionsRegion)

    @Provides
    @Singleton
    fun provideAnalytics(@ApplicationContext context: Context): FirebaseAnalytics =
        FirebaseAnalytics.getInstance(context)

    @Provides
    @Singleton
    fun provideCrashlytics(): FirebaseCrashlytics = FirebaseCrashlytics.getInstance()

    @Provides
    @Singleton
    fun provideAppCheck(app: FirebaseApp): FirebaseAppCheck = FirebaseAppCheck.getInstance(app)
}
