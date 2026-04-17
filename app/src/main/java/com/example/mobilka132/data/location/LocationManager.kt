package com.example.mobilka132.data.location

import android.content.pm.PackageManager
import android.widget.Toast
import androidx.compose.ui.geometry.Offset
import com.google.android.gms.location.LocationServices
import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.IntentSender
import android.location.Location
import android.os.Looper
import androidx.activity.result.ActivityResultRegistry
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresPermission
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.app.ActivityCompat
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationSettingsRequest
import com.google.android.gms.location.Priority
import com.google.android.gms.location.SettingsClient


class LocationManager(
    private val activity: Activity,
    private val registry: ActivityResultRegistry,
    var pixelsInMeter: Float = 1f
) {

    var mapLocation by mutableStateOf<Offset?>(null)
    var worldLocation: Location? = null

    fun positionOnMap(l: Location): Offset {
        return Offset(
            ((l.longitude - /*84.932881*/ 84.944904) * 61400 + 747).toFloat(),
            (-(l.latitude - /*56.4758*/ 56.468946) * 111000 + 713).toFloat()
        ) * pixelsInMeter
    }

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
        if (ActivityCompat.checkSelfPermission(
                activity,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            checkLocationSettings()
        } else {
            requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    fun checkLocationSettings() {
        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 1000).build()

        val builder = LocationSettingsRequest.Builder()
            .addLocationRequest(locationRequest)

        val client: SettingsClient = LocationServices.getSettingsClient(activity)
        val task = client.checkLocationSettings(builder.build())

        task.addOnSuccessListener {
            @SuppressLint("MissingPermission")
            getLastLocation()
        }

        task.addOnFailureListener { exception ->
            if (exception is ResolvableApiException) {
                try {
                    exception.startResolutionForResult(activity, 111)
                } catch (sendEx: IntentSender.SendIntentException) {
                }
            }
        }
    }

    private fun setMapLocation() {
        if (worldLocation != null) {
            mapLocation = positionOnMap(worldLocation!!)
            if (mapLocation!!.x > 1500 || mapLocation!!.y < 0 || mapLocation!!.x < 0 || mapLocation!!.y > 1500)
                mapLocation = null
        }
    }

    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
    fun getLastLocation() {
        if (ActivityCompat.checkSelfPermission(
                activity,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                requestNewLocationData()
                if (location != null) {
                    val lat = location.latitude
                    val lon = location.longitude
                    Toast.makeText(activity, "Широта: $lat, Долгота: $lon", Toast.LENGTH_LONG)
                        .show()
                } else {
                    Toast.makeText(activity, "Не удалось получить локацию", Toast.LENGTH_SHORT)
                        .show()
                    requestNewLocationData()
                }
            }
        }
    }

    fun requestNewLocationData() {
        if (ActivityCompat.checkSelfPermission(
                activity,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 1000)
                .setWaitForAccurateLocation(false)
                .setMinUpdateIntervalMillis(500)
                .setMaxUpdates(1)
                .build()
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                object : LocationCallback() {
                    override fun onLocationResult(locationResult: LocationResult) {
                        val location = locationResult.lastLocation
                        worldLocation = location
                        setMapLocation()
                        if (location != null) {
                            Toast.makeText(
                                activity,
                                "Новая локация: ${location.latitude} ${location.longitude}",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                },
                Looper.getMainLooper()
            )
        }
    }
}