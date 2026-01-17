package com.camille.steply.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.camille.steply.data.location.LocationRepository
import com.camille.steply.data.StepDataStore
import com.camille.steply.data.StepSensor
import com.camille.steply.data.AccelerometerStepSimulator
import com.camille.steply.data.todayMidnightEpochMillis
import com.camille.steply.service.StepForegroundService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt
import com.camille.steply.data.meteo.OpenMeteoApi
import com.camille.steply.data.meteo.openMeteoCodeToEmoji



data class HomeUiState(
    val steps: Int = 0,
    val km: String = "0.00",
    val kcal: String = "0",
    val streakDays: Int = 0,
    val dailyGoal: Int = 50,
    val isTracking: Boolean = true,
    val currentDate: String = "",
    val currentDayname: String = "",
    val currentDateIso: String = "",
    val selectedDateIso: String = "",
    val selectedSteps: Int = 0,
    val selectedKm: String = "0.00",
    val selectedKcal: String = "0",
    val weeklySteps: List<Int> = List(7) { 0 },
    val currentPlacename: String = "-  ",
    val locationLoading: Boolean = false,
    val locationError: String? = null,
    val meteoLoading: Boolean = false,
    val meteoError: String? = null,
    val meteoTempC: String = "--",
    val meteoDesc: String = "-",
    val stepsByDateIso: Map<String, Int> = emptyMap()
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

    val goalNotificationEnabled = store.goalNotificationEnabledFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), true)

    private val _uiState = MutableStateFlow(
        HomeUiState(
            currentDate = LocalDate.now().format(dateFormatter),
            currentDayname = LocalDate.now().format(dayFormatter),
            currentDateIso = LocalDate.now().toString(),
            selectedDateIso = LocalDate.now().toString()
            )
    )
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        refreshWeeklySteps(_uiState.value.currentDateIso)
        refreshStreak(_uiState.value.currentDateIso, _uiState.value.dailyGoal)
        loadSelectedDayFromStore(_uiState.value.selectedDateIso)
        refreshCalendarSteps()
        viewModelScope.launch {
            store.todayStepsFlow().collect { steps ->
                val kmText = stepsToKm(steps)
                val kmValue = kmText.toDouble()
                val kcalValue = (70.0 * kmValue * 0.75).roundToInt()

                val todayIso = LocalDate.now().toString()

                _uiState.update { state ->
                    val viewingToday = state.selectedDateIso == todayIso

                    val newMap = state.stepsByDateIso.toMutableMap()
                    newMap[todayIso] = steps

                    val today = LocalDate.parse(state.currentDateIso)
                    val last7 = (6 downTo 0).map { today.minusDays(it.toLong()).toString() }
                    val newWeekly = last7.map { iso -> newMap[iso] ?: 0 }

                    state.copy(
                        steps = steps,
                        km = kmText,
                        kcal = kcalValue.toString(),
                        selectedSteps = if (viewingToday) steps else state.selectedSteps,
                        selectedKm = if (viewingToday) kmText else state.selectedKm,
                        selectedKcal = if (viewingToday) kcalValue.toString() else state.selectedKcal,
                        stepsByDateIso = newMap,
                        weeklySteps = newWeekly
                    )
                }
                refreshStreak(todayIso, _uiState.value.dailyGoal)
            }
        }
    }


    suspend fun fetchCurrentLatLngOnce(): com.camille.steply.data.location.LatLng {
        return locationRepository.getCurrentLatLng()
    }

    fun ensureTrackingRunning() {
        StepForegroundService.start(appContext)
        _uiState.update { it.copy(isTracking = true) }
    }

    fun setGoalNotificationEnabled(enabled: Boolean) {
        viewModelScope.launch { store.setGoalNotificationEnabled(enabled) }
    }


    // -------------------- LOCATION --------------------
    fun refreshPlace() {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    locationLoading = true,
                    locationError = null,
                    meteoLoading = true,
                    meteoError = null
                )
            }

            try {
                val latLng = locationRepository.getCurrentLatLng()
                //Place
                val place = locationRepository.getPlaceName(latLng.lat, latLng.lon)
                //Meteo
                val meteo = OpenMeteoApi.service.getCurrent(latLng.lat, latLng.lon)
                val currentMeteo = meteo.current
                val temp = currentMeteo?.temperature2m
                val code = currentMeteo?.weatherCode
                _uiState.update {
                    it.copy(
                        currentPlacename = place,
                        locationLoading = false,

                        meteoTempC = temp?.let { String.format(Locale.getDefault(), "%.0f", it) } ?: "--",
                        meteoDesc = code?.let { openMeteoCodeToEmoji(it) } ?: "Unknown",
                        meteoLoading = false,
                        meteoError = null
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        currentPlacename = "-",
                        locationLoading = false,
                        locationError = e.message ?: "Location failed",

                        meteoLoading = false,
                        meteoError = e.message ?: "Meteo failed"
                    )
                }
            }
        }
    }

    // -------------------- REFRESH LOCATION DOPO UN PO --------------------

    private var lastPlaceRefreshMs: Long = 0L

    fun refreshPlaceThrottled(minIntervalMs: Long = 60_000L) {
        val now = System.currentTimeMillis()
        if (now - lastPlaceRefreshMs < minIntervalMs) return
        lastPlaceRefreshMs = now
        refreshPlace()
    }



    // -------------------- Steps -> Km --------------------

    private fun stepsToKm(steps: Int, stepLengthMeters: Double = 0.74): String {
        val km = (steps * stepLengthMeters) / 1000.0
        return String.format(Locale.US, "%.2f", km)
    }

    // -------------------- Kcal --------------------

    private fun kmToKcal(km: Double, weightKg: Double = 65.0): Int {
        return (weightKg * km * 0.75).roundToInt()
    }

    private fun formatKm(km: Double): String {
        return String.format(Locale.getDefault(), "%.2f", km)
    }



    // -------------------- DASHBOARD--------------------

    private fun refreshWeeklySteps(todayIso: String) {
        viewModelScope.launch {
            val today = LocalDate.parse(todayIso)
            val last7 = (6 downTo 0).map { today.minusDays(it.toLong()).toString() }

            val values = last7.map { iso ->
                store.getStepsForDateIso(iso)
            }

            _uiState.update { it.copy(weeklySteps = values) }
        }
    }

    // -------------------- CALENDAR--------------------

    private fun refreshCalendarSteps(monthsBack: Long = 11) {
        viewModelScope.launch {
            val today = LocalDate.now()
            val start = today.minusMonths(monthsBack).withDayOfMonth(1)

            val map = LinkedHashMap<String, Int>()
            var d = start
            while (!d.isAfter(today)) {
                val iso = d.toString()
                map[iso] = store.getStepsForDateIso(iso)
                d = d.plusDays(1)
            }

            _uiState.update { it.copy(stepsByDateIso = map) }
        }
    }


    // -------------------- PASSI DI OGGI --------------------

    private fun loadSelectedDayFromStore(dateIso: String) {
        viewModelScope.launch {
            val steps = store.getStepsForDateIso(dateIso)

            val kmText = stepsToKm(steps)
            val kmValue = kmText.toDouble()
            val kcalValue = (70.0 * kmValue * 0.75).roundToInt()

            _uiState.update { state ->
                val viewingToday = state.selectedDateIso == LocalDate.now().toString()

                state.copy(
                    steps = if (viewingToday) steps else state.steps,
                    km = if (viewingToday) kmText else state.km,
                    kcal = if (viewingToday) kcalValue.toString() else state.kcal,
                    selectedSteps = steps,
                    selectedKm = kmText,
                    selectedKcal = kcalValue.toString()
                )
            }
        }
    }


    // -------------------- SELECTED DAY --------------------

    fun selectDay(dateIso: String) {
        viewModelScope.launch {
            val steps = store.getStepsForDateIso(dateIso)

            val kmText = stepsToKm(steps)
            val kmValue = kmText.toDouble()
            val kcalValue = (70.0 * kmValue * 0.75).roundToInt()

            val d = LocalDate.parse(dateIso)

            _uiState.update {
                it.copy(
                    selectedDateIso = dateIso,
                    selectedSteps = steps,
                    selectedKm = kmText,
                    selectedKcal = kcalValue.toString(),
                    currentDate = d.format(dateFormatter),
                    currentDayname = d.format(dayFormatter),
                    )
            }
        }
    }

    fun selectToday() {
        val today = LocalDate.now()
        selectDay(today.toString())
    }


    // -------------------- STREAK --------------------

    private fun refreshStreak(todayIso: String, dailyGoal: Int) {
        viewModelScope.launch {
            if (dailyGoal <= 0) {
                _uiState.update { it.copy(streakDays = 0) }
                return@launch
            }

            val today = LocalDate.parse(todayIso)

            val todaySteps = store.getStepsForDateIso(todayIso)

            val startOffset = if (todaySteps >= dailyGoal) 0 else 1

            var streak = 0

            for (i in startOffset until 365) {
                val dIso = today.minusDays(i.toLong()).toString()
                val steps = store.getStepsForDateIso(dIso)

                if (steps >= dailyGoal) streak++
                else break
            }

            _uiState.update { it.copy(streakDays = streak) }
        }
    }
    override fun onCleared() {
        super.onCleared()
    }
}
