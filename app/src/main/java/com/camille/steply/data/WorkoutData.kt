package com.camille.steply.data

import com.google.firebase.firestore.GeoPoint

// User Workout
data class WorkoutData(
    // PythonAnywhere ID
    val idAllenamento: String = "",

    // User ID
    val idUtente: String = "",

    // Date
    val dataIso: String = "",

    // Workout order
    val timestamp: Long = System.currentTimeMillis(),

    // Workout Type
    val tipo: String = "Camminata",

    // Data
    val durataSec: Long = 0,
    val km: Double = 0.0,
    val calorie: Double = 0.0,

    // Path
    val percorso: List<GeoPoint> = emptyList(),

    // Photo
    val photoUrl: String? = null,

    val meteoEmoji: String = "",
    val meteoTempC: String = "--",

    )