package com.camille.steply.viewmodel

import android.net.Uri
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlin.math.roundToInt
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.auth.EmailAuthProvider


data class ProfileUiState(
    val username: String = "",
    val name: String = "",
    val surname: String = "",
    val weight: String = "",
    val goal: String = "10000",
    val profilePhotoUri: Uri? = null,
    val isSaving: Boolean = false,
    val message: String? = null,

    val totalSteps: Long = 0,
    val bestDaySteps: Int = 0,
    val totalKm: String = "0.0",
    val totalWorkouts: Int = 0
)

class ProfileViewModel : ViewModel() {
    private var userDocListener: ListenerRegistration? = null
    private var historyListener: ListenerRegistration? = null

    private val authListener = FirebaseAuth.AuthStateListener { fbAuth ->
        val uid = fbAuth.currentUser?.uid
        detachListeners()

        _uiState.value = ProfileUiState()

        if (uid != null) {
            loadUserData()
            observeGlobalStats()
        }
    }
    private fun detachListeners() {
        userDocListener?.remove()
        userDocListener = null
        historyListener?.remove()
        historyListener = null
    }



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
        auth.addAuthStateListener(authListener)

        if (auth.currentUser != null) {
            loadUserData()
            observeGlobalStats()
        }
    }

    // --- USER DATA LOADING ---

    private fun loadUserData() {
        val uid = auth.currentUser?.uid ?: return

        userDocListener?.remove()
        userDocListener=db.collection("users").document(uid).addSnapshotListener { snapshot, error ->
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
                        goal = snapshot.get("goal")?.toString() ?: "10000",
                        profilePhotoUri = photoUrl?.toUri()
                    )
                }
            }
        }
    }


    private fun observeGlobalStats() {
        val uid = auth.currentUser?.uid ?: return

        historyListener?.remove()
        historyListener=db.collection("users").document(uid).collection("history")
            .addSnapshotListener { querySnapshot, error ->
                if (error != null || querySnapshot == null) return@addSnapshotListener

                var total = 0L
                var best = 0

                for (document in querySnapshot.documents) {
                    val steps = document.getLong("steps")?.toInt() ?: 0
                    total += steps
                    if (steps > best) best = steps
                }

                val rawKm = (total * 0.74) / 1000.0
                val roundedKm = (rawKm * 10).roundToInt() / 10.0

                _uiState.update { it.copy(
                    totalSteps = total,
                    bestDaySteps = best,
                    totalKm = roundedKm.toString(),
                    totalWorkouts = querySnapshot.size()
                )}
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

        if (storageRef == null) return

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
                            _uiState.update { it.copy(isSaving = false, profilePhotoUri = downloadUri.toUri()) }
                            onSuccess(downloadUri)
                        }
                } else {
                    _uiState.update { it.copy(isSaving = false) }
                }
            }
    }

    fun changePassword(
        currentPassword: String,
        newPassword: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val user = auth.currentUser ?: run {
            onError("No user logged in")
            return
        }

        val email = user.email ?: run {
            onError("Missing email")
            return
        }

        val credential = EmailAuthProvider.getCredential(email, currentPassword)

        user.reauthenticate(credential)
            .addOnSuccessListener {
                user.updatePassword(newPassword)
                    .addOnSuccessListener { onSuccess() }
                    .addOnFailureListener { e ->
                        onError(e.message ?: "Error updating password")
                    }
            }
            .addOnFailureListener { e ->
                onError("Wrong current password")
            }
    }

    override fun onCleared() {
        auth.removeAuthStateListener(authListener)
        detachListeners()
        super.onCleared()
    }


    fun logout(onLogout: () -> Unit) {
        auth.signOut()
        onLogout()
    }
}