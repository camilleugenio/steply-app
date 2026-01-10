package com.camille.steply.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

data class ProfileUiState(
    val username: String = "",
    val name: String = "",
    val surname: String = "",
    val password: String = "",
    val profilePhotoUri: Uri? = null,
    val isPasswordVisible: Boolean = false,
    val isSaving: Boolean = false,
    val message: String? = null
)

class ProfileViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState

    // Firebase handles (stub-ready)
    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
    private val db: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }
    private val storage: FirebaseStorage by lazy { FirebaseStorage.getInstance() }

    fun onUsernameChange(value: String) = _uiState.update { it.copy(username = value, message = null) }
    fun onNameChange(value: String) = _uiState.update { it.copy(name = value, message = null) }
    fun onSurnameChange(value: String) = _uiState.update { it.copy(surname = value, message = null) }
    fun onPasswordChange(value: String) = _uiState.update { it.copy(password = value, message = null) }

    fun onPhotoSelected(uri: Uri?) {
        _uiState.update { it.copy(profilePhotoUri = uri, message = null) }
    }

    fun togglePasswordVisibility() {
        _uiState.update { it.copy(isPasswordVisible = !it.isPasswordVisible) }
    }

    fun saveProfile() {
        val s = _uiState.value
        if (s.username.isBlank() || s.name.isBlank() || s.surname.isBlank() || s.password.isBlank()) {
            _uiState.update { it.copy(message = "Please fill all fields.") }
            return
        }
        if (s.password.length < 6) {
            _uiState.update { it.copy(message = "Password must be at least 6 characters.") }
            return
        }

        _uiState.update { it.copy(isSaving = true, message = null) }

        // ✅ Firebase stub: later you’ll replace this with real calls
        saveProfileToFirebaseStub(
            username = s.username,
            name = s.name,
            surname = s.surname,
            password = s.password,
            photoUri = s.profilePhotoUri
        )
    }

    /**
     * STUB ONLY.
     * Here’s the intended flow when you implement it:
     * 1) Ensure user is authenticated (auth.currentUser != null)
     * 2) Upload photoUri to Storage (if not null) and get downloadUrl
     * 3) Write username/name/surname/photoUrl to Firestore under users/{uid}
     * 4) (Optional) update password via auth.currentUser?.updatePassword(...)
     */
    private fun saveProfileToFirebaseStub(
        username: String,
        name: String,
        surname: String,
        password: String,
        photoUri: Uri?
    ) {
        // This line prevents "unused" warnings while still being a stub:
        val uid = auth.currentUser?.uid

        // Pretend we did it (stub behavior)
        _uiState.update {
            it.copy(
                isSaving = false,
                message = if (uid == null)
                    "Firebase stub: no logged-in user (auth.currentUser is null)."
                else
                    "Firebase stub: would save profile for uid=$uid ✅"
            )
        }

        // --- Real implementation later (outline) ---
        // val user = auth.currentUser ?: return
        // val userDoc = db.collection("users").document(user.uid)
        //
        // if (photoUri != null) {
        //   val ref = storage.reference.child("profilePhotos/${user.uid}.jpg")
        //   ref.putFile(photoUri).continueWithTask { ref.downloadUrl }.addOnSuccessListener { url ->
        //       userDoc.set(mapOf("username" to username, "name" to name, "surname" to surname, "photoUrl" to url.toString()))
        //   }
        // } else {
        //   userDoc.set(mapOf("username" to username, "name" to name, "surname" to surname), SetOptions.merge())
        // }
        //
        // user.updatePassword(password) // optional + requires recent login
    }
}
