package idf.lotem.locationapplication

import android.Manifest
import android.annotation.SuppressLint
import android.app.*
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.location.LocationManager
import android.os.BatteryManager
import android.os.Build
import android.os.IBinder
import android.telephony.CellInfoLte
import android.telephony.TelephonyManager
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import okio.Okio
import timber.log.Timber

class LocationService : Service() {

    private lateinit var httpHandler: HttpHandler
    private var iconNotification: Bitmap? = null
    private var notification: Notification? = null
    private var mNotificationManager: NotificationManager? = null
    private val mNotificationId = 123

    private val job = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.Main + job)

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        generateForegroundNotification()
        return START_STICKY

    }

    override fun onCreate() {
        super.onCreate()
        Timber.d("Location service on create()")
        val hasFineLocationPermission = ActivityCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PERMISSION_GRANTED

        val hasCoarseLocationPermission = ActivityCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PERMISSION_GRANTED

        if (!hasFineLocationPermission && !hasCoarseLocationPermission) {
            // TODO handle no permission granted
            Timber.d("missing permissions")
            return
        }

        var config: Config
        resources.openRawResource(R.raw.config).use {
            val source = Okio.source(it)
            val bufferedSource = Okio.buffer(source)
            config = Gson().fromJson(bufferedSource.readUtf8(), Config::class.java)
        }

        httpHandler = HttpHandler(this, config)


        val listener = LocationUpdateHandler(::getUserInfo) { locationBody ->
            httpHandler.sendLocation(locationBody)
        }

        httpHandler.getConfiguration {
            Timber.d("getConfiguration call back")
            val locationManager = getSystemService(LOCATION_SERVICE) as LocationManager
            scope.launch {
                locationManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER,
                    it.minTimeMs,
                    it.minDistanceM,
                    listener
                )
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        job.cancel()
    }

    @SuppressLint("HardwareIds")
    private fun getUserInfo(): UserInfo {
        val smsPermission = ActivityCompat.checkSelfPermission(
            this,
            Manifest.permission.READ_SMS
        )

        val phoneStatePermission = ActivityCompat.checkSelfPermission(
            this,
            Manifest.permission.READ_PHONE_STATE
        )

        val telephonyManagerService =
            getSystemService(TELEPHONY_SERVICE) as? TelephonyManager

        val phoneNUmber = if (
            phoneStatePermission == PERMISSION_GRANTED &&
            smsPermission == PERMISSION_GRANTED
        ) {
            telephonyManagerService?.line1Number
        } else ""

        val imei = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                telephonyManagerService?.imei ?: ""
            } catch (e: Exception) {
                ""
            }
        } else {
            telephonyManagerService?.deviceId ?: ""
        }

        Timber.d("simserial number: ${telephonyManagerService?.simSerialNumber}")
        Timber.d("simserial number: ${telephonyManagerService?.subscriberId}")

        val cellInfoLte =
            telephonyManagerService?.allCellInfo?.find { it is CellInfoLte } as? CellInfoLte

        val lteSignalInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            SignalInfo(
                cellInfoLte?.cellSignalStrength?.rsrp,
                cellInfoLte?.cellSignalStrength?.rsrq,
                cellInfoLte?.cellIdentity?.ci,
                cellInfoLte?.cellIdentity?.pci,
                cellInfoLte?.cellIdentity?.additionalPlmns
            )
        } else {
            cellInfoLte?.getSignalInfo()
        }

        return UserInfo(
            androidVersion = Build.VERSION.RELEASE,
            IMEI = imei,
            imsi = telephonyManagerService?.subscriberId,
            PhoneNumber = phoneNUmber,
            networkInfo = NetworkInfo(
                null,
                lte = lteSignalInfo
            ),
            networkOperator = telephonyManagerService?.networkOperator,
            networkOperatorName = telephonyManagerService?.networkOperatorName,
            batteryLevel = getBatteryLevel()
        )
    }

    private fun generateForegroundNotification() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val intentMainLanding = Intent(this, MainActivity::class.java)
            val pendingIntent = PendingIntent.getActivity(
                this,
                0,
                intentMainLanding,
                0
            )
            iconNotification = BitmapFactory.decodeResource(resources, R.mipmap.ic_launcher)
            if (mNotificationManager == null) {
                mNotificationManager =
                    this.getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                assert(mNotificationManager != null)
                mNotificationManager?.createNotificationChannelGroup(
                    NotificationChannelGroup("chats_group", "Chats")
                )
                val notificationChannel = NotificationChannel(
                    "service_channel",
                    "Service Notifications",
                    NotificationManager.IMPORTANCE_MIN
                )
                notificationChannel.enableLights(false)
                notificationChannel.lockscreenVisibility = Notification.VISIBILITY_SECRET
                mNotificationManager?.createNotificationChannel(notificationChannel)
            }
            val builder = NotificationCompat.Builder(this, "service_channel")
            val title = StringBuilder(resources.getString(R.string.app_name))
                .append(" service is running")
                .toString()
            builder.apply {
                setContentTitle(title)
                setTicker(title)
                setSmallIcon(R.drawable.ic_location_24dp)
                priority = NotificationCompat.PRIORITY_HIGH
                setWhen(0)
                setOnlyAlertOnce(true)
                setContentIntent(pendingIntent)
                setOngoing(true)
                if (iconNotification != null) {
                    setLargeIcon(Bitmap.createScaledBitmap(iconNotification!!, 128, 128, false))
                }
                color = resources.getColor(R.color.purple_200, theme)
            }
            notification = builder.build()
            startForeground(mNotificationId, notification)
        }
    }

    private fun getBatteryLevel(): Int {
        val iFilter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        val batteryStatus: Intent? = registerReceiver(null, iFilter)
        val level = batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = batteryStatus?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        val batteryPct = level / scale.toDouble()
        return (batteryPct * 100).toInt()
    }
}