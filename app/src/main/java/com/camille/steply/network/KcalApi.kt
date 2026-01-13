package com.camille.steply.network

import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.GET
import retrofit2.http.Path

data class StartWorkoutRequest(
    val activity: String,     // "walk" | "run" | "cycle"
    val weight_kg: Double,
    val age_years: Int,
    val sex: String           // "male" | "female"
)

data class StartWorkoutResponse(
    val workout_id: String,
    val message: String? = null
)

data class UpdateWorkoutRequest(
    val elapsed_sec: Double,
    val distance_km: Double
)

data class UpdateWorkoutResponse(
    val workout_id: String,
    val current_kcal: Double,
    val details: Map<String, Any>? = null
)

interface KcalApi {
    @GET("health")
    suspend fun health(): Map<String, String>

    @POST("workouts/start")
    suspend fun startWorkout(@Body body: StartWorkoutRequest): StartWorkoutResponse

    @POST("workouts/{id}/update")
    suspend fun updateWorkout(
        @Path("id") workoutId: String,
        @Body body: UpdateWorkoutRequest
    ): UpdateWorkoutResponse
}