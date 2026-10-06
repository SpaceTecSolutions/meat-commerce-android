package com.spacetecsolutions.meatapp

import android.content.Context
import com.spacetecsolutions.meatapp.core.model.PasswordPolicy
import com.spacetecsolutions.meatapp.core.model.PhoneNumberConfig
import com.spacetecsolutions.meatapp.core.data.firebase.di.FirebaseRegionConfig
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AuthConfigModule {
    @Provides
    @Singleton
    fun providePhoneNumberConfig(@ApplicationContext context: Context): PhoneNumberConfig =
        PhoneNumberConfig(
            countryCode = context.getString(R.string.auth_country_code),
            nationalNumberLength = context.resources.getInteger(R.integer.auth_national_number_length),
        )

    @Provides
    @Singleton
    fun providePasswordPolicy(): PasswordPolicy = PasswordPolicy()

    @Provides
    @Singleton
    fun provideFirebaseRegionConfig(@ApplicationContext context: Context): FirebaseRegionConfig =
        FirebaseRegionConfig(
            functionsRegion = context.getString(R.string.firebase_functions_region),
            firestoreDatabaseId = context.getString(R.string.firestore_database_id),
            realtimeDatabaseUrl = context.getString(R.string.firebase_realtime_database_url),
        )
}
