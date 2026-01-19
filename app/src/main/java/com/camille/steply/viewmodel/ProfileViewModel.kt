package com.camille.steply.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.core.net.toUri

// Modello per la singola voce di peso nello storico
data class WeightEntry(
    val date: String = "",
    val weight: Double = 0.0,
    val timestamp: com.google.firebase.Timestamp? = null
)

data class ProfileUiState(
    val username: String = "",
    val name: String = "",
    val surname: String = "",
    val weight: String = "",
    val goal: String = "10000",
    val profilePhotoUri: Uri? = null,
    val weightHistory: List<WeightEntry> = emptyList(),
    val isSaving: Boolean = false,
    val message: String? = null
)

class ProfileViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState

    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
    private val db: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }

    init {
        if (auth.currentUser != null) {
            loadUserData()
            loadWeightHistory()
        }
    }

    /**
     * Carica i dati del profilo utente in tempo reale
     */
    private fun loadUserData() {
        val uid = auth.currentUser?.uid ?: return

        db.collection("users").document(uid).addSnapshotListener { snapshot, error ->
            if (error != null) {
                _uiState.update { it.copy(message = "Error: ${error.message}") }
                return@addSnapshotListener
            }

            if (snapshot != null && snapshot.exists()) {
                val photoUrl = snapshot.getString("photoUrl")
                _uiState.update {
                    it.copy(
                        name = snapshot.getString("name") ?: "",
                        surname = snapshot.getString("surname") ?: "",
                        username = snapshot.getString("username") ?: "",
                        weight = snapshot.get("weight")?.toString() ?: "",
                        goal = snapshot.get("dailyGoal")?.toString() ?: "10000",
                        profilePhotoUri = photoUrl?.toUri()
                    )
                }
            }
        }
    }

    /**
     * Carica lo storico dei pesi ordinato per tempo (necessario per il grafico)
     */
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

    /**
     * Registrazione nuovo utente
     */
    fun registerUser(
        name: String, surname: String, username: String,
        email: String, pass: String, weight: String, goal: String,
        onSuccess: () -> Unit, onError: (String) -> Unit
    ) {
        _uiState.update { it.copy(isSaving = true) }

        auth.createUserWithEmailAndPassword(email, pass)
            .addOnSuccessListener { result ->
                val uid = result.user?.uid ?: return@addOnSuccessListener
                val userData = mapOf(
                    "email" to email, "name" to name, "surname" to surname,
                    "username" to username, "weight" to weight,
                    "dailyGoal" to (goal.toIntOrNull() ?: 10000),
                    "steps" to 0, "createdAt" to com.google.firebase.Timestamp.now()
                )

                db.collection("users").document(uid).set(userData)
                    .addOnSuccessListener {
                        saveWeightToHistory(uid, weight)
                        _uiState.update { it.copy(isSaving = false) }
                        onSuccess()
                    }
            }
            .addOnFailureListener { e ->
                _uiState.update { it.copy(isSaving = false) }
                onError(e.localizedMessage ?: "Registration failed")
            }
    }

    /**
     * Login
     */
    fun loginUser(email: String, pass: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        _uiState.update { it.copy(isSaving = true) }
        auth.signInWithEmailAndPassword(email, pass)
            .addOnSuccessListener {
                _uiState.update { it.copy(isSaving = false) }
                onSuccess()
            }
            .addOnFailureListener { e ->
                _uiState.update { it.copy(isSaving = false) }
                onError(e.localizedMessage ?: "Login failed")
            }
    }

    /**
     * Controllo email durante la registrazione
     */
    fun checkEmailAndNext(email: String, onAvailable: () -> Unit) {
        if (!email.contains("@") || !email.contains(".")) {
            updateMessage("Invalid email format.")
            return
        }
        _uiState.update { it.copy(isSaving = true, message = null) }
        auth.fetchSignInMethodsForEmail(email)
            .addOnSuccessListener { result ->
                _uiState.update { it.copy(isSaving = false) }
                if (result.signInMethods?.isEmpty() == true) onAvailable()
                else updateMessage("Email already in use.")
            }
            .addOnFailureListener { e ->
                _uiState.update { it.copy(isSaving = false, message = e.localizedMessage) }
            }
    }

    /**
     * Aggiornamento profilo completo e salvataggio automatico dello storico
     */
    fun updateFullProfile(newName: String, newSurname: String, newWeight: String, newGoal: String, onSuccess: () -> Unit) {
        val uid = auth.currentUser?.uid ?: return
        _uiState.update { it.copy(isSaving = true) }

        val userMap = mapOf(
            "name" to newName,
            "surname" to newSurname,
            "weight" to newWeight,
            "dailyGoal" to (newGoal.toIntOrNull() ?: 10000)
        )

        db.collection("users").document(uid).set(userMap, SetOptions.merge())
            .addOnSuccessListener {
                saveWeightToHistory(uid, newWeight)
                _uiState.update { it.copy(isSaving = false) }
                onSuccess()
            }
    }

    /**
     * Salva il peso nella sottocollezione.
     * Gestisce la conversione della virgola in punto per evitare errori di database.
     */
    private fun saveWeightToHistory(uid: String, weightStr: String) {
        if (weightStr.isEmpty()) return
        val weightValue = weightStr.replace(",", ".").toDoubleOrNull() ?: return

        val currentDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val weightData = WeightEntry(
            date = currentDate,
            weight = weightValue,
            timestamp = com.google.firebase.Timestamp.now()
        )

        db.collection("users").document(uid)
            .collection("weight_history").document(currentDate)
            .set(weightData)
    }

    fun updateMessage(newMessage: String?) = _uiState.update { it.copy(message = newMessage) }

    fun logout(onLogout: () -> Unit) {
        auth.signOut()
        onLogout()
    }

    fun updateWeight(newWeight: String) {
        val userId = auth.currentUser?.uid ?: return

        // 1. Puliamo la stringa (gestiamo virgole e punti)
        val formattedWeight = newWeight.replace(",", ".")
        val weightDouble = formattedWeight.toDoubleOrNull() ?: return

        // 2. Prepariamo la data di oggi
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val todayDate = sdf.format(Date())
        val timestamp = com.google.firebase.Timestamp.now()

        // 3. Aggiorniamo il peso principale dell'utente
        db.collection("users").document(userId)
            .update("weight", formattedWeight)

        // 4. Aggiungiamo la pesata alla cronologia (sottocollezione)
        val historyEntry = hashMapOf(
            "weight" to weightDouble,
            "date" to todayDate,
            "timestamp" to timestamp
        )

        db.collection("users").document(userId)
            .collection("weight_history").document(todayDate) // Usiamo la data come ID per evitare doppioni nello stesso giorno
            .set(historyEntry)
            .addOnSuccessListener {
                // I dati si aggiorneranno automaticamente grazie allo snapshotListener che abbiamo già
            }
    }
}