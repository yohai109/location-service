package com.example.locationapplication

import android.location.Location
import android.location.LocationListener
import android.os.Bundle
import timber.log.Timber

class LocationUpdateHandler(private val callback: (RequestPayload) -> Unit) : LocationListener {
    private lateinit var lastKnownLocation: Location

    override fun onLocationChanged(location: Location) {
        Timber.d("received locations: $location")
        lastKnownLocation = location
    }

    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {
        Timber.d("location listener on status changed ")
        Timber.d("provider: $provider")
        Timber.d("extras: $extras")
        Timber.d("extras is empty: ${extras?.isEmpty}")
        Timber.d("extras keys: ${extras?.keySet()?.joinToString(",")}")
        Timber.d("timestamp: ${System.currentTimeMillis()}")
//        extras?.get(LocationListener })

        val requestBody = RequestPayload(
            lastKnownLocation,
            lastKnownLocation.time
        )

        callback(requestBody)
    }
}