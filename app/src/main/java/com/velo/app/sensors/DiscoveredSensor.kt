package com.velo.app.sensors

data class DiscoveredSensor(
    val name: String,
    val address: String,
    val rssi: Int,
    val type: SensorType
)

enum class SensorType(val title: String) {
    HEART_RATE("Heart Rate"),
    CADENCE_SPEED("Cadence & Speed (CSC)"),
    POWER("Cycling Power")
}
