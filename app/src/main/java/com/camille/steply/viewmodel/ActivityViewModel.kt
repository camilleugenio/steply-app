package com.camille.steply.viewmodel

import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class WorkoutType { WALK, RUN, CYCLING }

fun workoutColor(type: WorkoutType): Color = when (type) {
    WorkoutType.WALK -> Color(0xFF2F80FF)   // blu
    WorkoutType.RUN -> Color(0xFF9B51E0)    // viola
    WorkoutType.CYCLING -> Color(0xFF27AE60)// verde
}

data class ActivityUiState(
    val countdownType: WorkoutType? = null,
    val secondsLeft: Int = 3,
    val phaseText: String = "Ready",
    val isCountingDown: Boolean = false
)

class ActivityViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(ActivityUiState())
    val uiState: StateFlow<ActivityUiState> = _uiState.asStateFlow()

    private var countdownJob: Job? = null

    fun startCountdown(type: WorkoutType) {
        // se clicchi due volte velocemente, resetta
        countdownJob?.cancel()

        countdownJob = viewModelScope.launch {
            _uiState.update {
                it.copy(
                    countdownType = type,
                    secondsLeft = 3,
                    phaseText = "Ready",
                    isCountingDown = true
                )
            }

            delay(900)
            _uiState.update { it.copy(secondsLeft = 2, phaseText = "Set") }

            delay(900)
            _uiState.update { it.copy(secondsLeft = 1, phaseText = "Go") }

            delay(650)
            _uiState.update {
                it.copy(
                    countdownType = null,
                    isCountingDown = false,
                    secondsLeft = 3,
                    phaseText = "Ready"
                )
            }
        }
    }

    fun cancelCountdown() {
        countdownJob?.cancel()
        _uiState.update {
            it.copy(
                countdownType = null,
                isCountingDown = false,
                secondsLeft = 3,
                phaseText = "Ready"
            )
        }
    }
}
