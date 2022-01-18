package idf.lotem.locationapplication

import android.location.Location
import android.location.LocationListener
import android.os.Bundle

class LocationUpdateHandler(
    private val userInfo: () -> UserInfo,
    private val callback: (RequestPayload) -> Unit
) : LocationListener {
    private lateinit var lastKnownLocation: Location

    override fun onLocationChanged(location: Location) {
        lastKnownLocation = location

        val requestBody = RequestPayload(
            LocationObj(
                lastKnownLocation.latitude,
                lastKnownLocation.longitude,
                lastKnownLocation.altitude,
                lastKnownLocation.extras.getInt("satellites")
            ),
            lastKnownLocation.time,
            userInfo()
        )

        callback(requestBody)
    }

    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {
        val requestBody = RequestPayload(
            LocationObj(
                lastKnownLocation.latitude,
                lastKnownLocation.longitude,
                lastKnownLocation.altitude,
                lastKnownLocation.extras.getInt("satellites")
            ),
            lastKnownLocation.time,
            userInfo()
        )

        callback(requestBody)
    }
}