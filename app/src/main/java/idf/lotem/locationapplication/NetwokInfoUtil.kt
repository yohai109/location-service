package idf.lotem.locationapplication

import android.telephony.CellInfoLte


fun CellInfoLte.getSignalInfo(): SignalInfo {
    val data: ArrayList<String> = arrayListOf()
    val str: String = cellSignalStrength.toString()
    val temp = str.split(" ").toTypedArray()
    for (s in temp) {
        val temp2 = s.split("=").toTypedArray()
        if (temp2.size == 2) {
            data.add(temp2.last())
        }
    }

    val plmn = if (cellIdentity.mcc == Integer.MAX_VALUE || cellIdentity.mnc == Integer.MAX_VALUE) {
        "0"
    } else {
        cellIdentity.mcc.toString() + cellIdentity.mnc.toString()
    }

    return SignalInfo(
        rsrp = data[1].toInt(),
        rsrq = data[2].toInt(),
        cellId = cellIdentity.ci,
        pci = cellIdentity.pci,
        plmn = setOf(plmn)
    )
}