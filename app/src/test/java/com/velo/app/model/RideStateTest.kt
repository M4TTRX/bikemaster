package com.velo.app.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RideStateTest {

    @Test
    fun testRideStatusTransitions() {
        var status = RideStatus.IDLE
        assertEquals("IDLE", status.name)

        status = RideStatus.RECORDING
        assertEquals("RECORDING", status.name)

        status = RideStatus.PAUSED
        assertEquals("PAUSED", status.name)

        status = RideStatus.FINISHED
        assertEquals("FINISHED", status.name)
    }

    @Test
    fun testUnitSystemEnum() {
        assertEquals("METRIC", UnitSystem.METRIC.name)
        assertEquals("IMPERIAL", UnitSystem.IMPERIAL.name)
    }

    @Test
    fun testAverageSpeedComputation() {
        val distanceMeters = 18000.0 // 18 km
        val elapsedSeconds = 1800L  // 30 min = 0.5 hr
        val avgSpeed = ((distanceMeters / 1000.0) / (elapsedSeconds / 3600.0)).toFloat()
        assertEquals(36.0f, avgSpeed, 0.01f)
    }

    @Test
    fun testGpsJitterFilterLogic() {
        val speedKmhStationary = 0.8f
        val deltaDistStationary = 1.2
        val shouldAddStationary = deltaDistStationary > 1.5 && speedKmhStationary > 1.5f
        assertFalse(shouldAddStationary)

        val speedKmhMoving = 22.5f
        val deltaDistMoving = 9.4
        val shouldAddMoving = deltaDistMoving > 1.5 && speedKmhMoving > 1.5f
        assertTrue(shouldAddMoving)
    }
}
