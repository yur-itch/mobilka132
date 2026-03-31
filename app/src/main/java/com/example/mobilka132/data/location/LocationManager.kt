package com.example.mobilka132.data.location

import android.content.pm.PackageManager
import android.widget.Toast
import androidx.compose.ui.geometry.Offset
import com.google.android.gms.location.LocationServices
import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.os.Looper
import androidx.activity.result.ActivityResultRegistry
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresPermission
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest.Builder
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.Priority


class LocationManager(private val activity: Context, private val registry: ActivityResultRegistry) {

    var mapLocation by mutableStateOf<Offset?>(null)
    var worldLocation : Location? = null
    private val fusedLocationClient by lazy {
        LocationServices.getFusedLocationProviderClient(activity)
    }

    private val requestPermissionLauncher = registry.register(
        "location_permission_key",
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            @SuppressLint("MissingPermission")
            getLastLocation()
        }
    }

    fun checkPermission() {
        if (ActivityCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            getLastLocation()
        } else {
            requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    private fun setMapLocation() {
        mapLocation = worldLocation?.let { l -> Offset(((l.longitude - 84.932881) * 122800).toFloat(), (-(l.latitude - 56.4758) * 222000).toFloat()) ?: null }
    }

    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
    fun getLastLocation() {
        if (ActivityCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                requestNewLocationData()
                if (location != null) {
                    val lat = location.latitude
                    val lon = location.longitude
                    Toast.makeText(activity, "Широта: $lat, Долгота: $lon", Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(activity, "Не удалось получить локацию", Toast.LENGTH_SHORT).show()
                    requestNewLocationData()
                }
            }
        }
    }

    fun requestNewLocationData() {
        val locationRequest = Builder(Priority.PRIORITY_HIGH_ACCURACY, 1000)
            .setWaitForAccurateLocation(false)
            .setMinUpdateIntervalMillis(500)
            .setMaxUpdates(1)
            .build()

        if (ActivityCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.requestLocationUpdates(locationRequest, object : LocationCallback() {
                override fun onLocationResult(locationResult: LocationResult) {
                    val location = locationResult.lastLocation
                    worldLocation = location
                    setMapLocation()
                    if (location != null) {
                        Toast.makeText(activity, "Новая локация: ${location.latitude} ${location.longitude}", Toast.LENGTH_SHORT).show()
                    }
                }
            }, Looper.getMainLooper())
        }
    }
}