package idf.lotem.locationapplication

import android.app.Application
import timber.log.Timber

class LocationApplication: Application() {
    override fun onCreate() {
        super.onCreate()
        Timber.plant(Timber.DebugTree())
    }
}