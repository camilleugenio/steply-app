package com.camille.steply.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.camille.steply.MainActivity
import com.camille.steply.R
import com.camille.steply.data.StepDataStore
import com.camille.steply.data.StepSensor
import com.camille.steply.data.todayMidnightEpochMillis
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.time.LocalDate
import kotlin.math.sqrt
import com.google.firebase.auth.FirebaseAuth


class StepForegroundService : Service(), SensorEventListener {

    companion object {
        const val CHANNEL_ID = "steps_foreground"
        const val NOTIF_ID = 1001

        const val GOAL_CHANNEL_ID = "goal_reached"
        const val GOAL_NOTIFY_ID = 2001

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

    private var uid: String? = null

    private var authRetryJob: kotlinx.coroutines.Job? = null


    private var carryStepsToday: Int = 0
    private var bootBaseFromBoot: Long = -1L


    private val serviceJob = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.Main + serviceJob)

    private lateinit var store: StepDataStore
    private lateinit var stepSensor: StepSensor

    // Real step counter baseline
    private var listening = false
    private var dayStart = 0L
    private var baseSteps = 0L

    // Emulator accelerometer fallback
    private var accelEnabled = false
    private lateinit var sensorManager: SensorManager
    private var accelerometer: Sensor? = null

    private var lastStepTime = 0L
    private val threshold = 11.5f
    private val minStepInterval = 400L

    override fun onCreate() {
        super.onCreate()
        store = StepDataStore(applicationContext)
        uid = FirebaseAuth.getInstance().currentUser?.uid

        stepSensor = StepSensor(applicationContext)

        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        createNotificationChannel()
        createGoalNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent == null || intent.action == ACTION_START) {
            ensureForegroundStarted()
        }
        uid = FirebaseAuth.getInstance().currentUser?.uid
        if (intent == null) {
            scope.launch {
                if (store.isTrackingEnabled()) startTracking() else stopSelf()
            }
            return START_STICKY
        }
        when (intent.action) {
            ACTION_START -> startTracking()
            ACTION_STOP -> stopTracking()
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    // -------------------- TRACKING --------------------

    private fun ensureForegroundStarted() {
        // Always safe to call multiple times
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_steply)
            .setColor(0xFFF3A130.toInt())
            .setColorized(true)
            .setContentTitle("Steply")
            .setContentText("Starting step tracking…")
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setOngoing(true)
            .setShowWhen(false)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .build()

        startForeground(NOTIF_ID, notification)
    }

    private fun startTracking() {
        Log.d("STEP_SVC", "startTracking uid=$uid listening=$listening")
        if (uid == null) {
            authRetryJob?.cancel()
            authRetryJob = scope.launch {
                while (uid == null) {
                    kotlinx.coroutines.delay(1000)
                    uid = FirebaseAuth.getInstance().currentUser?.uid
                }
                startTracking()
            }
            return
        }

        if (listening) {
            // Re-arm step source in case emulator dropped the listener
            if (stepSensor.hasSensor()) {
                startRealStepCounter()
            } else {

                startAccelerometerFallback()
            }
            return
        }
        listening = true

        scope.launch { store.setTrackingEnabled(true) }

        scope.launch {
            val u = uid ?: return@launch
            val midnight = todayMidnightEpochMillis()
            val steps = store.getStepsForDayStartEpoch(u, midnight)
            val goal = store.getDailyGoal(default = 50)
            startForeground(NOTIF_ID, buildNotification(steps = steps, goal = goal))
        }

        scope.launch {
            val u = uid ?: return@launch
            val midnight = todayMidnightEpochMillis()

            // Load what the UI also uses
            dayStart = store.getDayStartEpoch(u)
            baseSteps = store.getBaseStepsFromBoot(u)

            // IMPORTANT: force the persisted "dayStart" to today's midnight bucket
            // so UI + service always refer to the same day key (prevents "catch up" behavior)
            if (dayStart != midnight) {
                dayStart = midnight
                store.clearGoalNotifiedIso()
                // You already have setBaseline(...) from the real sensor path.
                // In emulator mode baseSteps isn't meaningful, but we use it to persist dayStart.
                store.setBaseline(u, dayStart, 0L)
            }

            if (stepSensor.hasSensor()) {
                startRealStepCounter()
            } else {
                // Emulator / no step counter -> accelerometer shake fallback
                val midnight = todayMidnightEpochMillis()
                val steps = store.getStepsForDayStartEpoch(u, midnight)
                store.setSimDayStart(u, midnight)
                store.setSimStepsToday(u, steps)
                startAccelerometerFallback()
            }
        }

    }

    private fun stopTracking() {
        authRetryJob?.cancel()
        authRetryJob = null
        listening = false

        // stop both possible sources
        stepSensor.stopListening()
        stopAccelerometerFallback()

        scope.launch { store.setTrackingEnabled(false) }

        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        authRetryJob?.cancel()
        authRetryJob = null
        stepSensor.stopListening()
        stopAccelerometerFallback()
        scope.cancel()
        super.onDestroy()
    }

    // -------------------- REAL STEP COUNTER --------------------

