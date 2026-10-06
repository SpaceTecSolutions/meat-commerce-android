package com.spacetecsolutions.meatapp.tracking

import android.Manifest
import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.IBinder
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.*
import com.spacetecsolutions.meatapp.core.common.result.AppResult
import com.spacetecsolutions.meatapp.core.domain.repository.LiveTrackingRepository
import com.spacetecsolutions.meatapp.core.model.CodActor
import com.spacetecsolutions.meatapp.core.model.DeliveryLocation
import com.spacetecsolutions.meatapp.R
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import javax.inject.Inject

@AndroidEntryPoint
class DeliveryTrackingService : Service() {
    @Inject lateinit var trackingRepository: LiveTrackingRepository
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var locationClient: FusedLocationProviderClient
    private val throttle = LocationUpdateThrottle()
    // Keep at most the newest unsent point while RTDB is reconnecting.
    private val pendingLocations = Channel<DeliveryLocation>(Channel.CONFLATED)
    private var active: TrackingIdentity? = null

    override fun onCreate() {
        super.onCreate()
        locationClient = LocationServices.getFusedLocationProviderClient(this)
        createChannel()
        scope.launch {
            for (location in pendingLocations) {
                val identity = active ?: continue
                trackingRepository.publish(identity.sessionId, location)
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopTracking(cleanRemote = true)
            return START_NOT_STICKY
        }
        val identity = intent?.identity() ?: restoreIdentity()
        if (identity == null || !hasLocationPermission()) {
            stopTracking(cleanRemote = false)
            return START_NOT_STICKY
        }
        startForeground(NOTIFICATION_ID, notification())
        scope.launch {
            // Re-authorizes all feature/config/order/assignment gates on every restart.
            if (trackingRepository.activate(identity.actor, identity.orderId, identity.sessionId)
                is AppResult.Success) {
                active = identity
                persist(identity)
                requestLocations()
            } else stopTracking(cleanRemote = false)
        }
        return START_STICKY
    }

    @Suppress("MissingPermission")
    private fun requestLocations() {
        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 10_000)
            .setMinUpdateIntervalMillis(5_000).setMinUpdateDistanceMeters(10f).build()
        locationClient.requestLocationUpdates(request, callback, mainLooper)
    }

    private val callback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            if (active == null) return
            result.lastLocation?.takeIf(throttle::shouldPublish)?.let { location ->
                pendingLocations.trySend(location.toDomain())
            }
        }
    }

    private fun stopTracking(cleanRemote: Boolean) {
        if (::locationClient.isInitialized) locationClient.removeLocationUpdates(callback)
        pendingLocations.close()
        val identity = active ?: restoreIdentity()
        active = null
        clearPersisted()
        if (cleanRemote && identity != null) scope.launch {
            trackingRepository.stop(identity.orderId, identity.sessionId)
        }
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        if (::locationClient.isInitialized) locationClient.removeLocationUpdates(callback)
        scope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun hasLocationPermission() = ActivityCompat.checkSelfPermission(
        this, Manifest.permission.ACCESS_FINE_LOCATION,
    ) == PackageManager.PERMISSION_GRANTED || ActivityCompat.checkSelfPermission(
        this, Manifest.permission.ACCESS_COARSE_LOCATION,
    ) == PackageManager.PERMISSION_GRANTED

    private fun createChannel() {
        getSystemService(NotificationManager::class.java).createNotificationChannel(
            NotificationChannel(CHANNEL_ID, "Live delivery tracking", NotificationManager.IMPORTANCE_LOW),
        )
    }

    private fun notification() = NotificationCompat.Builder(this, CHANNEL_ID)
        .setSmallIcon(R.mipmap.ic_launcher)
        .setContentTitle("Live delivery tracking")
        .setContentText("Location is shared only for the active delivery")
        .setOngoing(true).setCategory(Notification.CATEGORY_SERVICE).build()

    private fun persist(value: TrackingIdentity) = getSharedPreferences(PREFS, MODE_PRIVATE).edit()
        .putString("order", value.orderId).putString("session", value.sessionId)
        .putString("actor", value.actor.name).apply()
    private fun restoreIdentity(): TrackingIdentity? = getSharedPreferences(PREFS, MODE_PRIVATE).let {
        val order = it.getString("order", null) ?: return null
        val session = it.getString("session", null) ?: return null
        val actor = runCatching { CodActor.valueOf(it.getString("actor", "")!!) }.getOrNull() ?: return null
        TrackingIdentity(order, session, actor)
    }
    private fun clearPersisted() = getSharedPreferences(PREFS, MODE_PRIVATE).edit().clear().apply()

    companion object {
        private const val ACTION_START = "com.spacetecsolutions.meatapp.tracking.START"
        private const val ACTION_STOP = "com.spacetecsolutions.meatapp.tracking.STOP"
        private const val CHANNEL_ID = "delivery_tracking"
        private const val NOTIFICATION_ID = 3101
        private const val PREFS = "active_delivery_tracking"

        fun start(context: Context, orderId: String, sessionId: String, actor: CodActor) {
            val intent = Intent(context, DeliveryTrackingService::class.java).setAction(ACTION_START)
                .putExtra("order", orderId).putExtra("session", sessionId).putExtra("actor", actor.name)
            ContextCompat.startForegroundService(context, intent)
        }
        fun stop(context: Context) {
            context.startService(Intent(context, DeliveryTrackingService::class.java).setAction(ACTION_STOP))
        }
    }
}

private data class TrackingIdentity(val orderId: String, val sessionId: String, val actor: CodActor)
private fun Intent.identity(): TrackingIdentity? {
    val order = getStringExtra("order") ?: return null
    val session = getStringExtra("session") ?: return null
    val actor = runCatching { CodActor.valueOf(getStringExtra("actor").orEmpty()) }.getOrNull() ?: return null
    return TrackingIdentity(order, session, actor)
}
private fun Location.toDomain() = DeliveryLocation(
    latitude, longitude, accuracy, speed.takeIf { hasSpeed() },
    bearing.takeIf { hasBearing() }, time,
)
