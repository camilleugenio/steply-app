package com.camille.steply.viewmodel

import android.app.Application
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.camille.steply.data.WorkoutData
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.GeoPoint
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.time.LocalDate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel


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

    private val uploadScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)


    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    private val _uiState = MutableStateFlow(ActivityUiState())
    val uiState: StateFlow<ActivityUiState> = _uiState.asStateFlow()

    private val _events = Channel<ActivityEvent>()
    val events = _events.receiveAsFlow()

    override fun onCleared() {
        super.onCleared()
        uploadScope.cancel()
    }

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

    fun startNewWorkout(type: WorkoutType) {
        val idTemporaneo = "PY_${System.currentTimeMillis()}"
        _uiState.update { it.copy(
            isRecording = true,
            currentWorkoutId = idTemporaneo
        )}
    }

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
        val documentKey = "${idAllenamento}_$todayIso"

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

        saveWorkoutToFirestore(finalWorkout, documentKey)

        _uiState.update { it.copy(isRecording = false, currentWorkoutId = null) }
    }

    private fun saveWorkoutToFirestore(workout: WorkoutData, documentKey: String) {
        val uid = auth.currentUser?.uid ?: return

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

    fun monitorLiveService(kmDallaService: Flow<Double>, pesoUtente: Double) {
        viewModelScope.launch {
            kmDallaService.collect { kmAttuali ->
                _uiState.update { state ->
                    state.copy(
                        currentKm = kmAttuali,
                        // Formula: Peso * Km * 0.9
                        currentCalories = pesoUtente * kmAttuali * 0.9
                    )
                }
            }
        }
    }
    //
    fun uploadWorkoutPhoto(workoutId: String, photoUri: Uri) {
        val uid = auth.currentUser?.uid ?: return
        val todayIso = LocalDate.now().toString()
        val documentKey = "${workoutId}_$todayIso"

        //viewModelScope.launch(Dispatchers.IO) {
        uploadScope.launch {
            try {
                // 1. Carica su Storage
                val storageRef = FirebaseStorage.getInstance().reference
                    .child("users")
                    .child(uid)
                    .child("workout_photos")
                    .child("$workoutId.jpg")

                storageRef.putFile(photoUri).await()
                val downloadUrl = storageRef.downloadUrl.await().toString()

                // 2. Aggiorna il documento Firestore esistente
                db.collection("users").document(uid)
                    .collection("workouts").document(documentKey)
                    .set(mapOf("photoUrl" to downloadUrl), com.google.firebase.firestore.SetOptions.merge())
                    .await()

                Log.d("WORKOUT_VM", "Foto salvata con successo: $downloadUrl")
            } catch (e: Exception) {
                Log.e("WORKOUT_VM", "Errore upload foto: ${e.message}")
            }
        }
    }

    fun deleteWorkout(snap: WorkoutReportSnapshot) {
        val uid = auth.currentUser?.uid ?: return

        val idAllenamento = snap.idAllenamento ?: ""

        val dateIso = java.time.Instant.ofEpochMilli(snap.startTimeMs)
            .atZone(java.time.ZoneId.systemDefault())
            .toLocalDate()
            .toString()

        val documentKey = "${idAllenamento}_$dateIso"

        viewModelScope.launch(Dispatchers.IO) {
            try {
                // 1. Elimina da Firestore
                db.collection("users").document(uid)
                    .collection("workouts").document(documentKey)
                    .delete()
                    .await()

                // 2. Elimina la foto dallo Storage (se presente)
                val storageRef = FirebaseStorage.getInstance().reference
                    .child("users")
                    .child(uid)
                    .child("workout_photos")
                    .child("$idAllenamento.jpg")

                try {
                    storageRef.delete().await()
                } catch (e: Exception) {
                    // Foto non presente, ignoriamo
                }

                Log.d("DELETE_DEBUG", "Eliminato documento: $documentKey")
            } catch (e: Exception) {
                Log.e("DELETE_DEBUG", "Errore eliminazione: ${e.message}")
            }
        }
    }
}