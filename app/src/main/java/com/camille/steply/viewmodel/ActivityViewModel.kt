package com.camille.steply.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.camille.steply.data.WorkoutData
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.GeoPoint
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate

// Stato della UI per l'attività in corso
data class ActivityUiState(
    val isCountingDown: Boolean = false,
    val secondsLeft: Int = 0,
    val countdownType: WorkoutType? = null,
    val phaseText: String = "",
    val isRecording: Boolean = false,
    val currentWorkoutId: String? = null,
    val currentKm: Double = 0.0,
    val currentCalories: Double = 0.0
)

sealed class ActivityEvent {
    data class NavigateToWorkout(val type: WorkoutType) : ActivityEvent()
}

class ActivityViewModel(app: Application) : AndroidViewModel(app) {

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    private val _uiState = MutableStateFlow(ActivityUiState())
    val uiState: StateFlow<ActivityUiState> = _uiState.asStateFlow()

    private val _events = Channel<ActivityEvent>()
    val events = _events.receiveAsFlow()

    /**
     * Fa partire il countdown prima dell'inizio dell'allenamento
     */
    fun startCountdown(type: WorkoutType) {
        viewModelScope.launch {
            _uiState.update { it.copy(
                isCountingDown = true,
                secondsLeft = 3,
                countdownType = type,
                phaseText = "Ready?"
            )}

            repeat(3) { i ->
                _uiState.update { it.copy(secondsLeft = 3 - i) }
                kotlinx.coroutines.delay(1000)
            }

            _uiState.update { it.copy(isCountingDown = false) }
            startNewWorkout(type)
            _events.send(ActivityEvent.NavigateToWorkout(type))
        }
    }

    /**
     * Inizializza un nuovo allenamento (qui potrai chiamare PythonAnywhere in futuro)
     */
    fun startNewWorkout(type: WorkoutType) {
        val idTemporaneo = "PY_${System.currentTimeMillis()}"
        _uiState.update { it.copy(
            isRecording = true,
            currentWorkoutId = idTemporaneo
        )}
    }

    /**
     * Funzione chiamata alla fine dell'allenamento per impacchettare i dati
     */
    fun finishAndSaveWorkout(
        tipo: String,
        durataSec: Long,
        km: Double,
        calorie: Double,
        percorso: List<GeoPoint>,
        meteoEmoji: String,
        meteoTempC: String
    ) {
        val uid = auth.currentUser?.uid ?: return
        val idAllenamento = _uiState.value.currentWorkoutId ?: return
        val todayIso = LocalDate.now().toString()

        val finalWorkout = WorkoutData(
            idAllenamento = idAllenamento,
            idUtente = uid,
            dataIso = todayIso,
            tipo = tipo,
            durataSec = durataSec,
            km = km,
            calorie = calorie,
            percorso = percorso,
            meteoEmoji = meteoEmoji,
            meteoTempC = meteoTempC,
            timestamp = System.currentTimeMillis()
        )

        saveWorkoutToFirestore(finalWorkout)

        // Reset dello stato locale
        _uiState.update { it.copy(isRecording = false, currentWorkoutId = null) }
    }

    /**
     * Scrive effettivamente il documento su Firestore
     */
    private fun saveWorkoutToFirestore(workout: WorkoutData) {
        val uid = auth.currentUser?.uid ?: return
        val documentKey = "${workout.idAllenamento}_${workout.dataIso}"

        db.collection("users").document(uid)
            .collection("workouts").document(documentKey)
            .set(workout)
            .addOnSuccessListener {
                Log.d("WORKOUT_VM", "Allenamento salvato con successo nel Cloud!")
            }
            .addOnFailureListener { e ->
                Log.e("WORKOUT_VM", "Errore nel salvataggio: ${e.message}")
            }
    }

    fun updateCurrentWorkoutId(id: String) {
        _uiState.update { it.copy(currentWorkoutId = id) }
    }

    // Questa funzione va chiamata quando si apre la schermata del workout
    fun monitorLiveService(kmDallaService: Flow<Double>, pesoUtente: Double) {
        viewModelScope.launch {
            kmDallaService.collect { kmAttuali ->
                _uiState.update { state ->
                    state.copy(
                        currentKm = kmAttuali,
                        // Formula: Peso * Km * 0.9 (costante corsa/camminata)
                        currentCalories = pesoUtente * kmAttuali * 0.9
                    )
                }
            }
        }
    }
}