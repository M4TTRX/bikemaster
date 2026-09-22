package com.velo.app.sensors

import com.velo.app.model.SensorDataType

data class DiscoveredSensor(
    val name: String,
    val address: String,
    val rssi: Int,
    val detectedCapabilities: Set<SensorDataType> = emptySet()
)
