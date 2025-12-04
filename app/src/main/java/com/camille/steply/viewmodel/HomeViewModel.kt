package com.camille.steply.viewmodel

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class HomeUiState(
    val steps: Int = 0,
    val isTracking: Boolean = false
)

class HomeViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
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
