package com.example.locationapplication

import android.location.Location

data class RequestPayload(
    val location: Location,
    val ts: Long,
    val userInfo: UserInfo
)
