package com.example.mobilka132.data.location

import android.content.pm.PackageManager
import android.widget.Toast
import androidx.compose.ui.geometry.Offset
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import android.Manifest
import android.location.Location
import android.os.Looper
import androidx.annotation.RequiresPermission
import androidx.core.app.ActivityCompat
import com.example.mobilka132.MainActivity
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest.Builder
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.Priority


class LocationManager {

    val mapLocation : Offset?
        get() = worldLocation?.let { l -> Offset(((l.longitude - 84.932881) * 122800).toFloat(), (-(l.latitude - 56.4758) * 222000).toFloat()) ?: null }
    var worldLocation : Location? = null
    var fusedLocationClient: FusedLocationProviderClient? = null
    val activity : MainActivity

    constructor(activity: MainActivity) {
        this.activity = activity
    }

    fun init() {
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(activity)
    }

    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
    fun getLastLocation() {
        fusedLocationClient!!.lastLocation.addOnSuccessListener { location ->
            requestNewLocationData()
            if (location != null) {
                val lat = location.latitude
                val lon = location.longitude
                Toast.makeText(activity, "Широта: $lat, Долгота: $lon", Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(activity, "Не удалось получить локацию", Toast.LENGTH_SHORT).show()
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
            fusedLocationClient!!.requestLocationUpdates(locationRequest, object : LocationCallback() {
                override fun onLocationResult(locationResult: LocationResult) {
                    val location = locationResult.lastLocation
                    worldLocation = location
                    if (location != null) {
                        Toast.makeText(activity, "Новая локация: ${location.latitude} ${location.longitude}", Toast.LENGTH_SHORT).show()
                    }
                }
            }, Looper.getMainLooper())
        }
    }

}