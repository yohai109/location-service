package com.example.locationapplication

import android.Manifest
import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.location.LocationManager
import android.net.wifi.WifiManager
import android.os.Build
import android.os.IBinder
import android.telephony.TelephonyManager
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import com.google.gson.Gson
import okio.Okio
import timber.log.Timber

class LocationService : Service() {

    private lateinit var httpHandler: HttpHandler
    private var iconNotification: Bitmap? = null
    private var notification: Notification? = null
    var mNotificationManager: NotificationManager? = null
    private val mNotificationId = 123

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
            return
        }
        var config: Config
        resources.openRawResource(R.raw.config).use {
            val source = Okio.source(it)
            val bufferedSource = Okio.buffer(source)
            config = Gson().fromJson(bufferedSource.readUtf8(), Config::class.java)
        }

        httpHandler = HttpHandler(this, config)

        val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager

        val listener = LocationUpdateHandler(getUserInfo()) {
            httpHandler.sendLocation(it)
        }
        locationManager.requestLocationUpdates(
            LocationManager.GPS_PROVIDER,
            30000L,
            5.0f,
            listener
        )
    }

    private fun getUserInfo(): UserInfo {
        val smsPermission = ActivityCompat.checkSelfPermission(
            this,
            Manifest.permission.READ_SMS
        )
        val phoneNumberPermission = ActivityCompat.checkSelfPermission(
            this,
            Manifest.permission.READ_PHONE_NUMBERS
        )

        val phoneStatePermission = ActivityCompat.checkSelfPermission(
            this,
            Manifest.permission.READ_PHONE_STATE
        )

        val wifiManagerService =
            applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
        val telephonyManagerService =
            getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager

        val phoneNUmber = if (
            phoneNumberPermission == PERMISSION_GRANTED &&
            smsPermission == PERMISSION_GRANTED
        ) {
            telephonyManagerService?.line1Number
        } else ""

        val mac = if (
            phoneStatePermission == PERMISSION_GRANTED) {
            wifiManagerService?.connectionInfo?.macAddress
        } else ""

        val imei = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            telephonyManagerService?.imei ?: ""
        } else {
            telephonyManagerService?.deviceId ?: ""
        }

        return UserInfo(
            androidVersion = Build.VERSION.RELEASE,
            IMEI = imei,
            mac = mac,
            PhoneNumber = phoneNUmber
        )
    }

    private fun generateForegroundNotification() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val intentMainLanding = Intent(this, MainActivity::class.java)
            val pendingIntent =
                PendingIntent.getActivity(this, 0, intentMainLanding, 0)
            iconNotification = BitmapFactory.decodeResource(resources, R.mipmap.ic_launcher)
            if (mNotificationManager == null) {
                mNotificationManager =
                    this.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                assert(mNotificationManager != null)
                mNotificationManager?.createNotificationChannelGroup(
                    NotificationChannelGroup("chats_group", "Chats")
                )
                val notificationChannel =
                    NotificationChannel(
                        "service_channel", "Service Notifications",
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
//                setContentText("Touch to open") //                    , swipe down for more options.
                setSmallIcon(R.drawable.ic_location_24dp)
                priority = NotificationCompat.PRIORITY_HIGH
                setWhen(0)
                setOnlyAlertOnce(true)
                setContentIntent(pendingIntent)
                setOngoing(true)
                if (iconNotification != null) {
                    setLargeIcon(Bitmap.createScaledBitmap(iconNotification!!, 128, 128, false))
                }
                builder.color = resources.getColor(R.color.purple_200)
            }
            notification = builder.build()
            startForeground(mNotificationId, notification)
        }
    }
}