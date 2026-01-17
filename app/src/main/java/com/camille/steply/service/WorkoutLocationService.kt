package com.camille.steply.service

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.camille.steply.R
import com.camille.steply.data.location.LatLng
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.flow.MutableSharedFlow

class WorkoutLocationService : Service() {

    companion object {
        const val ACTION_START = "com.camille.steply.service.WorkoutLocationService.START"
        const val ACTION_STOP = "com.camille.steply.service.WorkoutLocationService.STOP"
        val locations = MutableSharedFlow<LatLng>(extraBufferCapacity = 128)

        private const val CHANNEL_ID = "workout_location"
        private const val NOTIF_ID = 3101
    }

    private val fused by lazy { LocationServices.getFusedLocationProviderClient(this) }
    private var started = false

    private val callback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            val loc = result.lastLocation ?: return
            locations.tryEmit(LatLng(loc.latitude, loc.longitude))
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> startTrackingIfNeeded()
            ACTION_STOP -> stopTracking()
            else -> startTrackingIfNeeded()
        }
        return START_STICKY
    }

    private fun startTrackingIfNeeded() {
        if (started) return

        val fineOk = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED
        val coarseOk = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED

        if (!fineOk && !coarseOk) {
            stopSelf()
            return
        }

        started = true
        createChannelIfNeeded()
        startForeground(NOTIF_ID, buildNotification())

        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 500L)
            .setMinUpdateIntervalMillis(250L)
            .setMinUpdateDistanceMeters(0f)
            .build()

        fused.requestLocationUpdates(request, callback, mainLooper)
    }

    private fun stopTracking() {
        if (started) {
            started = false
            fused.removeLocationUpdates(callback)
        }
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun buildNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground) // puoi cambiarla
            .setContentTitle("Workout in corso")
            .setContentText("Tracciamento GPS attivo")
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun createChannelIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = getSystemService(NotificationManager::class.java)
            nm.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_ID,
                    "Workout location",
                    NotificationManager.IMPORTANCE_LOW
                )
            )
        }
    }

    override fun onDestroy() {
        kotlin.runCatching { fused.removeLocationUpdates(callback) }
        super.onDestroy()
    }
}
