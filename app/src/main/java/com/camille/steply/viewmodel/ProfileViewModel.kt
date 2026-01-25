package com.camille.steply.viewmodel

import android.net.Uri
import android.util.Log
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

/**
 * UI State for the Profile
 */
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

    private val storage: FirebaseStorage? by lazy {
        try {
            FirebaseStorage.getInstance()
        } catch (e: Exception) {
            null
        }
    }

    init {
        // Carichiamo i dati utente solo se loggato
        if (auth.currentUser != null) {
            loadUserData()
        }
    }

    // --- USER DATA LOADING ---

    private fun loadUserData() {
        val uid = auth.currentUser?.uid ?: return

        db.collection("users").document(uid).addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.e("DEBUG_FOTO", "Errore Firestore: ${error.message}")
                _uiState.update { it.copy(message = "Error: ${error.message}") }
                return@addSnapshotListener
            }

            if (snapshot != null && snapshot.exists()) {
                val photoUrl = snapshot.getString("photoUrl")
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
            }
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
                _uiState.update { it.copy(isSaving = false) }
                onSuccess()
            }
            .addOnFailureListener { e ->
                _uiState.update { it.copy(isSaving = false, message = e.localizedMessage) }
            }
    }

    fun uploadProfilePicture(uri: Uri, onSuccess: (String) -> Unit) {
        val uid = auth.currentUser?.uid ?: return
        val storageRef = storage?.reference?.child("profile_pics/$uid.jpg")

        if (storageRef == null) {
            Log.e("Profile", "Storage non disponibile")
            return
        }

        _uiState.update { it.copy(isSaving = true) }

        storageRef.putFile(uri)
            .continueWithTask { task ->
                if (!task.isSuccessful) task.exception?.let { throw it }
                storageRef.downloadUrl
            }
            .addOnCompleteListener { task ->
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

    //fun updateMessage(newMessage: String?) = _uiState.update { it.copy(message = newMessage) }
}