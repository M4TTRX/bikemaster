package com.velo.app.model

enum class RideStatus {
    IDLE,
    RECORDING,
    PAUSED,
    FINISHED
}

enum class UnitSystem {
    METRIC,    // km, km/h, meters
    IMPERIAL   // mi, mph, feet
}

enum class HeartRateZone(val title: String) {
    WARMUP("Warmup"),
    AEROBIC("Aerobic"),
    CARDIO("Cardio"),
    PEAK("Peak")
}

enum class SpeedSource(val title: String) {
    AUTO("Auto (Sensor Priority)"),
    SPEED_SENSOR("Speed Sensor Only"),
    GPS("GPS Only")
}

enum class SensorDataType(val title: String) {
    SPEED("Speed"),
    CADENCE("Cadence"),
    POWER("Power"),
    HEART_RATE("Heart Rate")
}

enum class MetricType(val title: String, val defaultUnitMetric: String, val defaultUnitImperial: String) {
    SPEED("Speed", "km/h", "mph"),
    CADENCE("Cadence", "rpm", "rpm"),
    HEART_RATE("Heart Rate", "bpm", "bpm"),
    POWER("Power", "W", "W"),
    DISTANCE("Distance", "km", "mi"),
    ELAPSED_TIME("Time", "", ""),
    AVG_SPEED("Avg Speed", "km/h", "mph"),
    MAX_SPEED("Max Speed", "km/h", "mph"),
    AVG_POWER("Avg Power", "W", "W"),
    MAX_POWER("Max Power", "W", "W"),
    ELEVATION("Elevation", "m", "ft")
}

data class RideTelemetry(
    val speedKmh: Float = 0.0f,
    val maxSpeedKmh: Float = 0.0f,
    val heartRateBpm: Int = 0,
    val distanceMeters: Double = 0.0,
    val elapsedTimeSeconds: Long = 0L,
    val cadenceRpm: Int = 0,
    val avgSpeedKmh: Float = 0.0f,
    val elevationGainMeters: Float = 0.0f,
    val powerWatts: Int = 0,
    val avgPowerWatts: Int = 0,
    val maxPowerWatts: Int = 0,
    val isGpsActive: Boolean = false,
    val isHeartRateConnected: Boolean = false,
    val isBleCadenceConnected: Boolean = false,
    val isBleSpeedConnected: Boolean = false,
    val isBlePowerConnected: Boolean = false,
    val speedSource: SpeedSource = SpeedSource.AUTO
) {
    val heartRateZone: HeartRateZone?
        get() = when {
            heartRateBpm <= 0 -> null
            heartRateBpm < 115 -> HeartRateZone.WARMUP
            heartRateBpm < 140 -> HeartRateZone.AEROBIC
            heartRateBpm < 165 -> HeartRateZone.CARDIO
            else -> HeartRateZone.PEAK
        }
}
