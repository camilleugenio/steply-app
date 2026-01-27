package com.camille.steply.data

import com.google.firebase.firestore.GeoPoint

/**
 * Rappresenta un singolo allenamento dell'utente.
 * La chiave del documento su Firestore sarà: "${idAllenamento}_${dataIso}"
 */
data class WorkoutData(
    // ID generato dal server PythonAnywhere
    val idAllenamento: String = "",

    // ID dell'utente (per sicurezza nella tupla)
    val idUtente: String = "",

    // Data in formato ISO (es: 2024-02-14)
    val dataIso: String = "",

    // Timestamp per ordinare i workout dal più recente al più vecchio
    val timestamp: Long = System.currentTimeMillis(),

    // Tipologia (Corsa, Camminata, etc.)
    val tipo: String = "Camminata",

    // Dati calcolati
    val durataSec: Long = 0,
    val km: Double = 0.0,
    val calorie: Double = 0.0,

    // Percorso GPS (Mappa) - Firestore supporta direttamente la lista di GeoPoint
    val percorso: List<GeoPoint> = emptyList(),

    // Riferimento alla foto salvata localmente (es: "workout_photo_123.jpg")
    val photoUrl: String? = null,

    val meteoEmoji: String = "",
    val meteoTempC: String = "--",

    )