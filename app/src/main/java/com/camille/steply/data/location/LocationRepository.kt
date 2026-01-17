package com.camille.steply.data.location

import android.content.Context
import android.location.Geocoder
import android.os.Build
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.Priority
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.IOException
import java.util.Locale
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

data class LatLng(val lat: Double, val lon: Double)

class LocationRepository(
    private val context: Context,
    private val fused: FusedLocationProviderClient
) {

    suspend fun getCurrentLatLng(): LatLng =
        suspendCancellableCoroutine { cont ->
            try {
                fused.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                    .addOnSuccessListener { loc ->
                        if (loc != null) {
                            cont.resume(LatLng(loc.latitude, loc.longitude))
                        } else {
                            fused.lastLocation
                                .addOnSuccessListener { last ->
                                    if (last != null) cont.resume(LatLng(last.latitude, last.longitude))
                                    else cont.resumeWithException(
                                        IllegalStateException(
                                            "Location is null (no current/last). On emulator: Extended Controls → Location → Send."
                                        )
                                    )
                                }
                                .addOnFailureListener { e -> cont.resumeWithException(e) }
                        }
                    }
                    .addOnFailureListener { e -> cont.resumeWithException(e) }
            } catch (se: SecurityException) {
                cont.resumeWithException(se)
            }
        }

    suspend fun getPlaceName(lat: Double, lon: Double): String =
        suspendCancellableCoroutine { cont ->
            try {
                val geocoder = Geocoder(context, Locale.getDefault())

                fun pickName(a: android.location.Address?): String =
                    a?.locality
                        ?: a?.subAdminArea
                        ?: a?.adminArea
                        ?: a?.countryName
                        ?: "Unknown place"

                if (Build.VERSION.SDK_INT >= 33) {
                    geocoder.getFromLocation(lat, lon, 1) { list ->
                        cont.resume(pickName(list.firstOrNull()))
                    }
                } else {
                    @Suppress("DEPRECATION")
                    val list = geocoder.getFromLocation(lat, lon, 1)
                    cont.resume(pickName(list?.firstOrNull()))
                }
            } catch (io: IOException) {
                cont.resume("Unknown place")
            } catch (e: Exception) {
                cont.resumeWithException(e)
            }
        }

    fun locationUpdates(
        intervalMs: Long = 500L,
        fastestMs: Long = 250L,
        minDistanceMeters: Float = 0f
    ): Flow<LatLng> = callbackFlow {

        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, intervalMs)
            .setMinUpdateIntervalMillis(fastestMs)
            .setMinUpdateDistanceMeters(minDistanceMeters)
            .build()

        val callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                val loc = result.lastLocation ?: return
                trySend(LatLng(loc.latitude, loc.longitude))
            }
        }

        try {
            fused.requestLocationUpdates(request, callback, context.mainLooper)
        } catch (se: SecurityException) {
            close(se)
        }

        awaitClose { fused.removeLocationUpdates(callback) }
    }
}
