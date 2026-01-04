package com.camille.steply.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import com.camille.steply.data.location.LocationRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.time.LocalDate
import java.time.format.DateTimeFormatter

data class HomeUiState(
    val steps: Int = 0,
    val isTracking: Boolean = false,
    val currentDate: String = "",
    val currentDayname: String = "",
    val currentPlacename: String = "-  ",
    val locationLoading: Boolean = false,
    val locationError: String? = null
)

class HomeViewModel (
    private val locationRepository: LocationRepository
) : ViewModel() {


    private val dateFormatter = DateTimeFormatter.ofPattern("d MMMM")
    private val dayFormatter = DateTimeFormatter.ofPattern("EEEE")




    private val _uiState = MutableStateFlow(
        HomeUiState(
            currentDate = LocalDate.now().format(dateFormatter),
            currentDayname = LocalDate.now().format(dayFormatter)
        )
    )
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    fun refreshPlace() {
        viewModelScope.launch {

            _uiState.update {
                it.copy(
                    locationLoading = true,
                    locationError = null
                )
            }

            try {
                val latLng = locationRepository.getCurrentLatLng()
                val place = locationRepository.getPlaceName(latLng.lat, latLng.lon)

                _uiState.update {
                    it.copy(
                        currentPlacename = place,
                        locationLoading = false
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        currentPlacename = "-",
                        locationLoading = false,
                        locationError = e.message?: "Location failed"
                    )
                }
            }
        }
    }

    fun addTestStep() {
        _uiState.value = _uiState.value.copy(
            steps = _uiState.value.steps + 1
        )
    }

    fun resetSteps() {
        _uiState.value = _uiState.value.copy(steps = 0)
    }

    fun startTracking() {
        _uiState.value = _uiState.value.copy(isTracking = true)
    }

    fun stopTracking() {
        _uiState.value = _uiState.value.copy(isTracking = false)
    }
}
