package com.velo.app.model

enum class RideStatus {
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

data class RideTelemetry(
    val speedKmh: Float = 0.0f,
    val heartRateBpm: Int = 0,
    val distanceMeters: Double = 0.0,
    val elapsedTimeSeconds: Long = 0L,
    val cadenceRpm: Int = 0,
    val avgSpeedKmh: Float = 0.0f,
    val elevationGainMeters: Float = 0.0f,
    val powerWatts: Int = 0,
    val isGpsActive: Boolean = false,
    val isWearOsConnected: Boolean = false,
    val isBleCadenceConnected: Boolean = false,
    val isBlePowerConnected: Boolean = false
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
