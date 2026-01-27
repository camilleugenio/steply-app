package com.camille.steply.viewmodel

import androidx.compose.ui.graphics.Color


// Questo file definisce una volta per tutte quali sport esistono nell'app
enum class WorkoutType {
    WALK,
    RUN,
    CYCLING
}

fun workoutColor(type: WorkoutType): Color {
    return when (type) {
        WorkoutType.WALK -> Color(0xFF2F80FF)    // Blu
        WorkoutType.RUN -> Color(0xFF9B51E0)     // Viola
        WorkoutType.CYCLING -> Color(0xFF27AE60) // Verde
    }
}