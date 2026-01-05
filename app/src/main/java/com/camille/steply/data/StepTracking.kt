package com.camille.steply.data


import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Handler
import android.os.Looper
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.work.CoroutineWorker
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.ExistingWorkPolicy
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.suspendCancellableCoroutine
import java.time.LocalDate
import java.time.ZoneId
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.math.sqrt
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import java.time.Instant




// -------------------- TIME UTILS --------------------

fun todayMidnightEpochMillis(
    zoneId: ZoneId = ZoneId.systemDefault()
): Long {
    return LocalDate.now(zoneId)
        .atStartOfDay(zoneId)
        .toInstant()
        .toEpochMilli()
}

fun nextMidnightDelayMillis(
    zoneId: ZoneId = ZoneId.systemDefault()
): Long {
    val now = System.currentTimeMillis()
    val tomorrowMidnight = LocalDate.now(zoneId)
        .plusDays(1)
        .atStartOfDay(zoneId)
        .toInstant()
        .toEpochMilli()

    return (tomorrowMidnight - now).coerceAtLeast(1_000L)
}

// -------------------- DATASTORE --------------------

private val Context.dataStore by preferencesDataStore(name = "steps_store")

class StepDataStore(private val context: Context) {

    private val KEY_DAY_START = longPreferencesKey("day_start_epoch")
    private val KEY_BASE_STEPS = longPreferencesKey("base_steps_from_boot")
    private val KEY_SIM_STEPS_TODAY = intPreferencesKey("sim_steps_today")
    private val KEY_SIM_DAY_START = longPreferencesKey("sim_day_start")

    private val dataStore = context.dataStore


    // -------------------- Versione Mobile --------------------
    suspend fun getDayStartEpoch(): Long {
        return context.dataStore.data.first()[KEY_DAY_START] ?: 0L
    }

    suspend fun getBaseStepsFromBoot(): Long {
        return context.dataStore.data.first()[KEY_BASE_STEPS] ?: 0L
    }

    suspend fun setBaseline(
        dayStartEpoch: Long,
        baseStepsFromBoot: Long
    ) {
        context.dataStore.edit { prefs ->
            prefs[KEY_DAY_START] = dayStartEpoch
            prefs[KEY_BASE_STEPS] = baseStepsFromBoot
        }
    }

    // -------------------- Versione Emulator --------------------
    suspend fun getSimDayStart(): Long {
        return dataStore.data.first()[KEY_SIM_DAY_START] ?: 0L
    }

    suspend fun setSimDayStart(value: Long) {
        dataStore.edit { it[KEY_SIM_DAY_START] = value }
    }

    suspend fun getSimStepsToday(): Int {
        return dataStore.data.first()[KEY_SIM_STEPS_TODAY] ?: 0
    }

    suspend fun setSimStepsToday(value: Int) {
        dataStore.edit { it[KEY_SIM_STEPS_TODAY] = value }
    }

    // ---------- STEPS PER GIORNO (storico) ----------

    private fun dayKeyIso(iso: String) = intPreferencesKey("steps_$iso")

    suspend fun getStepsForDateIso(iso: String): Int {
        return dataStore.data.first()[dayKeyIso(iso)] ?: 0
    }

    suspend fun setStepsForDateIso(iso: String, steps: Int) {
        dataStore.edit { prefs ->
            prefs[dayKeyIso(iso)] = steps
        }
    }

    suspend fun getStepsForDayStartEpoch(dayStartEpoch: Long, zoneId: ZoneId = ZoneId.systemDefault()): Int {
        val iso = Instant.ofEpochMilli(dayStartEpoch).atZone(zoneId).toLocalDate().toString()
        return getStepsForDateIso(iso)
    }

    suspend fun setStepsForDayStartEpoch(dayStartEpoch: Long, steps: Int, zoneId: ZoneId = ZoneId.systemDefault()) {
        val iso = Instant.ofEpochMilli(dayStartEpoch).atZone(zoneId).toLocalDate().toString()
        setStepsForDateIso(iso, steps)
    }


}


