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

data class HomeUiState(
    val steps: Int = 0,
    val isTracking: Boolean = false,
    val currentDate: String = "",
    val currentDayname: String = "",
    val currentPlacename: String = "-  ",
    val locationLoading: Boolean = false,
    val locationError: String? = null
)

class HomeViewModel(
    private val appContext: Context,                 // ✅ aggiunto
    private val locationRepository: LocationRepository
) : ViewModel() {

    private val dateFormatter = DateTimeFormatter.ofPattern("d MMMM")
    private val dayFormatter = DateTimeFormatter.ofPattern("EEEE")

    // ✅ step components
    private val store = StepDataStore(appContext)
    private val sensor = StepSensor(appContext)
    private var listening = false

    private val _uiState = MutableStateFlow(
        HomeUiState(
            currentDate = LocalDate.now().format(dateFormatter),
            currentDayname = LocalDate.now().format(dayFormatter)
        )
    )
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

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
                    }

                    // reboot-safe
                    if (currentFromBoot < baseSteps) {
                        baseSteps = currentFromBoot
                        store.setBaseline(dayStart, baseSteps)
                    }

                    val todaySteps = (currentFromBoot - baseSteps).coerceAtLeast(0L).toInt()
                    _uiState.update { it.copy(steps = todaySteps) }
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

    // -------------------- DEBUG (eliminare) --------------------
    fun addTestStep(amount: Int = 200) {
        _uiState.update { it.copy(steps = it.steps + amount) }
    }

    fun resetSteps() {
        _uiState.update { it.copy(steps = 0) }
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
                _uiState.update { it.copy(steps = current) }
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
                _uiState.update { it.copy(steps = 0) }
            } else {
                val saved = store.getSimStepsToday()
                _uiState.update { it.copy(steps = saved) }
            }
        }
    }

}
