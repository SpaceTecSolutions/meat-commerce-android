package com.spacetecsolutions.meatapp.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build

object NotificationChannels {
    const val ORDERS = "orders"
    const val PAYMENTS = "payments"
    const val DELIVERY = "delivery"
    const val PROMOTIONS = "promotions"
    const val SYSTEM = "system_reports"

    fun create(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        context.getSystemService(NotificationManager::class.java).createNotificationChannels(
            listOf(
                NotificationChannel(ORDERS, "Orders", NotificationManager.IMPORTANCE_HIGH),
                NotificationChannel(PAYMENTS, "Payments", NotificationManager.IMPORTANCE_DEFAULT),
                NotificationChannel(DELIVERY, "Delivery", NotificationManager.IMPORTANCE_HIGH),
                NotificationChannel(PROMOTIONS, "Promotions", NotificationManager.IMPORTANCE_DEFAULT),
                NotificationChannel(SYSTEM, "System and reports", NotificationManager.IMPORTANCE_DEFAULT),
            ),
        )
    }

    fun forCategory(category: String?) = when (category) {
        "ORDER" -> ORDERS
        "PAYMENT" -> PAYMENTS
        "DELIVERY" -> DELIVERY
        "PROMOTION" -> PROMOTIONS
        else -> SYSTEM
    }
}
