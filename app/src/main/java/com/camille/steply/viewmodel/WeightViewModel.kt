package com.camille.steply.viewmodel

import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class WeightEntry(
    val date: String = "",
    val weight: Double = 0.0,
    val timestamp: com.google.firebase.Timestamp? = null
)

data class WeightUiState(
    val weightHistory: List<WeightEntry> = emptyList(),
    val isSaving: Boolean = false
)

class WeightViewModel : ViewModel() {

    // Inizializziamo lo stato
    private val _uiState = MutableStateFlow(WeightUiState())
    val uiState: StateFlow<WeightUiState> = _uiState

    // Riferimenti a Firebase
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()

    init {
        // Carica i dati appena il ViewModel viene creato
        loadWeightHistory()
    }

    fun loadWeightHistory() {
        val uid = auth.currentUser?.uid ?: return

        db.collection("users").document(uid)
            .collection("weight_history")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener

                val history = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(WeightEntry::class.java)
                } ?: emptyList()

                _uiState.update { it.copy(weightHistory = history) }
            }
    }

    fun addWeightEntry(newWeight: String, onSuccess: () -> Unit) {
        val userId = auth.currentUser?.uid ?: return
        val formattedWeight = newWeight.replace(",", ".")
        val weightDouble = formattedWeight.toDoubleOrNull() ?: return

        val sdf = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
        val todayDate = sdf.format(Date())

        _uiState.update { it.copy(isSaving = true) }

        val historyEntry = hashMapOf(
            "weight" to weightDouble,
            "date" to todayDate,
            "timestamp" to com.google.firebase.Timestamp.now()
        )

        db.runBatch { batch ->
            val userRef = db.collection("users").document(userId)
            val historyRef = db.collection("users").document(userId)
                .collection("weight_history").document(todayDate)

            batch.update(userRef, "weight", formattedWeight)
            batch.set(historyRef, historyEntry)
        }.addOnCompleteListener {
            _uiState.update { it.copy(isSaving = false) }
            if (it.isSuccessful) {
                onSuccess()
            }
        }
    }
}