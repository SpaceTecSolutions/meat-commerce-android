package com.spacetecsolutions.meatapp.core.data.firebase.appcheck

import com.google.firebase.appcheck.AppCheckProviderFactory
import com.google.firebase.appcheck.FirebaseAppCheck
import com.spacetecsolutions.meatapp.core.common.result.AppError
import com.spacetecsolutions.meatapp.core.common.result.AppResult
import com.spacetecsolutions.meatapp.core.domain.service.AppCheckManager
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class FirebaseAppCheckManager @Inject constructor(
    private val firebaseAppCheck: FirebaseAppCheck,
    private val providerFactory: AppCheckProviderFactory,
) : AppCheckManager {
    override fun initialize() {
        firebaseAppCheck.installAppCheckProviderFactory(providerFactory)
    }

    override suspend fun getToken(forceRefresh: Boolean): AppResult<String> = try {
        AppResult.Success(firebaseAppCheck.getAppCheckToken(forceRefresh).await().token)
    } catch (error: Exception) {
        AppResult.Failure(AppError.Unknown(error))
    }
}
