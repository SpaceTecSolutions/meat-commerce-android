package com.spacetecsolutions.meatapp.notification

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.net.Uri
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.spacetecsolutions.meatapp.MainActivity
import com.spacetecsolutions.meatapp.R
import com.spacetecsolutions.meatapp.core.domain.repository.MessagingRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import javax.inject.Inject

@AndroidEntryPoint
class MeatBushMessagingService : FirebaseMessagingService() {
    @Inject lateinit var repository: MessagingRepository
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onNewToken(token: String) {
        serviceScope.launch { repository.registerDeviceToken(token) }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        val title = (message.data["title"] ?: message.notification?.title ?: "MeatBush").take(80)
        val body = (message.data["body"] ?: message.notification?.body ?: return).take(240)
        NotificationChannels.create(this)
        val manager = getSystemService(NotificationManager::class.java)
        val route = message.data["deepLinkRoute"]?.takeIf { it.matches(Regex("[a-z_]+/[a-z0-9_-]+")) }
        val intent = Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        if (route != null) intent.data = Uri.parse("${getString(R.string.deep_link_scheme)}://app/$route")
        message.data["orderId"]?.take(100)?.let { intent.putExtra("notificationOrderId", it) }
        listOf("notificationId", "deepLinkRoute", "event", "category").forEach { key ->
            message.data[key]?.take(120)?.let { intent.putExtra(key, it) }
        }
        val launch = PendingIntent.getActivity(this, message.messageId?.hashCode() ?: body.hashCode(), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val channel = NotificationChannels.forCategory(message.data["category"])
        manager.notify(message.messageId?.hashCode() ?: body.hashCode(), NotificationCompat.Builder(this, channel)
            .setSmallIcon(R.drawable.ic_stat_notification).setContentTitle(title).setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body)).setAutoCancel(true)
            .setContentIntent(launch).build())
    }

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }
}
