package com.camille.steply.viewmodel

import androidx.compose.ui.graphics.Color

enum class WorkoutType {
    WALK,
    RUN,
    CYCLING
}

fun workoutColor(type: WorkoutType): Color {
    return when (type) {
        WorkoutType.WALK -> Color(0xFF2F80FF)
        WorkoutType.RUN -> Color(0xFF9B51E0)
        WorkoutType.CYCLING -> Color(0xFF27AE60)
    }
}