package com.spacetecsolutions.meatapp.core.data.firebase.auth

import android.app.Activity
import java.lang.ref.WeakReference
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PhoneAuthActivityProvider @Inject constructor() {
    private var activityReference = WeakReference<Activity>(null)

    fun attach(activity: Activity) {
        activityReference = WeakReference(activity)
    }

    fun detach(activity: Activity) {
        if (activityReference.get() === activity) activityReference.clear()
    }

    internal fun currentActivity(): Activity? = activityReference.get()
}
