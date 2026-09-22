package com.velo.app.sensors

import com.velo.app.model.RideTelemetry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.max
import kotlin.math.sin

/**
 * Built-in simulator engine allowing end-to-end testing of real-time speed morphing,
 * power, heart rate zones, elevation gain, and cadence on the Android Emulator without
 * physical bike sensors.
 */
class MockRideSimulator(private val scope: CoroutineScope) {

    private val _telemetry = MutableStateFlow(
        RideTelemetry(
            speedKmh = 0.0f,
            maxSpeedKmh = 0.0f,
            heartRateBpm = 0,
            distanceMeters = 0.0,
            elapsedTimeSeconds = 0L,
            cadenceRpm = 0,
            avgSpeedKmh = 0.0f,
            elevationGainMeters = 0f,
            powerWatts = 0,
            avgPowerWatts = 0,
            maxPowerWatts = 0,
            isGpsActive = false,
            isHeartRateConnected = false,
            isBlePowerConnected = false
        )
    )
    val telemetry: StateFlow<RideTelemetry> = _telemetry.asStateFlow()

    private var simulationJob: Job? = null
    private var tick = 0
    private var totalPowerJoules: Long = 0L

    fun start() {
        if (simulationJob?.isActive == true) return

        simulationJob = scope.launch {
            while (isActive) {
                delay(1000L) // Throttled to 1Hz battery-saving update rate
                tick++

                // Create realistic smooth cycling dynamics
                val speedWave = (sin(tick * 0.08) * 12.0).toFloat()
                val currentSpeed = (32.0f + speedWave).coerceAtLeast(0.0f)

                // Alternate HR through zones (Warmup -> Aerobic -> Cardio -> Peak)
                val hrWave = (sin(tick * 0.05) * 35).toInt()
                val currentHr = (135 + hrWave).coerceIn(90, 185)

                // Power dynamics (160W - 320W)
                val powerWave = (sin(tick * 0.09) * 60).toInt()
                val currentPower = (210 + powerWave).coerceIn(120, 380)
                totalPowerJoules += currentPower

                val prev = _telemetry.value
                val newDistance = prev.distanceMeters + (currentSpeed / 3.6)
                val newElapsed = prev.elapsedTimeSeconds + 1
                val newElevation = prev.elevationGainMeters + if (tick % 5 == 0) 1.0f else 0.0f
                val newCadence = (85 + (sin(tick * 0.07) * 10).toInt()).coerceIn(60, 110)
                val newMaxSpeed = max(prev.maxSpeedKmh, currentSpeed)
                val newMaxPower = max(prev.maxPowerWatts, currentPower)
                val newAvgPower = if (newElapsed > 0) (totalPowerJoules / newElapsed).toInt() else 0

                _telemetry.value = prev.copy(
                    speedKmh = currentSpeed,
                    maxSpeedKmh = newMaxSpeed,
                    heartRateBpm = currentHr,
                    distanceMeters = newDistance,
                    elapsedTimeSeconds = newElapsed,
                    cadenceRpm = newCadence,
                    avgSpeedKmh = ((newDistance / 1000.0) / (newElapsed / 3600.0)).toFloat(),
                    elevationGainMeters = newElevation,
                    powerWatts = currentPower,
                    avgPowerWatts = newAvgPower,
                    maxPowerWatts = newMaxPower,
                    isGpsActive = true,
                    isHeartRateConnected = true,
                    isBlePowerConnected = true
                )
            }
        }
    }

    fun stop() {
        simulationJob?.cancel()
        simulationJob = null
    }

    fun reset() {
        stop()
        tick = 0
        totalPowerJoules = 0L
        _telemetry.value = RideTelemetry(
            speedKmh = 0.0f,
            maxSpeedKmh = 0.0f,
            heartRateBpm = 0,
            distanceMeters = 0.0,
            elapsedTimeSeconds = 0L,
            cadenceRpm = 0,
            avgSpeedKmh = 0.0f,
            elevationGainMeters = 0.0f,
            powerWatts = 0,
            avgPowerWatts = 0,
            maxPowerWatts = 0,
            isGpsActive = false,
            isHeartRateConnected = false,
            isBlePowerConnected = false
        )
    }
}
