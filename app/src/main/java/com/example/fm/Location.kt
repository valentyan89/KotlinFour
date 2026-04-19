package com.example.fm

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.os.Build
import android.os.Looper
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresPermission
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import java.io.IOException
import java.util.Locale

@Composable
fun LocationAddressScreen() {
    val context = LocalContext.current
    val locationClient = remember { LocationServices.getFusedLocationProviderClient(context) }

    var address by remember { mutableStateOf("Нажмите кнопку") }
    var coords by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
        val coarseGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true

        if (fineGranted || coarseGranted) {
            if (!hasAnyLocationPermission(context)) {
                errorMessage = "Разрешения на геолокацию не предоставлены"
                isLoading = false
                return@rememberLauncherForActivityResult
            }

            isLoading = true
            errorMessage = null

            getLocationAndAddress(context, locationClient) { loc, addr ->
                if (loc != null) {
                    coords = "Lat: %.6f\nLng: %.6f".format(loc.latitude, loc.longitude)
                    address = addr ?: "Адрес не удалось определить"
                } else {
                    errorMessage = "Не удалось получить местоположение"
                }
                isLoading = false
            }
        } else {
            errorMessage = "Разрешения на геолокацию не предоставлены"
            isLoading = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (isLoading) {
            CircularProgressIndicator()
            Spacer(Modifier.height(24.dp))
        }

        Text(
            text = address,
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(16.dp))

        Text(
            text = coords,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(Modifier.height(32.dp))

        errorMessage?.let { msg ->
            Text(
                text = msg,
                color = MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(16.dp))
        }

        Button(
            onClick = {
                isLoading = true
                errorMessage = null

                requestLocationPermissions(
                    context = context,
                    launcher = permissionLauncher,
                    locationClient = locationClient
                ) { loc, addr ->
                    if (loc != null) {
                        coords = "Lat: %.6f\nLng: %.6f".format(loc.latitude, loc.longitude)
                        address = addr ?: "Адрес не удалось определить"
                    } else {
                        errorMessage = "Не удалось получить ме��тоположение"
                    }
                    isLoading = false
                }
            },
            enabled = !isLoading
        ) {
            Text("Получить мой адрес")
        }
    }
}

private fun requestLocationPermissions(
    context: Context,
    launcher: ActivityResultLauncher<Array<String>>,
    locationClient: FusedLocationProviderClient,
    onResult: (Location?, String?) -> Unit
) {
    val needed = arrayOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION
    ).filter {
        ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED
    }.toTypedArray()

    if (needed.isEmpty()) {
        if (hasAnyLocationPermission(context)) {
            getLocationAndAddress(context, locationClient, onResult)
        } else {
            onResult(null, null)
        }
    } else {
        launcher.launch(needed)
    }
}


private fun getLocationAndAddress(
    context: Context,
    client: FusedLocationProviderClient,
    onResult: (Location?, String?) -> Unit
) {
    if (!hasAnyLocationPermission(context)) {
        onResult(null, null)
        return
    }

    try {
        client.lastLocation
            .addOnSuccessListener { location: Location? ->
                if (location != null) {
                    processGeocoding(context, location, onResult)
                } else {
                    requestFreshLocation(context, client, onResult)
                }
            }
            .addOnFailureListener { e ->
                Log.e("Location", "lastLocation failed", e)
                onResult(null, null)
            }
    } catch (e: SecurityException) {
        Log.w("Location", "SecurityException (permission revoked?)", e)
        onResult(null, null)
    }
}

private fun hasAnyLocationPermission(context: Context): Boolean {
    val fine = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.ACCESS_FINE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED

    val coarse = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.ACCESS_COARSE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED

    return fine || coarse
}

@RequiresPermission(anyOf = [
    Manifest.permission.ACCESS_FINE_LOCATION,
    Manifest.permission.ACCESS_COARSE_LOCATION
])
private fun requestFreshLocation(
    context: Context,
    client: FusedLocationProviderClient,
    onResult: (Location?, String?) -> Unit
) {
    if (!hasAnyLocationPermission(context)) {
        onResult(null, null)
        return
    }

    val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000)
        .setWaitForAccurateLocation(true)
        .setMaxUpdateAgeMillis(10000)
        .setMaxUpdates(1)
        .build()

    val locationCallback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            client.removeLocationUpdates(this)

            val freshLocation = result.lastLocation
            if (freshLocation != null) {
                processGeocoding(context, freshLocation, onResult)
            } else {
                onResult(null, null)
            }
        }
    }

    try {
        client.requestLocationUpdates(
            locationRequest,
            locationCallback,
            Looper.getMainLooper()
        ).addOnFailureListener { e ->
            Log.e("Location", "requestLocationUpdates failed", e)
            client.removeLocationUpdates(locationCallback)
            onResult(null, null)
        }
    } catch (e: SecurityException) {
        Log.w("Location", "SecurityException in requestLocationUpdates", e)
        onResult(null, null)
    } catch (e: Exception) {
        Log.e("Location", "requestLocationUpdates failed", e)
        onResult(null, null)
    }
}

private fun processGeocoding(
    context: Context,
    location: Location,
    onResult: (Location, String?) -> Unit
) {
    if (!Geocoder.isPresent()) {
        onResult(location, null)
        return
    }

    val geocoder = Geocoder(context, Locale.getDefault())

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        geocoder.getFromLocation(
            location.latitude,
            location.longitude,
            1,
            object : Geocoder.GeocodeListener {
                override fun onGeocode(addresses: List<Address>) {
                    val addressText = addresses.firstOrNull()?.let { buildAddressString(it) }
                    onResult(location, addressText ?: "Адрес не найден")
                }

                override fun onError(errorMessage: String?) {
                    Log.w("Geocoder", "Error: $errorMessage")
                    onResult(location, null)
                }
            }
        )
    } else {
        @Suppress("DEPRECATION")
        try {
            val addresses = geocoder.getFromLocation(location.latitude, location.longitude, 1)
            val addressText = addresses?.firstOrNull()?.let { buildAddressString(it) }
            onResult(location, addressText ?: "Адрес не найден")
        } catch (e: IOException) {
            Log.e("Geocoder", "getFromLocation IO failed (no internet?)", e)
            onResult(location, null)
        } catch (e: Exception) {
            Log.e("Geocoder", "getFromLocation failed", e)
            onResult(location, null)
        }
    }
}

private fun buildAddressString(address: Address): String = buildString {
    address.getAddressLine(0)?.let { line ->
        if (line.isNotBlank()) append(line)
    }
    if (!address.locality.isNullOrBlank()) {
        if (isNotEmpty()) append(", ")
        append(address.locality)
    }
    if (!address.adminArea.isNullOrBlank()) {
        if (isNotEmpty()) append(", ")
        append(address.adminArea)
    }
    if (!address.countryName.isNullOrBlank()) {
        if (isNotEmpty()) append(", ")
        append(address.countryName)
    }
    if (isEmpty()) append("Адрес не распознан")
}