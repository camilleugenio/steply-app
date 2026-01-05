package com.camille.steply.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.camille.steply.data.location.LocationRepository
import com.camille.steply.data.StepDataStore
import com.camille.steply.data.StepSensor
import com.camille.steply.data.AccelerometerStepSimulator
import com.camille.steply.data.todayMidnightEpochMillis
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

data class HomeUiState(
    val steps: Int = 0,
    val km: String = "0.00",
    val kcal: String = "0",
    val isTracking: Boolean = false,
    val currentDate: String = "",
    val currentDayname: String = "",
    val currentDateIso: String = "",
    val weeklySteps: List<Int> = List(7) { 0 },
    val currentPlacename: String = "-  ",
    val locationLoading: Boolean = false,
    val locationError: String? = null
)

class HomeViewModel(
    private val appContext: Context,
    private val locationRepository: LocationRepository
) : ViewModel() {

    private val dateFormatter = DateTimeFormatter.ofPattern("d MMMM")
    private val dayFormatter = DateTimeFormatter.ofPattern("EEEE")

    private val store = StepDataStore(appContext)
    private val sensor = StepSensor(appContext)
    private var listening = false

    private val _uiState = MutableStateFlow(
        HomeUiState(
            currentDate = LocalDate.now().format(dateFormatter),
            currentDayname = LocalDate.now().format(dayFormatter),
            currentDateIso = LocalDate.now().toString()
        )
    )
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        refreshWeeklySteps(_uiState.value.currentDateIso)
    }


    // -------------------- LOCATION --------------------
    fun refreshPlace() {
        viewModelScope.launch {
            _uiState.update { it.copy(locationLoading = true, locationError = null) }

            try {
                val latLng = locationRepository.getCurrentLatLng()
                val place = locationRepository.getPlaceName(latLng.lat, latLng.lon)

                _uiState.update {
                    it.copy(
                        currentPlacename = place,
                        locationLoading = false
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        currentPlacename = "-",
                        locationLoading = false,
                        locationError = e.message ?: "Location failed"
                    )
                }
            }
        }
    }

    // -------------------- STEPS (REAL) --------------------
    fun startStepUpdates() {
        if (listening) return
        listening = true
        _uiState.update { it.copy(isTracking = true) }

        viewModelScope.launch {
            var dayStart = store.getDayStartEpoch()
            var baseSteps = store.getBaseStepsFromBoot()
            val midnight = todayMidnightEpochMillis()

            sensor.startListening { currentFromBoot ->
                viewModelScope.launch {

                    // nuovo giorno -> reset baseline
                    if (dayStart != midnight) {
                        dayStart = midnight
                        baseSteps = currentFromBoot
                        store.setBaseline(dayStart, baseSteps)

                        val now = LocalDate.now()
                        _uiState.update {
                            it.copy(
                                currentDate = now.format(dateFormatter),
                                currentDayname = now.format(dayFormatter),
                                currentDateIso = now.toString()
                            )
                        }
                        refreshWeeklySteps(now.toString())
                    }

                    // reboot-safe
                    if (currentFromBoot < baseSteps) {
                        baseSteps = currentFromBoot
                        store.setBaseline(dayStart, baseSteps)
                    }

                    val todaySteps = (currentFromBoot - baseSteps).coerceAtLeast(0L).toInt()
                    val kmText = stepsToKm(todaySteps)      // String "1.23"
                    val kmValue = kmText.toDouble()         // Double 1.23 (ok perché Locale.US)
                    val kcalValue = (70.0 * kmValue * 0.75).roundToInt()

                    store.setStepsForDayStartEpoch(dayStart, todaySteps)

                    _uiState.update {
                        it.copy(
                            steps = todaySteps,
                            km = kmText,
                            kcal = kcalValue.toString()
                        )
                    }

                    refreshWeeklySteps(_uiState.value.currentDateIso)

                }
            }
        }
    }

    fun stopStepUpdates() {
        if (!listening) return
        listening = false
        sensor.stopListening()
        _uiState.update { it.copy(isTracking = false) }
    }

    // -------------------- Steps -> Km --------------------

    private fun stepsToKm(steps: Int, stepLengthMeters: Double = 0.74): String {
        val km = (steps * stepLengthMeters) / 1000.0
        return String.format(Locale.US, "%.2f", km)
    }

    // -------------------- Kcal --------------------

    private fun kmToKcal(km: Double, weightKg: Double = 65.0): Int {
        // ~0.75 kcal per kg per km (walking)
        return (weightKg * km * 0.75).roundToInt()
    }

    private fun formatKm(km: Double): String {
        return String.format(Locale.getDefault(), "%.2f", km)
    }


    // -------------------- DEBUG (eliminare) --------------------
    fun addTestStep(amount: Int = 200) {
        _uiState.update { it.copy(steps = it.steps + amount) }
    }

    fun resetSteps() {
        _uiState.update { it.copy(steps = 0, km = "0.00") }
    }

    override fun onCleared() {
        super.onCleared()
        sensor.stopListening()
    }

    // -------------------- ACCELEROMETRO --------------------
    private var accelSimulator: AccelerometerStepSimulator? = null

    fun startAccelerometerSimulation() {
        if (accelSimulator != null) return

        // prima carico i passi salvati di oggi
        loadSimulatedStepsIfNeeded()

        accelSimulator = AccelerometerStepSimulator(appContext) {
            viewModelScope.launch {
                val current = store.getSimStepsToday() + 1
                store.setSimStepsToday(current)

                val todayMidnight = todayMidnightEpochMillis()
                store.setStepsForDayStartEpoch(todayMidnight, current)

                val kmText = stepsToKm(current)          // "1.23"
                val kmValue = kmText.toDouble()          // 1.23
                val kcalValue = (70.0 * kmValue * 0.75).roundToInt()

                _uiState.update {
                    it.copy(
                        steps = current,
                        km = kmText,
                        kcal = kcalValue.toString()
                    )
                }
                refreshWeeklySteps(_uiState.value.currentDateIso)
            }
        }
        accelSimulator?.start()
    }

    fun stopAccelerometerSimulation() {
        accelSimulator?.stop()
        accelSimulator = null
    }

    fun loadSimulatedStepsIfNeeded() {
        viewModelScope.launch {
            val midnight = todayMidnightEpochMillis()

            val savedDayStart = store.getSimDayStart()
            if (savedDayStart != midnight) {
                store.setSimDayStart(midnight)
                store.setSimStepsToday(0)

                val now = LocalDate.now()
                _uiState.update {
                    it.copy(
                        steps = 0,
                        km = stepsToKm(0),
                        kcal = "0",
                        currentDate = now.format(dateFormatter),
                        currentDayname = now.format(dayFormatter),
                        currentDateIso = now.toString()
                    )
                }
            } else {
                val saved = store.getSimStepsToday()

                val kmText = stepsToKm(saved)
                val kmValue = kmText.toDouble()
                val kcalValue = (70.0 * kmValue * 0.75).toInt()

                _uiState.update {
                    it.copy(
                        steps = saved,
                        km = kmText,
                        kcal = kcalValue.toString()
                    )
                }
            }
        }
    }

    private fun refreshWeeklySteps(todayIso: String) {
        viewModelScope.launch {
            val today = LocalDate.parse(todayIso)
            val last7 = (6 downTo 0).map { today.minusDays(it.toLong()).toString() } // ISO strings

            val values = last7.map { iso ->
                store.getStepsForDateIso(iso)
            }

            _uiState.update { it.copy(weeklySteps = values) }
        }
    }

}
