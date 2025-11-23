package com.bisonnotesai.android.location

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.location.Location
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.IOException
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Location data model
 */
data class LocationData(
    val latitude: Double,
    val longitude: Double,
    val altitude: Double?,
    val accuracy: Float?,
    val address: String?,
    val city: String?,
    val state: String?,
    val country: String?,
    val timestamp: Date
)

/**
 * Location manager using FusedLocationProviderClient
 * Phase 6: Location Services Implementation
 */
@Singleton
class LocationManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)

    private val geocoder: Geocoder = Geocoder(context, Locale.getDefault())

    /**
     * Check if location permission is granted
     */
    fun hasLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Get current location
     * @return LocationData or null if location cannot be obtained
     */
    suspend fun getCurrentLocation(): LocationData? {
        if (!hasLocationPermission()) {
            return null
        }

        return try {
            val location = getCurrentLocationInternal()
            location?.let { loc ->
                val address = getAddressFromLocation(loc.latitude, loc.longitude)
                LocationData(
                    latitude = loc.latitude,
                    longitude = loc.longitude,
                    altitude = loc.altitude,
                    accuracy = loc.accuracy,
                    address = address?.getAddressLine(0),
                    city = address?.locality,
                    state = address?.adminArea,
                    country = address?.countryName,
                    timestamp = Date(loc.time)
                )
            }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Get location without reverse geocoding (faster)
     */
    suspend fun getCurrentLocationFast(): LocationData? {
        if (!hasLocationPermission()) {
            return null
        }

        return try {
            val location = getCurrentLocationInternal()
            location?.let { loc ->
                LocationData(
                    latitude = loc.latitude,
                    longitude = loc.longitude,
                    altitude = loc.altitude,
                    accuracy = loc.accuracy,
                    address = null,
                    city = null,
                    state = null,
                    country = null,
                    timestamp = Date(loc.time)
                )
            }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Reverse geocode coordinates to address
     */
    private fun getAddressFromLocation(latitude: Double, longitude: Double): Address? {
        return try {
            @Suppress("DEPRECATION")
            val addresses = geocoder.getFromLocation(latitude, longitude, 1)
            addresses?.firstOrNull()
        } catch (e: IOException) {
            null
        }
    }

    /**
     * Get current location using FusedLocationProviderClient
     */
    private suspend fun getCurrentLocationInternal(): Location? =
        suspendCancellableCoroutine { continuation ->
            try {
                val cancellationToken = CancellationTokenSource()

                continuation.invokeOnCancellation {
                    cancellationToken.cancel()
                }

                fusedLocationClient.getCurrentLocation(
                    Priority.PRIORITY_HIGH_ACCURACY,
                    cancellationToken.token
                ).addOnSuccessListener { location ->
                    continuation.resume(location)
                }.addOnFailureListener { exception ->
                    continuation.resumeWithException(exception)
                }
            } catch (e: SecurityException) {
                continuation.resumeWithException(e)
            }
        }

    /**
     * Format location for display
     */
    fun formatLocation(location: LocationData): String {
        return buildString {
            location.address?.let { append(it) }
                ?: run {
                    location.city?.let { append(it) }
                    location.state?.let {
                        if (isNotEmpty()) append(", ")
                        append(it)
                    }
                    location.country?.let {
                        if (isNotEmpty()) append(", ")
                        append(it)
                    }
                }

            if (isEmpty()) {
                append(String.format("%.6f, %.6f", location.latitude, location.longitude))
            }
        }
    }
}
