package com.camille.steply.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.camille.steply.data.history.WorkoutHistoryRepository
import com.camille.steply.pages.WorkoutReportSnapshot
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class WorkoutHistoryUiState(
    val isLoading: Boolean = true,
    val items: List<WorkoutReportSnapshot> = emptyList()
)

class WorkoutHistoryViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = WorkoutHistoryRepository(app.applicationContext)

    private val _uiState = MutableStateFlow(WorkoutHistoryUiState())
    val uiState: StateFlow<WorkoutHistoryUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repo.history.collect { list ->
                _uiState.value = WorkoutHistoryUiState(
                    isLoading = false,
                    items = list
                )
            }
        }
    }

    fun addWorkout(snapshot: WorkoutReportSnapshot) {
        viewModelScope.launch { repo.add(snapshot) }
    }
}