    private fun startRealStepCounter() {
        stopAccelerometerFallback()

        stepSensor.startListening { currentFromBoot ->
            scope.launch {
                val midnight = todayMidnightEpochMillis()
                val u = uid ?: return@launch


                // First callback after service/process start
                if (dayStart == 0L) {
                    dayStart = midnight
                    carryStepsToday = store.getStepsForDayStartEpoch(u, dayStart)
                    bootBaseFromBoot = currentFromBoot
                }


                // New day: reset carry and set new boot base
                if (dayStart != midnight) {
                    dayStart = midnight
                    carryStepsToday = 0
                    bootBaseFromBoot = currentFromBoot
                    store.clearGoalNotifiedIso()
                }

                if (currentFromBoot < bootBaseFromBoot) {
                    // reboot/sensor reset
                    bootBaseFromBoot = currentFromBoot
                    carryStepsToday = store.getStepsForDayStartEpoch(u, dayStart)
                }


                // Delta since we (re)started listening
                val delta = (currentFromBoot - bootBaseFromBoot).coerceAtLeast(0L).toInt()
                val computed = carryStepsToday + delta

                // Never go backwards vs persisted value
                val persisted = store.getStepsForDayStartEpoch(u, dayStart)
                val todaySteps = maxOf(persisted, computed)

                // Persist + update notif + maybe goal
                store.setStepsForDayStartEpoch(u, dayStart, todaySteps)
                maybeNotifyGoal(todaySteps)

                val goal = store.getDailyGoal(default = 50)
                startForeground(NOTIF_ID, buildNotification(todaySteps, goal))
            }
        }
    }



    // -------------------- ACCELEROMETER FALLBACK (EMULATOR) --------------------

    private fun startAccelerometerFallback() {
        if (accelEnabled) return
        accelEnabled = true

        // if no accel sensor, we still keep the foreground notif alive
        accelerometer?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
        }
    }

    private fun stopAccelerometerFallback() {
        if (!accelEnabled) return
        accelEnabled = false
        try {
            sensorManager.unregisterListener(this)
        } catch (_: Exception) {}
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (!listening){
            Log.d("STEP_SVC", "onSensorChanged but NOT listening")
            return
        }
        if (event.sensor.type != Sensor.TYPE_ACCELEROMETER) return

        val x = event.values[0]
        val y = event.values[1]
        val z = event.values[2]
        val magnitude = sqrt(x * x + y * y + z * z)

        val now = System.currentTimeMillis()
        if (magnitude > threshold && now - lastStepTime > minStepInterval) {
            lastStepTime = now

            scope.launch {
                val midnight = todayMidnightEpochMillis()

                // new day for emulator path too
                if (dayStart != midnight) {
                    dayStart = midnight
                    store.clearGoalNotifiedIso()

                    val u = uid ?: return@launch
                    // Persist the new dayStart/baseline so UI reads the same day bucket
                    store.setBaseline(u, dayStart, /* baseSteps */ 0L)
                    // (or store.setDayStartEpoch(u, dayStart) if you have that instead)
                }

                // increment today steps based on stored value

                val u = uid ?: return@launch
                val current = store.getStepsForDayStartEpoch(u, midnight)
                val next = current + 1
                store.setStepsForDayStartEpoch(u, midnight, next)
                store.setSimDayStart(u, midnight)
                store.setSimStepsToday(u, next)
                maybeNotifyGoal(next)

                val goal = store.getDailyGoal(default = 50)
                startForeground(NOTIF_ID, buildNotification(next, goal))
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit

    // -------------------- GOAL NOTIFICATION --------------------

    private suspend fun maybeNotifyGoal(todaySteps: Int) {
        if (!store.isGoalNotificationEnabled()) return

        val goal = store.getDailyGoal(default = 50)
        if (goal <= 0) return

        val todayIso = LocalDate.now().toString()
        val alreadyNotifiedIso = store.getGoalNotifiedIso()

        if (todaySteps >= goal && alreadyNotifiedIso != todayIso) {
            store.setGoalNotifiedIso(todayIso)

            val nm = getSystemService(NotificationManager::class.java)
            nm.notify(GOAL_NOTIFY_ID, buildGoalReachedNotification(todaySteps, goal))
        }
    }


    // -------------------- NOTIFICATIONS --------------------

    private fun buildNotification(steps: Int, goal: Int): Notification {
        val openAppIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_steply)
            .setColor(0xFFF3A130.toInt())
            .setColorized(true)
            .setContentText("Steps: $steps / $goal")
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setOngoing(true)
            .setShowWhen(false)
            .setAutoCancel(false)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setContentIntent(openAppIntent)
            .build()
    }

    private fun buildGoalReachedNotification(steps: Int, goal: Int): Notification {
        val openAppIntent = PendingIntent.getActivity(
            this,
            1,
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, GOAL_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_steply)
            .setColor(0xFFFF9F1C.toInt())
            .setColorized(true)
            .setContentTitle("Goal reached! 🎉")
            .setContentText("You hit $goal steps.")
            .setShowWhen(false)
            .setAutoCancel(true)
            .setOnlyAlertOnce(true)
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

    private fun createGoalNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                GOAL_CHANNEL_ID,
                "Goal reached",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notifies when you reach your daily step goal"
                setShowBadge(true)
            }
            val nm = getSystemService(NotificationManager::class.java)
            nm.createNotificationChannel(channel)
        }
    }
}
