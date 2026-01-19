package com.camille.steply.viewmodel

import android.net.Uri
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import android.util.Log

/**
 * Data model for weight history entries
 */
data class WeightEntry(
    val date: String = "",
    val weight: Double = 0.0,
    val timestamp: com.google.firebase.Timestamp? = null
)

/**
 * UI State for the Profile and Auth flow
 */
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
    //private val storage = FirebaseStorage.getInstance()
    private val db: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }

    private val storage: FirebaseStorage? by lazy {
        try {
            // Se il bucket ti dà ancora problemi, usa getInstance() senza parametri
            FirebaseStorage.getInstance()
        } catch (e: Exception) {
            null
        }
    }
    init {
        if (auth.currentUser != null) {
            loadUserData()
            loadWeightHistory()
        }
    }

    // --- USER DATA LOADING ---

    private fun loadUserData() {
        val uid = auth.currentUser?.uid ?: return

        db.collection("users").document(uid).addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.e("DEBUG_FOTO", "Errore Firestore: ${error.message}") // <--- LOG ERRORE
                _uiState.update { it.copy(message = "Error: ${error.message}") }
                return@addSnapshotListener
            }

            if (snapshot != null && snapshot.exists()) {
                val photoUrl = snapshot.getString("photoUrl")

                // --- AGGIUNGI QUESTA RIGA QUI ---
                Log.d("DEBUG_FOTO", "URL recuperato dal DB: $photoUrl")

                _uiState.update {
                    it.copy(
                        name = snapshot.getString("name") ?: "",
                        surname = snapshot.getString("surname") ?: "",
                        username = snapshot.getString("username") ?: "",
                        weight = snapshot.get("weight")?.toString() ?: "",
                        goal = snapshot.get("goal")?.toString() ?: "10000",
                        profilePhotoUri = photoUrl?.toUri()
                    )
                }
            } else {
                Log.d("DEBUG_FOTO", "Il documento non esiste per l'UID: $uid")
            }
        }
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

    // --- AUTHENTICATION FUNCTIONS ---

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

    fun checkEmailAndNext(email: String, onAvailable: () -> Unit) {
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

    // --- PROFILE UPDATE FUNCTIONS ---

    fun updateFullProfile(newName: String, newSurname: String, newWeight: String, newGoal: String, onSuccess: () -> Unit) {
        val uid = auth.currentUser?.uid ?: return
        _uiState.update { it.copy(isSaving = true) }

        val userMap = mapOf(
            "name" to newName,
            "surname" to newSurname,
            "weight" to newWeight,
            "goal" to (newGoal.toIntOrNull() ?: 10000)
        )

        db.collection("users").document(uid).set(userMap, SetOptions.merge())
            .addOnSuccessListener {
                saveWeightToHistory(uid, newWeight)
                _uiState.update { it.copy(isSaving = false) }
                onSuccess()
            }
    }

    fun uploadProfilePicture(uri: Uri, onSuccess: (String) -> Unit) {
        val uid = auth.currentUser?.uid ?: return

        // USIAMO IL PUNTO DI DOMANDA: storage?.
        // Se storage è null, l'intera riga non viene eseguita e storageRef sarà null
        val storageRef = storage?.reference?.child("profile_pics/$uid.jpg")

        if (storageRef == null) {
            Log.e("Profile", "Storage non disponibile (bucket errato o rete)")
            return
        }

        _uiState.update { it.copy(isSaving = true) }

        storageRef.putFile(uri)
            .addOnFailureListener { e ->
                Log.e("Upload", "Errore: ${e.message}")
                _uiState.update { it.copy(isSaving = false) }
            }
            .continueWithTask { task ->
                if (!task.isSuccessful) task.exception?.let { throw it }
                storageRef.downloadUrl
            }
            .addOnCompleteListener { task ->
                // ... resto del codice identico ...
                if (task.isSuccessful) {
                    val downloadUri = task.result.toString()
                    db.collection("users").document(uid).update("photoUrl", downloadUri)
                        .addOnSuccessListener {
                            _uiState.update { it.copy(isSaving = false, profilePhotoUri = Uri.parse(downloadUri)) }
                            onSuccess(downloadUri)
                        }
                } else {
                    _uiState.update { it.copy(isSaving = false) }
                }
            }
    }



    // --- WEIGHT LOGGING FUNCTIONS ---

    fun updateWeight(newWeight: String) {
        val userId = auth.currentUser?.uid ?: return
        val formattedWeight = newWeight.replace(",", ".")
        val weightDouble = formattedWeight.toDoubleOrNull() ?: return

        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val todayDate = sdf.format(Date())

        db.collection("users").document(userId).update("weight", formattedWeight)

        val historyEntry = hashMapOf(
            "weight" to weightDouble,
            "date" to todayDate,
            "timestamp" to com.google.firebase.Timestamp.now()
        )

        db.collection("users").document(userId)
            .collection("weight_history").document(todayDate)
            .set(historyEntry)
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

    // --- ACCOUNT SETTINGS ---

    fun changePassword(newPassword: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        auth.currentUser?.updatePassword(newPassword)
            ?.addOnCompleteListener { task ->
                if (task.isSuccessful) onSuccess()
                else onError(task.exception?.message ?: "Error updating password")
            }
    }

    fun logout(onLogout: () -> Unit) {
        auth.signOut()
        onLogout()
    }

    fun updateMessage(newMessage: String?) = _uiState.update { it.copy(message = newMessage) }
}