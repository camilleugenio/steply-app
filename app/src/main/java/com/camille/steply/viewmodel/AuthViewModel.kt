package com.camille.steply.viewmodel

import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Stato specifico per l'autenticazione
data class AuthUiState(
    val isSaving: Boolean = false,
    val message: String? = null,
    val isRegistered: Boolean = false
)

class AuthViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    // Logica di controllo email
    fun checkEmailAndNext(email: String, onAvailable: () -> Unit) {
        _uiState.update { it.copy(isSaving = true, message = null) }
        auth.fetchSignInMethodsForEmail(email)
            .addOnSuccessListener { result ->
                _uiState.update { it.copy(isSaving = false) }
                if (result.signInMethods?.isEmpty() == true) onAvailable()
                else _uiState.update { it.copy(message = "Email already in use.") }
            }
            .addOnFailureListener { e ->
                _uiState.update { it.copy(isSaving = false, message = e.localizedMessage) }
            }
    }

    // REGISTRATION FUNCTION
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
                    "goal" to (goal.toIntOrNull() ?: 10000),
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

    // LOGIN FUNCTION
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

    fun updateMessage(msg: String?) {
        _uiState.update { it.copy(message = msg) }
    }

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
}