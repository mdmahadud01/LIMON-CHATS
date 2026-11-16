package com.example.media

import android.annotation.SuppressLint
import android.content.Context
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.os.Build
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.Locale
import kotlin.coroutines.resume

object LocationHelper {
    @SuppressLint("MissingPermission")
    suspend fun getCurrentLocation(context: Context): Result<Pair<Double, Double>> = runCatching {
        val client = LocationServices.getFusedLocationProviderClient(context)
        val cts = CancellationTokenSource()
        val loc: Location? = client.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, cts.token).await()
            ?: client.lastLocation.await()
        requireNotNull(loc) { "Location unavailable right now. Make sure GPS/location is turned on." }
        loc.latitude to loc.longitude
    }

    suspend fun resolvePlaceName(context: Context, latitude: Double, longitude: Double): String? {
        if (!Geocoder.isPresent()) return null
        val geocoder = Geocoder(context, Locale.getDefault())
        return runCatching {
            val address: Address? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                suspendCancellableCoroutine { cont ->
                    geocoder.getFromLocation(latitude, longitude, 1) { list ->
                        if (cont.isActive) cont.resume(list.firstOrNull())
                    }
                }
            } else {
                withContext(Dispatchers.IO) {
                    @Suppress("DEPRECATION")
                    geocoder.getFromLocation(latitude, longitude, 1)?.firstOrNull()
                }
            }
            address?.let { addr ->
                val parts = listOfNotNull(
                    addr.thoroughfare ?: addr.featureName,
                    addr.locality ?: addr.subAdminArea,
                    addr.adminArea
                ).distinct().filter { it.isNotBlank() }
                parts.take(2).joinToString(", ").ifBlank { addr.countryName }
            }?.takeIf { it.isNotBlank() }
        }.getOrNull()
    }
}
