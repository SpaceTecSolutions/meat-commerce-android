package com.spacetecsolutions.meatapp

import android.app.Application
import com.spacetecsolutions.meatapp.core.domain.service.AppCheckManager
import com.spacetecsolutions.meatapp.notification.NotificationChannels
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class MeatBushApplication : Application() {
    @Inject
    lateinit var appCheckManager: AppCheckManager

    override fun onCreate() {
        super.onCreate()
        NotificationChannels.create(this)
        appCheckManager.initialize()
    }
}
