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
    val segments: List<TrackSegment> = listOf(TrackSegment(dashed = false)),
    val distanceMeters: Double = 0.0
)

data class TrackSegment(
    val dashed: Boolean,
    val points: List<GLatLng> = emptyList()
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
            segments = listOf(TrackSegment(dashed = false)),
            distanceMeters = 0.0
        )
        last = null
        resume() // vedi sotto: resume non deve più cancellare/ricreare job
    }

    fun pause() {
        _state.update { st ->
            if (st.paused) st
            else {
                // ✅ quando vai in pausa, apri un NUOVO segmento tratteggiato
                val lastPoint = st.segments.lastOrNull()?.points?.lastOrNull()
                val newSeg = TrackSegment(dashed = true, points = lastPoint?.let { listOf(it) } ?: emptyList())
                st.copy(paused = true, segments = st.segments + newSeg)
            }
        }
        last = null // ✅ così non sommi distanza durante pausa
    }

    fun resume() {
        _state.update { st ->
            if (!st.paused) st
            else {
                // ✅ quando riprendi, apri un NUOVO segmento pieno
                val lastPoint = st.segments.lastOrNull()?.points?.lastOrNull()
                val newSeg = TrackSegment(dashed = false, points = lastPoint?.let { listOf(it) } ?: emptyList())
                st.copy(paused = false, segments = st.segments + newSeg)
            }
        }
        last = null // ✅ riparti a misurare da zero dal primo punto dopo resume
    }

    fun ensureLocationUpdates() {
        if (locationJob != null) return

        locationJob = viewModelScope.launch {
            locationRepository.locationUpdates().collect { p ->
                addPoint(p)
            }
        }
    }

    private fun addPoint(p: LatLng) {
        val newPoint = GLatLng(p.lat, p.lon)

        _state.update { st ->
            // ✅ set start solo la prima volta
            val start = st.startPoint ?: newPoint

            // ✅ aggiungo il punto all’ULTIMO segmento
            val segs = st.segments.toMutableList()
            val lastSeg = segs.removeLastOrNull() ?: TrackSegment(dashed = st.paused)

            val updatedLast = lastSeg.copy(points = lastSeg.points + newPoint)
            segs.add(updatedLast)

            // ✅ distanza: SOLO se NON in pausa
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
