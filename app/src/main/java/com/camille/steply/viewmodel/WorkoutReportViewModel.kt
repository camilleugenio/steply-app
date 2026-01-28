package com.camille.steply.viewmodel

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

@Parcelize @Serializable
data class LatLngP(val lat: Double, val lon: Double) : Parcelable

@Parcelize @Serializable
data class TrackSegmentP(val dashed: Boolean, val points: List<LatLngP>) : Parcelable

@Parcelize @Serializable
data class WorkoutReportSnapshot(
    val idAllenamento: String? = null,
    val type: String,
    val startTimeMs: Long,
    val durationSec: Int,
    val distanceMeters: Double,
    val kcal: Int,
    val meteoEmoji: String,
    val meteoTempC: String,
    val startPoint: LatLngP?,
    val segments: List<TrackSegmentP>,
    val photoUri: String? = null
) : Parcelable



data class WorkoutReportUiState(
    val snapshot: WorkoutReportSnapshot? = null,
    val fromHistory: Boolean = false,
    val workoutPhotoUri: String? = null,
    val showPhotoPreview: Boolean = false
)

sealed class WorkoutReportEffect {
    data object RequestCameraPermission : WorkoutReportEffect()
    data object LaunchCamera : WorkoutReportEffect()
    data object NavigateBackToActivity : WorkoutReportEffect()
}

class WorkoutReportViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(WorkoutReportUiState())
    val uiState: StateFlow<WorkoutReportUiState> = _uiState

    private val _effect = MutableStateFlow<WorkoutReportEffect?>(null)
    val effect: StateFlow<WorkoutReportEffect?> = _effect

    fun consumeEffect() {
        _effect.value = null
    }

    fun init(snapshot: WorkoutReportSnapshot, fromHistory: Boolean) {
        _uiState.update {
            it.copy(
                snapshot = snapshot,
                fromHistory = fromHistory,
                workoutPhotoUri = snapshot.photoUri
            )
        }
    }

    fun onCloseClicked() {
        _effect.value = WorkoutReportEffect.NavigateBackToActivity
    }

    fun onPolaroidClicked(canTakePhoto: Boolean) {
        val uri = uiState.value.workoutPhotoUri
        when {
            uri != null -> _uiState.update { it.copy(showPhotoPreview = true) }
            canTakePhoto -> _effect.value = WorkoutReportEffect.LaunchCamera
        }
    }

    fun onDismissPreview() {
        _uiState.update { it.copy(showPhotoPreview = false) }
    }

    fun onRetakePhotoClicked() {
        _uiState.update { it.copy(showPhotoPreview = false) }
        _effect.value = WorkoutReportEffect.LaunchCamera
    }

    fun onCameraPermissionNeeded() {
        _effect.value = WorkoutReportEffect.RequestCameraPermission
    }

    fun onPhotoCaptured(uriString: String?) {
        _uiState.update { it.copy(workoutPhotoUri = uriString) }
    }
}
