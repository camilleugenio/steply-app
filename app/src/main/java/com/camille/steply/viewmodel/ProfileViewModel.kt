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
    val weight: String = "",
    val dailyGoal: String = "10000",
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
        //auth.signOut()

        if (auth.currentUser != null) {
            loadUserData()
        }
        /* LOGIN FORZATO: Appena apri la schermata profilo, l'app fa il login da sola
        auth.signInWithEmailAndPassword("test@steply.it", "password")
            .addOnSuccessListener {
                _uiState.update { it.copy(message = "Login automatico riuscito!") }
            }
            .addOnFailureListener {
                _uiState.update { it.copy(message = "Errore login automatico: ${it.message}") }
            }*/
    }

    // FUNZIONE PER LA SCHERMATA DI REGISTRAZIONE
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
        // Indica alla UI che il caricamento è iniziato
        _uiState.update { it.copy(isSaving = true) }

        // 1. Creazione account in Firebase Authentication
        auth.createUserWithEmailAndPassword(email, pass)
            .addOnSuccessListener { result ->
                val uid = result.user?.uid ?: return@addOnSuccessListener

                // 2. Preparazione della mappa dati con i nuovi campi
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

                // 3. Salvataggio su Firestore Database
                db.collection("users").document(uid).set(userData)
                    .addOnSuccessListener {
                        _uiState.update { it.copy(isSaving = false) }
                        onSuccess()
                    }
                    .addOnFailureListener {
                        _uiState.update { it.copy(isSaving = false) }
                        onError("Errore durante il salvataggio dei dati nel database")
                    }
            }
            .addOnFailureListener { e ->
                _uiState.update { it.copy(isSaving = false) }
                onError(e.localizedMessage ?: "Errore durante la creazione dell'account")
            }
    }

    // --- NUOVA FUNZIONE PER IL LOGIN ---
    fun loginUser(
        email: String,
        pass: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        // Indica alla UI che stiamo lavorando
        _uiState.update { it.copy(isSaving = true, message = null) }

        auth.signInWithEmailAndPassword(email, pass)
            .addOnSuccessListener {
                // Login riuscito
                _uiState.update { it.copy(isSaving = false) }
                onSuccess()
            }
            .addOnFailureListener { e ->
                // Login fallito
                _uiState.update { it.copy(isSaving = false) }

                // Messaggio "precisino" in base all'errore
                val errorMsg = when {
                    e.message?.contains("password") == true -> "Password errata. Riprova!"
                    e.message?.contains("no user") == true -> "Email non trovata."
                    else -> e.localizedMessage ?: "Errore durante il login"
                }

                _uiState.update { it.copy(message = errorMsg) }
                onError(errorMsg)
            }
    }

    fun updateMessage(newMessage: String?) {
        _uiState.update { it.copy(message = newMessage) }
    }

    private fun loadUserData() {
        val uid = auth.currentUser?.uid ?: return

        db.collection("users").document(uid).get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    // Recuperiamo i dati dal database
                    val name = document.getString("name") ?: ""
                    val surname = document.getString("surname") ?: ""
                    val username = document.getString("username") ?: ""
                    val weight = document.get("weight")?.toString() ?: ""
                    val goal = document.get("dailyGoal")?.toString() ?: "10000"
                    val photoUrl = document.getString("photoUrl")

                    // Aggiorniamo la UI con i dati reali
                    _uiState.update {
                        it.copy(
                            name = name,
                            surname = surname,
                            username = username,
                            weight = weight,
                            dailyGoal = goal,
                            // Se c'è una foto, la carichiamo tramite Uri (coil la gestirà)
                            profilePhotoUri = if (photoUrl != null) Uri.parse(photoUrl) else null
                        )
                    }
                }
            }
            .addOnFailureListener { e ->
                _uiState.update { it.copy(message = "Errore caricamento: ${e.message}") }
            }
    }

    // Funzioni di cambio stato
    fun onUsernameChange(value: String) = _uiState.update { it.copy(username = value) }
    fun onNameChange(value: String) = _uiState.update { it.copy(name = value) }
    fun onSurnameChange(value: String) = _uiState.update { it.copy(surname = value) }

    fun onPasswordChange(value: String) {
        _uiState.update {
            it.copy(
                password = value,
                message = null
            )
        } // message = null pulisce l'errore
    }

    fun onTogglePasswordVisibility() =
        _uiState.update { it.copy(isPasswordVisible = !it.isPasswordVisible) }

    fun onPhotoSelected(uri: Uri?) = _uiState.update { it.copy(profilePhotoUri = uri) }

    fun logout(onLogout: () -> Unit) {
        auth.signOut()
        onLogout()
    }

    /**
     * QUESTA È LA FUNZIONE CHE VIENE CHIAMATA DAL TASTO "SALVA" NELLA UI
     */
    fun saveProfile() {
        val s = _uiState.value
        saveProfileToFirebase(s.username, s.name, s.surname, s.profilePhotoUri)
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
        val uid = auth.currentUser?.uid ?: return
        if (photoUri != null && photoUri.toString().startsWith("content")) {
            val storageRef = storage.reference.child("profilePhotos/$uid.jpg")
            storageRef.putFile(photoUri).continueWithTask { storageRef.downloadUrl }
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) writeToFirestore(
                        uid,
                        username,
                        name,
                        surname,
                        task.result.toString()
                    )
                }
        } else {
            writeToFirestore(uid, username, name, surname, photoUri?.toString())
        }
    }

    private fun writeToFirestore(
        uid: String,
        username: String,
        name: String,
        surname: String,
        photoUrl: String?
    ) {
        val userMap = mutableMapOf("username" to username, "name" to name, "surname" to surname)
        photoUrl?.let { userMap["photoUrl"] = it }
        db.collection("users").document(uid).set(userMap, SetOptions.merge())
    }

    fun checkEmailAndNext(email: String, onAvailable: () -> Unit) {
        // 1. Validazione formale dell'email
        if (!email.contains("@") || !email.contains(".")) {
            updateMessage("Please enter a valid email address.")
            return
        }

        // 2. Avvio caricamento (mostra il cerchietto nel bottone)
        _uiState.update { it.copy(isSaving = true, message = null) }

        // 3. Controllo reale su Firebase
        auth.fetchSignInMethodsForEmail(email)
            .addOnSuccessListener { result ->
                _uiState.update { it.copy(isSaving = false) }

                val methods = result.signInMethods ?: emptyList<String>()

                if (methods.isEmpty()) {
                    // Email libera, procediamo allo step successivo
                    onAvailable()
                } else {
                    // Email già occupata
                    updateMessage("An account with this email already exists.")
                }
            }
            .addOnFailureListener { e ->
                _uiState.update { it.copy(isSaving = false) }
                // Messaggio generico in caso di problemi di rete
                updateMessage("Verification failed: ${e.localizedMessage}")
            }
    }
}