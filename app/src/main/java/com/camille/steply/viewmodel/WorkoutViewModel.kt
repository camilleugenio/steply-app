package com.camille.steply.viewmodel

import android.location.Location
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.camille.steply.data.location.LatLng
import com.camille.steply.data.location.LocationRepository
import com.google.android.gms.maps.model.LatLng as GLatLng
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class WorkoutMapState(
    val paused: Boolean = false,
    val startPoint: GLatLng? = null,
    val points: List<GLatLng> = emptyList(),
    val distanceMeters: Double = 0.0
)

class WorkoutViewModel(
    private val locationRepository: LocationRepository
) : ViewModel() {

    private val _state = MutableStateFlow(WorkoutMapState())
    val state: StateFlow<WorkoutMapState> = _state.asStateFlow()

    private var locationJob: Job? = null
    private var last: LatLng? = null

    fun start() {
        _state.value = WorkoutMapState(
            paused = false,
            startPoint = null,
            points = emptyList(),
            distanceMeters = 0.0
        )
        last = null
        resume()
    }

    fun pause() {
        _state.update { it.copy(paused = true) }
        locationJob?.cancel()
        locationJob = null
        last = null // importantissimo: evita linea che collega punti lontani dopo resume
    }

    fun resume() {
        _state.update { it.copy(paused = false) }
        locationJob?.cancel()
        locationJob = viewModelScope.launch {
            locationRepository.locationUpdates().collect { p ->
                addPoint(p)
            }
        }
    }

    private fun addPoint(p: LatLng) {
        val newPoint = GLatLng(p.lat, p.lon)

        _state.update { st ->
            // ✅ set start SOLO la prima volta
            val start = st.startPoint ?: newPoint
            st.copy(startPoint = start)
        }

        val prev = last
        if (prev != null) {
            val d = distanceMeters(prev, p)
            if (d in 0.5..50.0) {
                _state.update { it.copy(distanceMeters = it.distanceMeters + d) }
            }
        }

        last = p
        _state.update { it.copy(points = it.points + newPoint) }
    }


    private fun distanceMeters(a: LatLng, b: LatLng): Double {
        val out = FloatArray(1)
        Location.distanceBetween(a.lat, a.lon, b.lat, b.lon, out)
        return out[0].toDouble()
    }
}
