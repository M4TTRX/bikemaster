package com.velo.app

import android.app.Application
import com.velo.app.data.DefaultRideRepository
import com.velo.app.data.RideRepository
import com.velo.app.data.SettingsRepository
import com.velo.app.sensors.BleSensorManager
import com.velo.app.sensors.LocationTracker
import com.velo.app.sensors.WearOsManager

class VeloApplication : Application() {

    lateinit var settingsRepository: SettingsRepository
        private set

    lateinit var rideRepository: RideRepository
        private set

    override fun onCreate() {
        super.onCreate()
        settingsRepository = SettingsRepository(this)
        val locationTracker = LocationTracker(this)
        val bleSensorManager = BleSensorManager(this)
        val wearOsManager = WearOsManager(this)

        rideRepository = DefaultRideRepository(
            locationTracker = locationTracker,
            bleSensorManager = bleSensorManager,
            wearOsManager = wearOsManager,
            settingsRepository = settingsRepository
        )
    }
}
