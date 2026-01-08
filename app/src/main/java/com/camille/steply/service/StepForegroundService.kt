package com.camille.steply.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.Context
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.camille.steply.MainActivity
import com.camille.steply.R
import com.camille.steply.data.StepDataStore
import com.camille.steply.data.StepSensor
import com.camille.steply.data.todayMidnightEpochMillis
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class StepForegroundService : Service() {

    companion object {
        const val CHANNEL_ID = "steps_foreground"
        const val NOTIF_ID = 1001

        const val ACTION_START = "ACTION_START"
        const val ACTION_STOP = "ACTION_STOP"

        fun start(context: Context) {
            val intent = Intent(context, StepForegroundService::class.java).apply {
                action = ACTION_START
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, StepForegroundService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
    }

    // ---- Coroutine scope ----
    private val serviceJob = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.Main + serviceJob)

    // ---- Data ----
    private lateinit var store: StepDataStore
    private lateinit var sensor: StepSensor

    private var listening = false
    private var dayStart = 0L
    private var baseSteps = 0L

    // Emulator / no-sensor safeguard
    private var heartbeatJob: Job? = null

    override fun onCreate() {
        super.onCreate()
        store = StepDataStore(applicationContext)
        sensor = StepSensor(applicationContext)
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> startTracking()
            ACTION_STOP -> stopTracking()
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    // -------------------- TRACKING --------------------

    private fun startTracking() {
        if (listening) return
        listening = true

        // Persist toggle for reboot restart + UI
        scope.launch { store.setTrackingEnabled(true) }

        // Foreground notification must appear immediately
        startForeground(NOTIF_ID, buildNotification(steps = 0))

        scope.launch {
            dayStart = store.getDayStartEpoch()
            baseSteps = store.getBaseStepsFromBoot()

            sensor.startListening { currentFromBoot ->
                scope.launch {
                    val midnight = todayMidnightEpochMillis()

                    // New day → reset baseline
                    if (dayStart != midnight) {
                        dayStart = midnight
                        baseSteps = currentFromBoot
                        store.setBaseline(dayStart, baseSteps)
                    }

                    // Reboot-safe
                    if (currentFromBoot < baseSteps) {
                        baseSteps = currentFromBoot
                        store.setBaseline(dayStart, baseSteps)
                    }

                    val todaySteps =
                        (currentFromBoot - baseSteps).coerceAtLeast(0L).toInt()

                    // Single source of truth
                    store.setStepsForDayStartEpoch(dayStart, todaySteps)

                    // Re-assert foreground notification
                    startForeground(
                        NOTIF_ID,
                        buildNotification(todaySteps)
                    )
                }
            }
        }

        // ✅ Heartbeat (emulator / no sensor case)
        startHeartbeat()
    }

    private fun stopTracking() {
        listening = false

        heartbeatJob?.cancel()
        heartbeatJob = null

        sensor.stopListening()

        scope.launch { store.setTrackingEnabled(false) }

        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        heartbeatJob?.cancel()
        scope.cancel()
        sensor.stopListening()
        super.onDestroy()
    }

    // -------------------- HEARTBEAT --------------------

    private fun startHeartbeat() {
        heartbeatJob?.cancel()

        heartbeatJob = scope.launch {
            while (listening) {
                val midnight = todayMidnightEpochMillis()
                val steps = store.getStepsForDayStartEpoch(midnight)

                // Re-attach foreground notification
                startForeground(
                    NOTIF_ID,
                    buildNotification(steps)
                )

                delay(1000L) //
            }
        }
    }

    // -------------------- NOTIFICATION --------------------

    private fun buildNotification(steps: Int): Notification {
        val openAppIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground) // replace later
            .setContentTitle("Steply")
            .setContentText("Steps: $steps")
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setOngoing(true)
            .setShowWhen(false)
            .setAutoCancel(false)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setContentIntent(openAppIntent)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Step counter",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows your daily steps"
                setShowBadge(false)
            }

            val nm = getSystemService(NotificationManager::class.java)
            nm.createNotificationChannel(channel)
        }
    }
}
