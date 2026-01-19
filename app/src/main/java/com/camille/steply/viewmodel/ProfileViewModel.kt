package com.camille.steply.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

data class ProfileUiState(
    val username: String = "",
    val name: String = "",
    val surname: String = "",
    val weight: String = "",
    val goal: String = "10000",
    val profilePhotoUri: Uri? = null,
    val isSaving: Boolean = false,
    val message: String? = null
)

class ProfileViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState

    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
    private val db: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }
    private val storage: FirebaseStorage by lazy { FirebaseStorage.getInstance() }

    init {
        if (auth.currentUser != null) {
            loadUserData()
        }
    }

    /**
     * Carica i dati dell'utente loggato da Firestore
     */
    private fun loadUserData() {
        val uid = auth.currentUser?.uid ?: return

        // Usiamo addSnapshotListener invece di get()
        db.collection("users").document(uid).addSnapshotListener { snapshot, error ->
            if (error != null) {
                _uiState.update { it.copy(message = "Errore: ${error.message}") }
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
                        goal = snapshot.get("dailyGoal")?.toString() ?: "10,000",
                        profilePhotoUri = if (photoUrl != null) Uri.parse(photoUrl) else null
                    )
                }
            }
        }
    }

    /**
     * Registrazione nuovo utente
     */
    fun registerUser(
        name: String,
        surname: String,
        username: String,
        email: String,
        pass: String,
        weight: String,
        goal: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        _uiState.update { it.copy(isSaving = true) }

        auth.createUserWithEmailAndPassword(email, pass)
            .addOnSuccessListener { result ->
                val uid = result.user?.uid ?: return@addOnSuccessListener

                val userData = mapOf(
                    "email" to email,
                    "name" to name,
                    "surname" to surname,
                    "username" to username,
                    "weight" to weight,
                    "dailyGoal" to (goal.toIntOrNull() ?: 10000),
                    "steps" to 0,
                    "createdAt" to com.google.firebase.Timestamp.now()
                )

                db.collection("users").document(uid).set(userData)
                    .addOnSuccessListener {
                        _uiState.update { it.copy(isSaving = false) }
                        onSuccess()
                    }
                    .addOnFailureListener {
                        _uiState.update { it.copy(isSaving = false) }
                        onError("Database save error")
                    }
            }
            .addOnFailureListener { e ->
                _uiState.update { it.copy(isSaving = false) }
                onError(e.localizedMessage ?: "Registration failed")
            }
    }

    /**
     * Login utente esistente
     */
    fun loginUser(email: String, pass: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        _uiState.update { it.copy(isSaving = true, message = null) }
        auth.signInWithEmailAndPassword(email, pass)
            .addOnSuccessListener {
                _uiState.update { it.copy(isSaving = false) }
                onSuccess()
            }
            .addOnFailureListener { e ->
                _uiState.update { it.copy(isSaving = false) }
                val errorMsg = if (e.message?.contains("password") == true) "Incorrect password" else "Login failed"
                _uiState.update { it.copy(message = errorMsg) }
                onError(errorMsg)
            }
    }

    /**
     * Verifica disponibilità email
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
     * Aggiornamento profilo (Edit Profile)
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
                // FONDAMENTALE: Ricarica i dati per aggiornare lo Stato Globale
                loadUserData()

                _uiState.update { it.copy(isSaving = false) }
                onSuccess() // Torna indietro alla pagina Profilo
            }
    }

    fun updateMessage(newMessage: String?) = _uiState.update { it.copy(message = newMessage) }

    fun logout(onLogout: () -> Unit) {
        auth.signOut()
        onLogout()
    }
}