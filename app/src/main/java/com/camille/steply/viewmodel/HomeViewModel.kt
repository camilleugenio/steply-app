package com.camille.steply.viewmodel

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.camille.steply.data.location.LocationRepository
import com.camille.steply.data.StepDataStore
import com.camille.steply.data.StepSensor
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
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.tasks.await


data class HomeUiState(
    val steps: Int = 0,
    val km: String = "0.00",
    val kcal: String = "0",
    val weightKg: Double? = null,
    val streakDays: Int = 0,
    val dailyGoal: Int = 0,
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

    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
    private val db: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }
    private var goalListener: ListenerRegistration? = null
    private var historyListener: ListenerRegistration? = null

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

    private var isFirestoreLoaded = false

    init {
        observeUserSettingsFromFirestore()
        syncHistoryFromFirestore()
        // Sincronizzazione prioritaria con Firestore
        observeTodayHistoryFromFirestore {
            isFirestoreLoaded = true
        }
        refreshWeeklySteps(_uiState.value.currentDateIso)
        refreshCalendarSteps()



        viewModelScope.launch {
            store.todayStepsFlow().collect { localSteps ->
                val uid = auth.currentUser?.uid ?: return@collect
                val todayIso = LocalDate.now().toString()

                // Prendiamo il valore attuale presente nella UI
                val currentStepsInUi = _uiState.value.steps

                // PROTEZIONE: Scriviamo su Firestore solo se il sensore locale è maggiore di 0
                // E se è maggiore di quanto già salvato su Firestore (evita il reset a 0)
                if (isFirestoreLoaded && localSteps > 0 && localSteps > currentStepsInUi) {
                    db.collection("users").document(uid)
                        .collection("history").document(todayIso)
                        .set(mapOf("steps" to localSteps))

                    // L'aggiornamento della UI avverrà tramite il SnapshotListener
                }
            }
        }
    }

    private fun observeTodayHistoryFromFirestore(onFirstLoad: () -> Unit = {}) {
        val uid = auth.currentUser?.uid ?: return
        val todayIso = LocalDate.now().toString()

        historyListener?.remove()
        historyListener = db.collection("users").document(uid)
            .collection("history").document(todayIso)
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener

                if (snapshot != null && snapshot.exists()) {
                    val firestoreSteps = snapshot.getLong("steps")?.toInt() ?: 0
                    updateUI(firestoreSteps, todayIso)
                }
                onFirstLoad()
            }
    }

    private fun updateUI(steps: Int, dateIso: String) {
        val todayIso = LocalDate.now().toString()

        // FILTRO ANTI-RESET: Se arrivano meno passi di quelli che abbiamo già per oggi, ignoriamo l'update.
        if (dateIso == todayIso && steps < _uiState.value.steps && _uiState.value.steps > 0) {
            return
        }

        val kmText = stepsToKm(steps)
        val weight = _uiState.value.weightKg ?: 70.0
        val kcalValue = (weight * kmText.toDouble() * 0.75).roundToInt()

        _uiState.update { state ->
            val viewingToday = state.selectedDateIso == dateIso
            state.copy(
                steps = if (dateIso == todayIso) steps else state.steps,
                km = if (dateIso == todayIso) kmText else state.km,
                kcal = if (dateIso == todayIso) kcalValue.toString() else state.kcal,
                selectedSteps = if (viewingToday) steps else state.selectedSteps,
                selectedKm = if (viewingToday) kmText else state.selectedKm,
                selectedKcal = if (viewingToday) kcalValue.toString() else state.selectedKcal
            )
        }
        refreshStreak(dateIso, _uiState.value.dailyGoal)
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

    fun refreshPlace() {
        viewModelScope.launch {
            _uiState.update { it.copy(locationLoading = true, locationError = null, meteoLoading = true, meteoError = null) }
            try {
                val latLng = locationRepository.getCurrentLatLng()
                val place = locationRepository.getPlaceName(latLng.lat, latLng.lon)
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
                _uiState.update { it.copy(currentPlacename = "-", locationLoading = false, locationError = e.message, meteoLoading = false, meteoError = e.message) }
            }
        }
    }

    private var lastPlaceRefreshMs: Long = 0L
    fun refreshPlaceThrottled(minIntervalMs: Long = 60_000L) {
        val now = System.currentTimeMillis()
        if (now - lastPlaceRefreshMs < minIntervalMs) return
        lastPlaceRefreshMs = now
        refreshPlace()
    }

    private fun stepsToKm(steps: Int, stepLengthMeters: Double = 0.74): String {
        val km = (steps * stepLengthMeters) / 1000.0
        return String.format(Locale.US, "%.2f", km)
    }

    private fun refreshWeeklySteps(todayIso: String) {
        viewModelScope.launch {
            val today = LocalDate.parse(todayIso)
            val last7 = (6 downTo 0).map { today.minusDays(it.toLong()).toString() }
            val values = last7.map { iso -> store.getStepsForDateIso(iso) }
            _uiState.update { it.copy(weeklySteps = values) }
        }
    }

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

    private fun loadSelectedDayFromStore(dateIso: String) {
        viewModelScope.launch {
            val steps = store.getStepsForDateIso(dateIso)
            val kmText = stepsToKm(steps)
            val weight = _uiState.value.weightKg ?: 70.0
            val kcalValue = (weight * kmText.toDouble() * 0.75).roundToInt()

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

    fun selectDay(dateIso: String) {
        viewModelScope.launch {
            val steps = store.getStepsForDateIso(dateIso)
            val kmText = stepsToKm(steps)
            val weight = _uiState.value.weightKg ?: 70.0
            val kcalValue = (weight * kmText.toDouble() * 0.75).roundToInt()
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
        val todayIso = LocalDate.now().toString()
        // Invece di ricaricare dal DataStore (che può essere a 0),
        // usiamo i passi che abbiamo già sincronizzato nello state
        val currentSteps = _uiState.value.steps
        val d = LocalDate.now()

        _uiState.update {
            it.copy(
                selectedDateIso = todayIso,
                selectedSteps = currentSteps,
                selectedKm = stepsToKm(currentSteps),
                selectedKcal = ((it.weightKg ?: 70.0) * stepsToKm(currentSteps).toDouble() * 0.75).roundToInt().toString(),
                currentDate = d.format(dateFormatter),
                currentDayname = d.format(dayFormatter),
            )
        }
    }

    private fun refreshStreak(todayIso: String, dailyGoal: Int) {
        viewModelScope.launch {
            if (dailyGoal <= 0) {
                _uiState.update { it.copy(streakDays = 0) }
                return@launch
            }

            val today = LocalDate.parse(todayIso)
            val todaySteps = _uiState.value.steps

            // 1. Controlliamo oggi
            var streak = 0
            if (todaySteps >= dailyGoal) {
                streak = 1
                // 2. Controlliamo i giorni passati a ritroso
                for (i in 1..365) {
                    val dIso = today.minusDays(i.toLong()).toString()
                    // Cerchiamo nella mappa dei passi che abbiamo già (caricata dal calendario/weekly)
                    val steps = store.getStepsForDateIso(dIso)

                    if (steps >= dailyGoal) {
                        streak++
                    } else {
                        break
                    }
                }
            } else {
                // Se oggi non abbiamo raggiunto il goal, lo streak potrebbe essere
                // comunque attivo se ieri lo avevamo raggiunto (streak "pendente")
                for (i in 1..365) {
                    val dIso = today.minusDays(i.toLong()).toString()
                    val steps = store.getStepsForDateIso(dIso)
                    if (steps >= dailyGoal) streak++ else break
                }
            }

            _uiState.update { it.copy(streakDays = streak) }
        }
    }

    override fun onCleared() {
        goalListener?.remove()
        historyListener?.remove()
        super.onCleared()
    }

    private fun observeUserSettingsFromFirestore() {
        val uid = auth.currentUser?.uid ?: return
        goalListener?.remove()
        goalListener = db.collection("users").document(uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null || !snapshot.exists()) return@addSnapshotListener
                val goal = (snapshot.getLong("goal") ?: snapshot.get("goal")?.toString()?.toLongOrNull() ?: 0L).toInt()
                val weightKg: Double? = runCatching {
                    val raw = snapshot.get("weight") ?: return@runCatching null
                    when (raw) {
                        is Number -> raw.toDouble()
                        is String -> raw.trim().replace(",", ".").toDoubleOrNull()
                        else -> raw.toString().trim().replace(",", ".").toDoubleOrNull()
                    }
                }.getOrNull()
                _uiState.update { it.copy(dailyGoal = goal, weightKg = weightKg) }
                viewModelScope.launch { store.setDailyGoal(goal) }
                refreshStreak(LocalDate.now().toString(), goal)
            }
    }

    fun simulateStepsDebug() {
        val uid = auth.currentUser?.uid ?: return
        val todayIso = LocalDate.now().toString()

        viewModelScope.launch {
            val currentSteps = _uiState.value.steps
            val newSteps = currentSteps + 1000

            val historyData = mapOf("steps" to newSteps)
            db.collection("users").document(uid)
                .collection("history").document(todayIso)
                .set(historyData)
        }
    }

    private fun syncHistoryFromFirestore() {
        val uid = auth.currentUser?.uid ?: return

        viewModelScope.launch {
            try {
                // 1. Scarichiamo i dati da Firestore e ASPETTIAMO (await) che finisca
                val querySnapshot = db.collection("users").document(uid)
                    .collection("history")
                    .get()
                    .await()

                // 2. Salviamo ogni giorno nel DataStore locale
                querySnapshot.documents.forEach { doc ->
                    val dateIso = doc.id
                    val steps = doc.getLong("steps")?.toInt() ?: 0
                    store.saveSteps(dateIso, steps)
                }

                // 3. SOLO ORA ricalcoliamo lo streak, perché ora i dati ci sono!
                val today = LocalDate.now().toString()
                refreshStreak(today, _uiState.value.dailyGoal)
                refreshWeeklySteps(today)
                refreshCalendarSteps()

                Log.d("STREAK_DEBUG", "Sincronizzazione completata e streak ricalcolato")
            } catch (e: Exception) {
                Log.e("STREAK_DEBUG", "Errore sync: ${e.message}")
            }
        }
    }
}