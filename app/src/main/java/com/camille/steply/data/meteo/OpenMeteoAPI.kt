@file:OptIn(kotlinx.serialization.InternalSerializationApi::class)
package com.camille.steply.data.meteo

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import retrofit2.Retrofit
import retrofit2.http.GET
import retrofit2.http.Query
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory


// ---------- DTOs ----------
@Serializable
data class OpenMeteoResponse(
    @SerialName("current")
    val current: OpenMeteoCurrent? = null
)

@Serializable
data class OpenMeteoCurrent(
    @SerialName("temperature_2m")
    val temperature2m: Double? = null,

    @SerialName("weather_code")
    val weatherCode: Int? = null
)

// ---------- Retrofit service ----------
interface OpenMeteoService {
    @GET("v1/forecast")
    suspend fun getCurrent(
        @Query("latitude") lat: Double,
        @Query("longitude") lon: Double,

        // ⬇️ only what you need
        @Query("current")
        current: String = "temperature_2m,weather_code",

        @Query("temperature_unit") temperatureUnit: String = "celsius",
        @Query("timezone") timezone: String = "auto"
    ): OpenMeteoResponse
}

// ---------- Client ----------
object OpenMeteoApi {

    private val json = Json {
        ignoreUnknownKeys = true
    }

    val service: OpenMeteoService by lazy {
        Retrofit.Builder()
            .baseUrl("https://api.open-meteo.com/")
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(OpenMeteoService::class.java)
    }
}

// ---------- Weather ----------

fun openMeteoCodeToEmoji(code: Int): String = when (code) {
    0 -> "☀️"
    1, 2, 3 -> "⛅️"          // nuvoloso
    45, 48 -> "🌫️"           // nebbia
    51, 53, 55 -> "🌦️"       // pioviggine
    56, 57 -> "🧊🌦️"         // pioviggine gelata
    61, 63, 65 -> "🌧️"       // pioggia
    66, 67 -> "🧊🌧️"         // pioggia gelata
    71, 73, 75 -> "❄️"       // neve
    77 -> "❄️"               // neve
    80, 81, 82 -> "🌧️"       // rovesci
    85, 86 -> "🌨️"           // rovesci di neve
    95 -> "⛈️"               // temporale
    96, 99 -> "⛈️🧊"         // temporale + grandine
    else -> "❓"
}

