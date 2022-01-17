package com.example.locationapplication

import android.location.Location

data class RequestBody(
    val location: Location,
    val ts: Long
)
