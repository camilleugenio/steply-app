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
    val password: String = "",
    val profilePhotoUri: Uri? = null,
    val isPasswordVisible: Boolean = false,
    val isSaving: Boolean = false,
    val message: String? = null
)

class ProfileViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState

    // Riferimenti a Firebase
    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
    private val db: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }
    private val storage: FirebaseStorage by lazy { FirebaseStorage.getInstance() }

    init {
        // Appena apri la schermata profilo, l'app fa il login da sola
        auth.signInWithEmailAndPassword("test@steply.it", "password")
            .addOnSuccessListener {
                _uiState.update { it.copy(message = "Login automatico riuscito!") }
            }
            .addOnFailureListener {
                _uiState.update { it.copy(message = "Errore login automatico: ${it.message}") }
            }
    }

    fun onUsernameChange(value: String) = _uiState.update { it.copy(username = value, message = null) }
    fun onNameChange(value: String) = _uiState.update { it.copy(name = value, message = null) }
    fun onSurnameChange(value: String) = _uiState.update { it.copy(surname = value, message = null) }
    fun onPasswordChange(value: String) = _uiState.update { it.copy(password = value, message = null) }
    fun onTogglePasswordVisibility() = _uiState.update { it.copy(isPasswordVisible = !it.isPasswordVisible) }
    fun onPhotoSelected(uri: Uri?) = _uiState.update { it.copy(profilePhotoUri = uri) }

    /**
     * QUESTA È LA FUNZIONE CHE VIENE CHIAMATA DAL TASTO "SALVA" NELLA UI
     */
    fun saveProfile() {
        val s = _uiState.value
        _uiState.update { it.copy(isSaving = true, message = "Salvataggio in corso...") }

        // Chiamiamo la funzione REALE invece dello stub
        saveProfileToFirebase(
            username = s.username,
            name = s.name,
            surname = s.surname,
            photoUri = s.profilePhotoUri
        )
    }

    /**
     * LOGICA REALE DI SALVATAGGIO SU CLOUD (MILANO europe-west8)
     */
    private fun saveProfileToFirebase(
        username: String,
        name: String,
        surname: String,
        photoUri: Uri?
    ) {
        val user = auth.currentUser
        if (user == null) {
            _uiState.update { it.copy(isSaving = false, message = "Errore: Devi essere loggato!") }
            return
        }

        val uid = user.uid

        if (photoUri != null) {
            // 1. Carichiamo la foto su Firebase Storage
            val storageRef = storage.reference.child("profilePhotos/$uid.jpg")

            storageRef.putFile(photoUri)
                .continueWithTask { task ->
                    if (!task.isSuccessful) task.exception?.let { throw it }
                    storageRef.downloadUrl
                }
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val downloadUrl = task.result.toString()
                        // 2. Salviamo i dati + link foto su Firestore
                        writeToFirestore(uid, username, name, surname, downloadUrl)
                    } else {
                        _uiState.update { it.copy(isSaving = false, message = "Errore caricamento immagine.") }
                    }
                }
        } else {
            // Se non c'è una nuova foto, salviamo solo i testi
            writeToFirestore(uid, username, name, surname, null)
        }
    }

    private fun writeToFirestore(uid: String, username: String, name: String, surname: String, photoUrl: String?) {
        val userMap = mutableMapOf(
            "username" to username,
            "name" to name,
            "surname" to surname,
            "updatedAt" to com.google.firebase.Timestamp.now()
        )
        photoUrl?.let { userMap["photoUrl"] = it }

        db.collection("users").document(uid)
            .set(userMap, SetOptions.merge())
            .addOnSuccessListener {
                _uiState.update { it.copy(isSaving = false, message = "Profilo aggiornato con successo!") }
            }
            .addOnFailureListener { e ->
                _uiState.update { it.copy(isSaving = false, message = "Errore Cloud: ${e.message}") }
            }
    }
}