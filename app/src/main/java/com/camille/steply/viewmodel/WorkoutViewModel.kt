package com.camille.steply.viewmodel

import android.location.Location
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.camille.steply.data.location.LatLng
import com.camille.steply.network.KcalApi
import com.camille.steply.network.StartWorkoutRequest
import com.camille.steply.network.UpdateWorkoutRequest
import com.camille.steply.service.WorkoutLocationService
import com.google.android.gms.maps.model.LatLng as GLatLng
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class WorkoutMapState(
    val paused: Boolean = false,
    val startPoint: GLatLng? = null,
    val segments: List<TrackSegment> = listOf(TrackSegment(dashed = false)),
    val distanceMeters: Double = 0.0
)

data class TrackSegment(
    val dashed: Boolean,
    val points: List<GLatLng> = emptyList()
)

class WorkoutViewModel(
    private val kcalApi: KcalApi
) : ViewModel() {

    private val _state = MutableStateFlow(WorkoutMapState())
    val state: StateFlow<WorkoutMapState> = _state.asStateFlow()

    private val _currentWorkoutId = MutableStateFlow<String?>(null)
    val currentWorkoutId: StateFlow<String?> = _currentWorkoutId.asStateFlow()
    private var locationJob: Job? = null
    private var last: LatLng? = null


    // -------- KCAL (SERVER) --------
    private val _kcal = MutableStateFlow(0.0)
    val kcal: StateFlow<Double> = _kcal.asStateFlow()

    private var workoutId: String? = null
    private var kcalJob: Job? = null

    fun startRemoteSessionAndLoop(
        activity: String,
        weightKg: Double,
        ageYears: Int,
        sex: String,
        elapsedSecProvider: () -> Int,
        intervalMs: Long = 10_000L
    ) {
        // already running
        if (workoutId != null && kcalJob != null) return

        viewModelScope.launch {
            // 1) ensure we have a workoutId
            if (workoutId == null) {
                runCatching {
                    kcalApi.startWorkout(
                        StartWorkoutRequest(
                            activity = activity,
                            weight_kg = weightKg,
                            age_years = ageYears,
                            sex = sex
                        )
                    )
                }.onSuccess { resp ->
                    workoutId = resp.workout_id
                    _currentWorkoutId.value = resp.workout_id
                    android.util.Log.d("KCAL", "Started workoutId=${resp.workout_id}")
                }.onFailure { e ->
                    android.util.Log.e("KCAL", "startWorkout FAILED", e)
                    return@launch // no id => can't poll
                }
            }

            // 2) start loop once
            if (kcalJob == null) {
                kcalJob = launch {
                    while (true) {
                        val id = workoutId ?: break
                        val st = state.value

                        if (!st.paused) {
                            val elapsed = elapsedSecProvider().toDouble()
                            val distanceKm = st.distanceMeters / 1000.0

                            runCatching {
                                kcalApi.updateWorkout(
                                    workoutId = id,
                                    body = UpdateWorkoutRequest(
                                        elapsed_sec = elapsed,
                                        distance_km = distanceKm
                                    )
                                )
                            }.onSuccess { resp ->
                                _kcal.value = resp.current_kcal
                                android.util.Log.d("KCAL", "kcal=${resp.current_kcal}")
                            }.onFailure { e ->
                                android.util.Log.e("KCAL", "updateWorkout FAILED", e)
                            }
                        }

                        delay(intervalMs)
                    }
                }
            }
        }
    }

    fun stopKcalLoop() {
        kcalJob?.cancel()
        kcalJob = null
        workoutId = null
        _kcal.value = 0.0
    }


    fun pingServer() {
        viewModelScope.launch {
            runCatching { kcalApi.health() }
                .onSuccess { resp ->
                    android.util.Log.d("KCAL", "HEALTH OK: $resp")
                }
                .onFailure { e ->
                    android.util.Log.e("KCAL", "HEALTH FAILED", e)
                }
        }
    }


    fun start() {
        _state.value = WorkoutMapState(
            paused = false,
            startPoint = null,
            segments = listOf(TrackSegment(dashed = false)),
            distanceMeters = 0.0
        )
        last = null
        resume()
    }


    fun pause() {
        _state.update { st ->
            if (st.paused) st
            else {
                val lastPoint = st.segments.lastOrNull()?.points?.lastOrNull()
                val newSeg = TrackSegment(dashed = true, points = lastPoint?.let { listOf(it) } ?: emptyList())
                st.copy(paused = true, segments = st.segments + newSeg)
            }
        }
        last = null
    }

    fun resume() {
        _state.update { st ->
            if (!st.paused) st
            else {
                val lastPoint = st.segments.lastOrNull()?.points?.lastOrNull()
                val newSeg = TrackSegment(dashed = false, points = lastPoint?.let { listOf(it) } ?: emptyList())
                st.copy(paused = false, segments = st.segments + newSeg)
            }
        }
        last = null
    }

    fun ensureLocationUpdates() {
        if (locationJob != null) return

        locationJob = viewModelScope.launch {
            WorkoutLocationService.locations.collect { p ->
                addPoint(p)
            }
        }
    }

    private fun addPoint(p: LatLng) {
        val newPoint = GLatLng(p.lat, p.lon)

        _state.update { st ->
            val start = st.startPoint ?: newPoint

            val segs = st.segments.toMutableList()
            val lastSeg = segs.removeLastOrNull() ?: TrackSegment(dashed = st.paused)

            val updatedLast = lastSeg.copy(points = lastSeg.points + newPoint)
            segs.add(updatedLast)

            var newDist = st.distanceMeters
            if (!st.paused) {
                val prev = last
                if (prev != null) {
                    val d = distanceMeters(prev, p)
                    if (d in 0.5..50.0) newDist += d
                }
                last = p
            } else {
                // in pausa non accumulo distanza
                last = null
            }

            st.copy(
                startPoint = start,
                segments = segs,
                distanceMeters = newDist
            )
        }
    }

    private fun <T> MutableList<T>.removeLastOrNull(): T? =
        if (isEmpty()) null else removeAt(lastIndex)


    private fun distanceMeters(a: LatLng, b: LatLng): Double {
        val out = FloatArray(1)
        Location.distanceBetween(a.lat, a.lon, b.lat, b.lon, out)
        return out[0].toDouble()
    }
}
