package com.example.locationapplication

import android.telephony.CellInfo
import android.telephony.CellInfoGsm
import android.telephony.CellInfoLte
import com.google.gson.Gson
import timber.log.Timber

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
    val cellInfo: List<String>,
    val networkOperator: String?,
    val networkOperatorName: String?
)

data class LocationObj(
    val latitude: Double,
    val longitude: Double,
    val altitude: Double,
    val satellites: Int
)

fun CellInfo.asString(): String {
    val gson = Gson()
    Timber.d("cell info type: ${this::class.simpleName}")
    return when (this) {
        is CellInfoLte -> {
            gson.toJson(this.cellSignalStrength)
//            gson.toJson(this)
        }
        is CellInfoGsm -> gson.toJson(this)
        else -> gson.toJson(this)
    }
}