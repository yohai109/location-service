package com.example.locationapplication

import android.telephony.CellInfoGsm
import android.telephony.CellInfoLte

data class RequestPayload(
    val location: LocationObj,
    val ts: Long,
    val userInfo: UserInfo
)

data class UserInfo(
    val androidVersion: String?,
    val mac: String?,
    val IMEI: String?,
    val PhoneNumber: String?,
    val networkOperator: String?,
    val networkOperatorName: String?,
    val networkInfo: NetworkInfo
)

data class NetworkInfo(
    val gsm: CellInfoGsm?,
    val lte: CellInfoLte?
)

data class LocationObj(
    val latitude: Double,
    val longitude: Double,
    val altitude: Double,
    val satellites: Int
)