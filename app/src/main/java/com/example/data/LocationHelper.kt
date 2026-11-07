package com.example.data

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.os.Build
import android.os.Looper
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.IOException
import java.util.Locale
import kotlin.coroutines.resume

data class UserLocation(
    val latitude: Double,
    val longitude: Double,
    val address: String,
    val accuracy: Float = 0f,
    val timestamp: Long = System.currentTimeMillis()
)

class LocationHelper(private val context: Context) {
    private val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)

    // Default reference coordinates in Vietnam (Hoàn Kiếm, Hà Nội)
    companion object {
        const val DEFAULT_LATITUDE = 21.028511
        const val DEFAULT_LONGITUDE = 105.854167
        const val DEFAULT_ADDRESS = "32 Đường Láng, Đống Đa, Hà Nội"
    }

    /**
     * Check if coarse or fine location permissions are granted
     */
    fun hasLocationPermission(): Boolean {
        val fineLocation = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val coarseLocation = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        return fineLocation || coarseLocation
    }

    /**
     * Get current single location fix via FusedLocationProviderClient
     */
    @SuppressLint("MissingPermission")
    suspend fun getCurrentLocation(): UserLocation = suspendCancellableCoroutine { continuation ->
        if (!hasLocationPermission()) {
            continuation.resume(
                UserLocation(
                    latitude = DEFAULT_LATITUDE,
                    longitude = DEFAULT_LONGITUDE,
                    address = DEFAULT_ADDRESS
                )
            )
            return@suspendCancellableCoroutine
        }

        val cts = CancellationTokenSource()
        continuation.invokeOnCancellation {
            cts.cancel()
        }

        fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cts.token)
            .addOnSuccessListener { location: Location? ->
                if (location != null) {
                    val addressStr = reverseGeocode(location.latitude, location.longitude)
                    continuation.resume(
                        UserLocation(
                            latitude = location.latitude,
                            longitude = location.longitude,
                            address = addressStr,
                            accuracy = location.accuracy,
                            timestamp = location.time
                        )
                    )
                } else {
                    // Try lastLocation as fallback
                    fusedLocationClient.lastLocation.addOnSuccessListener { lastLoc: Location? ->
                        if (lastLoc != null) {
                            continuation.resume(
                                UserLocation(
                                    latitude = lastLoc.latitude,
                                    longitude = lastLoc.longitude,
                                    address = reverseGeocode(lastLoc.latitude, lastLoc.longitude),
                                    accuracy = lastLoc.accuracy,
                                    timestamp = lastLoc.time
                                )
                            )
                        } else {
                            continuation.resume(
                                UserLocation(
                                    latitude = DEFAULT_LATITUDE,
                                    longitude = DEFAULT_LONGITUDE,
                                    address = DEFAULT_ADDRESS
                                )
                            )
                        }
                    }.addOnFailureListener {
                        continuation.resume(
                            UserLocation(
                                latitude = DEFAULT_LATITUDE,
                                longitude = DEFAULT_LONGITUDE,
                                address = DEFAULT_ADDRESS
                            )
                        )
                    }
                }
            }
            .addOnFailureListener {
                continuation.resume(
                    UserLocation(
                        latitude = DEFAULT_LATITUDE,
                        longitude = DEFAULT_LONGITUDE,
                        address = DEFAULT_ADDRESS
                    )
                )
            }
    }

    /**
     * Emits continuous location updates as a Kotlin Coroutine Flow
     * Ideal for tracking live coordinates during ride-hailing
     */
    @SuppressLint("MissingPermission")
    fun getLocationUpdates(intervalMs: Long = 5000L): Flow<UserLocation> = callbackFlow {
        if (!hasLocationPermission()) {
            trySend(
                UserLocation(
                    latitude = DEFAULT_LATITUDE,
                    longitude = DEFAULT_LONGITUDE,
                    address = DEFAULT_ADDRESS
                )
            )
            close()
            return@callbackFlow
        }

        val locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            intervalMs
        ).apply {
            setMinUpdateIntervalMillis(intervalMs / 2)
            setWaitForAccurateLocation(false)
        }.build()

        val locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                val loc = result.lastLocation ?: return
                val addressStr = reverseGeocode(loc.latitude, loc.longitude)
                trySend(
                    UserLocation(
                        latitude = loc.latitude,
                        longitude = loc.longitude,
                        address = addressStr,
                        accuracy = loc.accuracy,
                        timestamp = loc.time
                    )
                )
            }
        }

        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            locationCallback,
            Looper.getMainLooper()
        )

        awaitClose {
            fusedLocationClient.removeLocationUpdates(locationCallback)
        }
    }

    /**
     * Converts coordinates into a readable address in Vietnam
     */
    fun reverseGeocode(lat: Double, lng: Double): String {
        return try {
            val geocoder = Geocoder(context, Locale.forLanguageTag("vi-VN"))
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                // Synchronous fallback or formatted string
                val results = geocoder.getFromLocation(lat, lng, 1)
                formatAddress(results?.firstOrNull(), lat, lng)
            } else {
                @Suppress("DEPRECATION")
                val results = geocoder.getFromLocation(lat, lng, 1)
                formatAddress(results?.firstOrNull(), lat, lng)
            }
        } catch (e: Exception) {
            String.format(Locale.US, "Vị trí GPS (%.4f, %.4f)", lat, lng)
        }
    }

    private fun formatAddress(addr: Address?, lat: Double, lng: Double): String {
        if (addr == null) {
            return String.format(Locale.US, "Vị trí GPS (%.4f, %.4f)", lat, lng)
        }
        val parts = mutableListOf<String>()
        addr.featureName?.let { if (it.isNotBlank()) parts.add(it) }
        addr.thoroughfare?.let { if (it.isNotBlank() && !parts.contains(it)) parts.add(it) }
        addr.subLocality?.let { if (it.isNotBlank()) parts.add(it) }
        addr.subAdminArea?.let { if (it.isNotBlank()) parts.add(it) }
        addr.adminArea?.let { if (it.isNotBlank()) parts.add(it) }

        return if (parts.isNotEmpty()) {
            parts.take(3).joinToString(", ")
        } else {
            String.format(Locale.US, "Vị trí (%.4f, %.4f)", lat, lng)
        }
    }

    /**
     * Calculate geodesic distance in kilometers between two points
     */
    fun calculateDistanceKm(
        lat1: Double,
        lon1: Double,
        lat2: Double,
        lon2: Double
    ): Double {
        val results = FloatArray(1)
        Location.distanceBetween(lat1, lon1, lat2, lon2, results)
        return (results[0] / 1000.0).coerceAtLeast(0.5)
    }
}
