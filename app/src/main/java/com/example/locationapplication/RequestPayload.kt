package com.example.locationapplication

data class RequestPayload(
    val location: LocationObj,
    val ts: Long,
    val userInfo: UserInfo
)

data class UserInfo(
    val androidVersion: String?,
    val IMEI: String?,
    val imsi: String?,
    val PhoneNumber: String?,
    val networkOperator: String?,
    val networkOperatorName: String?,
    val networkInfo: NetworkInfo
)

data class NetworkInfo(
    val gsm: SignalInfo?,
    val lte: SignalInfo?
)

data class LocationObj(
    val latitude: Double,
    val longitude: Double,
    val altitude: Double,
    val satellites: Int
)

data class SignalInfo(
    val rsrp: Int?,
    val rsrq: Int?,
    val cellId: Int?,
    val pci: Int?,
    val plmn: Set<String>?,
)