package com.velo.app.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RideTelemetryTest {

    @Test
    fun testHeartRateZoneClassification() {
        val zeroHr = RideTelemetry(heartRateBpm = 0)
        assertNull(zeroHr.heartRateZone)

        val warmupHr = RideTelemetry(heartRateBpm = 110)
        assertEquals(HeartRateZone.WARMUP, warmupHr.heartRateZone)

        val aerobicHr = RideTelemetry(heartRateBpm = 135)
        assertEquals(HeartRateZone.AEROBIC, aerobicHr.heartRateZone)

        val cardioHr = RideTelemetry(heartRateBpm = 155)
        assertEquals(HeartRateZone.CARDIO, cardioHr.heartRateZone)

        val peakHr = RideTelemetry(heartRateBpm = 175)
        assertEquals(HeartRateZone.PEAK, peakHr.heartRateZone)
    }

    @Test
    fun testDistanceAndSpeedImperialCalculations() {
        val speedKmh = 30.0f
        val speedMph = speedKmh * 0.621371f
        assertEquals(18.64f, speedMph, 0.01f)

        val distanceMeters = 10000.0 // 10 km
        val distanceKm = distanceMeters / 1000.0
        val distanceMiles = distanceKm * 0.621371
        assertEquals(10.0, distanceKm, 0.001)
        assertEquals(6.213, distanceMiles, 0.001)
    }

    @Test
    fun testSpeedSourceEnumValues() {
        assertEquals("AUTO", SpeedSource.AUTO.name)
        assertEquals("SPEED_SENSOR", SpeedSource.SPEED_SENSOR.name)
        assertEquals("GPS", SpeedSource.GPS.name)
        assertEquals("Auto (Sensor Priority)", SpeedSource.AUTO.title)
    }

    @Test
    fun testRideTelemetryDefaults() {
        val telemetry = RideTelemetry()
        assertEquals(0.0f, telemetry.speedKmh, 0.001f)
        assertEquals(0.0f, telemetry.maxSpeedKmh, 0.001f)
        assertEquals(0, telemetry.heartRateBpm)
        assertEquals(0.0, telemetry.distanceMeters, 0.001)
        assertEquals(0L, telemetry.elapsedTimeSeconds)
        assertEquals(0, telemetry.cadenceRpm)
        assertEquals(0.0f, telemetry.avgSpeedKmh, 0.001f)
        assertEquals(0.0f, telemetry.elevationGainMeters, 0.001f)
        assertEquals(SpeedSource.AUTO, telemetry.speedSource)
    }
}
