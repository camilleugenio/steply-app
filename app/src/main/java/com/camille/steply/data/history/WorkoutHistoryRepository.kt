package com.camille.steply.data.history

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.camille.steply.viewmodel.WorkoutReportSnapshot
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val Context.dataStore by preferencesDataStore(name = "workout_history")

class WorkoutHistoryRepository(private val context: Context) {

    private val KEY = stringPreferencesKey("history_json")

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    val history: Flow<List<WorkoutReportSnapshot>> =
        context.dataStore.data.map { prefs ->
            val raw = prefs[KEY] ?: "[]"
            runCatching { json.decodeFromString<List<WorkoutReportSnapshot>>(raw) }
                .getOrElse { emptyList() }
                .sortedByDescending { it.startTimeMs }
        }

    suspend fun add(snapshot: WorkoutReportSnapshot) {
        context.dataStore.edit { prefs ->
            val current = runCatching {
                json.decodeFromString<List<WorkoutReportSnapshot>>(prefs[KEY] ?: "[]")
            }.getOrElse { emptyList() }

            val updated = (listOf(snapshot) + current)
                .sortedByDescending { it.startTimeMs }

            prefs[KEY] = json.encodeToString(updated)
        }
    }

    suspend fun clearAll() {
        context.dataStore.edit { it.remove(KEY) }
    }

    suspend fun updatePhoto(startTimeMs: Long, photoUri: String?) {
        context.dataStore.edit { prefs ->
            val current = runCatching {
                json.decodeFromString<List<WorkoutReportSnapshot>>(prefs[KEY] ?: "[]")
            }.getOrElse { emptyList() }

            val updated = current.map { snap ->
                if (snap.startTimeMs == startTimeMs) snap.copy(photoUri = photoUri) else snap
            }.sortedByDescending { it.startTimeMs }

            prefs[KEY] = json.encodeToString(updated)
        }
    }
}
