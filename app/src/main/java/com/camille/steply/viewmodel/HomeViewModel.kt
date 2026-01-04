package com.camille.steply.viewmodel

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.LocalDate
import java.time.format.DateTimeFormatter

data class HomeUiState(
    val steps: Int = 0,
    val isTracking: Boolean = false,
    val currentDate: String = "",
    val currentDayname: String = ""
)

class HomeViewModel : ViewModel() {

    private val dateFormatter = DateTimeFormatter.ofPattern("d MMMM")
    private val dayFormatter = DateTimeFormatter.ofPattern("EEEE")


    private val _uiState = MutableStateFlow(
        HomeUiState(
            currentDate = LocalDate.now().format(dateFormatter),
            currentDayname = LocalDate.now().format(dayFormatter)
        )
    )
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

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