// -------------------- STEP SENSOR --------------------

class StepSensor(context: Context) : SensorEventListener {

    private val sensorManager =
        context.getSystemService(Context.SENSOR_SERVICE) as SensorManager

    private val stepCounter: Sensor? =
        sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)

    private var onValue: ((Long) -> Unit)? = null

    fun hasSensor(): Boolean = stepCounter != null

    fun startListening(onStepsFromBoot: (Long) -> Unit) {
        onValue = onStepsFromBoot
        stepCounter?.let {
            sensorManager.registerListener(
                this,
                it,
                SensorManager.SENSOR_DELAY_UI
            )
        }
    }

    fun stopListening() {
        sensorManager.unregisterListener(this)
        onValue = null
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type == Sensor.TYPE_STEP_COUNTER) {
            val value = event.values.firstOrNull()?.toLong() ?: 0L
            onValue?.invoke(value)
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    /**
     * Lettura singola (usata dal Worker a mezzanotte)
     */
    suspend fun readOnce(timeoutMs: Long = 1500L): Long {
        val sensor = stepCounter ?: return 0L

        return suspendCancellableCoroutine { cont ->
            val listener = object : SensorEventListener {
                override fun onSensorChanged(event: SensorEvent) {
                    val v = event.values.firstOrNull()?.toLong() ?: 0L
                    sensorManager.unregisterListener(this)
                    if (cont.isActive) cont.resume(v)
                }
                override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
            }

            sensorManager.registerListener(
                listener,
                sensor,
                SensorManager.SENSOR_DELAY_NORMAL
            )

            cont.invokeOnCancellation {
                sensorManager.unregisterListener(listener)
            }

            Handler(Looper.getMainLooper()).postDelayed({
                try {
                    sensorManager.unregisterListener(listener)
                } catch (_: Exception) {}
                if (cont.isActive) cont.resume(0L)
            }, timeoutMs)
        }
    }
}

// -------------------- MIDNIGHT WORKER --------------------

class MidnightBaselineWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val store = StepDataStore(applicationContext)
        val sensor = StepSensor(applicationContext)

        val fromBoot = sensor.readOnce()
        val midnight = todayMidnightEpochMillis()

        if (fromBoot > 0L) {
            store.setBaseline(
                dayStartEpoch = midnight,
                baseStepsFromBoot = fromBoot
            )
        }

        scheduleNext(applicationContext)
        return Result.success()
    }

    companion object {
        private const val UNIQUE_NAME = "midnight_steps_baseline"

        fun scheduleNext(context: Context) {
            val delay = nextMidnightDelayMillis()

            val request = OneTimeWorkRequestBuilder<MidnightBaselineWorker>()
                .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                UNIQUE_NAME,
                ExistingWorkPolicy.REPLACE,
                request
            )
        }
    }
}

// -------------------- ACCELEROMETRO --------------------

class AccelerometerStepSimulator(
    context: Context,
    private val onStep: () -> Unit
) : SensorEventListener {

    private val sensorManager =
        context.getSystemService(Context.SENSOR_SERVICE) as SensorManager

    private val accelerometer =
        sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

    private var lastStepTime = 0L
    private val threshold = 11.5f          // soglia movimento
    private val minStepInterval = 400L     // ms tra passi

    fun start() {
        sensorManager.registerListener(
            this,
            accelerometer,
            SensorManager.SENSOR_DELAY_GAME
        )
    }

    fun stop() {
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent) {
        val x = event.values[0]
        val y = event.values[1]
        val z = event.values[2]

        val magnitude = sqrt(x * x + y * y + z * z)

        val now = System.currentTimeMillis()
        if (magnitude > threshold && now - lastStepTime > minStepInterval) {
            lastStepTime = now
            onStep()
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
}

