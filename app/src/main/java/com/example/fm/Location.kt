package com.example.fm

import android.Manifest
import android.content.Context
import android.location.LocationManager
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.os.Build
import android.os.Handler
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

    var address by remember { mutableStateOf("нажмите кнопку") }
    var cords by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions.all { it.value }) {
            isLoading = true
            errorMessage = null

            getLocationAndAddress(context, locationClient) { loc, addr ->
                if (loc != null) {
                    cords = "lat: %.6f\nlon: %.6f".format(loc.latitude, loc.longitude)
                    address = addr ?: "адрес не определен"
                }
                else {
                    errorMessage = "Не удалось определить местоположение, проверьте GPS и интернет"
                }
                isLoading = false
            }
        }
        else {
            errorMessage = "Доступ к геолокации не предоставлен"
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
            text = cords,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(Modifier.height(28.dp))

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

                if (!isLocationEnabled(context)) {
                    isLoading = false
                    errorMessage = "гео выключена, включите"
                    return@Button
                }

                requestLocationPermissions(
                    context = context,
                    launcher = permissionLauncher,
                    locationClient = locationClient
                ) { loc, addr ->
                    if (loc != null) {
                        cords = "lat: %.6f\nlon: %.6f".format(loc.latitude, loc.longitude)
                        address = addr ?: "нет интернета"
                    }
                    else {
                        errorMessage = "не получилось найти"
                    }
                    isLoading = false
                }
            },
            enabled = !isLoading
        ) {
            Text("get address")
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
    ).filter { ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED}.toTypedArray()

    if (needed.isNotEmpty()) {
        launcher.launch(needed)
    }
    else {
        getLocationAndAddress(context, locationClient, onResult)
    }
}

@RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
private fun getLocationAndAddress(
    context: Context,
    client: FusedLocationProviderClient,
    onResult: (Location?, String?) -> Unit
) {
    try {
        client.lastLocation
            .addOnSuccessListener { location: Location? ->
                if (location != null) {
                    processGeocoding(context, location, onResult)
                }
                else {
                    requestFreshLocation(context, client, onResult)
                }
            }
            .addOnFailureListener { e ->
                onResult(null, null)
            }
    } catch (e: SecurityException) {
        onResult(null, null)
    }
}

@RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
private fun requestFreshLocation(
    context: Context,
    client: FusedLocationProviderClient,
    onResult: (Location?, String?) -> Unit
) {
    val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000)
        .setWaitForAccurateLocation(true)
        .setMaxUpdateAgeMillis(10000)
        .setMaxUpdates(1)
        .build()

    lateinit var timeoutHandler: Handler
    lateinit var timeoutRunnable: Runnable

    val locationCallback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            timeoutHandler.removeCallbacks(timeoutRunnable)
            client.removeLocationUpdates(this)

            client.removeLocationUpdates(this)
            val freshLocation = result.lastLocation
            if (freshLocation != null) {
                processGeocoding(context, freshLocation, onResult)
            }
            else {
                onResult(null, null)
            }
        }
    }

    timeoutHandler = Handler(Looper.getMainLooper())
    timeoutRunnable = Runnable {
        client.removeLocationUpdates(locationCallback)
        onResult(null, null)
    }

    timeoutHandler.postDelayed(timeoutRunnable, 12000)

    try {
        client.requestLocationUpdates(
            locationRequest,
            locationCallback,
            Looper.getMainLooper()
        ).addOnFailureListener { e ->
            timeoutHandler.removeCallbacks(timeoutRunnable)
            Log.e("Location", "requestLocationUpdates failed", e)
            client.removeLocationUpdates(locationCallback)
            onResult(null, null)
        }
    }
    catch (e: SecurityException) {
        timeoutHandler.removeCallbacks(timeoutRunnable)
        Log.w("Location", "SecurityException in requestLocationUpdates", e)
        onResult(null, null)
    }
    catch (e: Exception) {
        timeoutHandler.removeCallbacks(timeoutRunnable)
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

    var delivered = false
    fun deliverOnce(addr: String?) {
        if (delivered) return
        delivered = true
        onResult(location, addr)
    }

    val h = Handler(Looper.getMainLooper())
    val timeout = Runnable { deliverOnce(null) }
    h.postDelayed(timeout, 8000)

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        geocoder.getFromLocation(
            location.latitude,
            location.longitude,
            1,
            object : Geocoder.GeocodeListener {
                override fun onGeocode(addresses: List<Address>) {
                    h.removeCallbacks(timeout)
                    val text = addresses.firstOrNull()?.let { buildAddressString(it) }
                    deliverOnce(text)
                }

                override fun onError(errorMessage: String?) {
                    h.removeCallbacks(timeout)
                    deliverOnce(null)
                }
            }
        )
    }
    else {
        @Suppress("DEPRECATION")
        try {
            val addresses = geocoder.getFromLocation(location.latitude, location.longitude, 1)
            h.removeCallbacks(timeout)
            val text = addresses?.firstOrNull()?.let { buildAddressString(it) }
            deliverOnce(text)
        } catch (e: Exception) {
            h.removeCallbacks(timeout)
            deliverOnce(null)
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
    if (isEmpty()) append("адрес не распознан")
}


private fun isLocationEnabled(context: Context): Boolean {
    val lm = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    return lm.isProviderEnabled(LocationManager.GPS_PROVIDER) || lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
}