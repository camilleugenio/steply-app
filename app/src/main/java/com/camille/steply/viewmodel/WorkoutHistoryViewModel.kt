package com.camille.steply.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.camille.steply.data.WorkoutData
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.roundToInt

class WorkoutHistoryViewModel(app: Application) : AndroidViewModel(app) {
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    // Flusso per la lista dei workout
    private val _items = MutableStateFlow<List<WorkoutReportSnapshot>>(emptyList())
    val items: StateFlow<List<WorkoutReportSnapshot>> = _items.asStateFlow()

    // Flusso per lo stato di caricamento
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    fun monitorUserHistory() {
        val uid = auth.currentUser?.uid ?: return

        if (_items.value.isEmpty()) {
            _isLoading.value = true
        }

        db.collection("users").document(uid).collection("workouts")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, e ->
                _isLoading.value = false

                if (e != null) {
                    android.util.Log.e("HISTORY", "Errore Firestore: ${e.message}")
                    return@addSnapshotListener
                }

                val list = snapshot?.documents?.mapNotNull { doc ->
                    val workout = doc.toObject(WorkoutData::class.java)
                    workout?.let {

                        val points = it.percorso.map { gp ->
                            LatLngP(gp.latitude, gp.longitude)
                        }

                        val segments = if (points.size >= 2) {
                            listOf(
                                TrackSegmentP(
                                    dashed = false,
                                    points = points
                                )
                            )
                        } else {
                            emptyList()
                        }

                        val startPoint = points.firstOrNull()

                        // Mappiamo i dati da Firebase al modello della UI
                        WorkoutReportSnapshot(
                            idAllenamento = it.idAllenamento,
                            type = it.tipo,
                            startTimeMs = it.timestamp,
                            durationSec = it.durataSec.toInt(),
                            distanceMeters = it.km * 1000.0,
                            kcal = it.calorie.roundToInt(),
                            meteoEmoji = it.meteoEmoji,
                            meteoTempC = it.meteoTempC,
                            startPoint = startPoint,
                            segments = segments,
                            photoUri = it.photoUrl
                        )
                    }
                } ?: emptyList()

                _items.value = list
            }
    }

}