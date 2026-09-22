package com.velo.app.model

import com.velo.app.sensors.DiscoveredSensor
import com.velo.app.sensors.SensorType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BlePairingTest {

    @Test
    fun testDiscoveredSensorModel() {
        val sensor = DiscoveredSensor(
            name = "Wahoo SPEED v2",
            address = "C4:7D:CC:12:34:56",
            rssi = -62,
            type = SensorType.CADENCE_SPEED
        )

        assertEquals("Wahoo SPEED v2", sensor.name)
        assertEquals("C4:7D:CC:12:34:56", sensor.address)
        assertEquals(-62, sensor.rssi)
        assertEquals(SensorType.CADENCE_SPEED, sensor.type)
    }

    @Test
    fun testSensorTypeCoverage() {
        val types = SensorType.entries
        assertEquals(3, types.size)
        assertTrue(types.contains(SensorType.CADENCE_SPEED))
        assertTrue(types.contains(SensorType.POWER))
        assertTrue(types.contains(SensorType.HEART_RATE))
    }

    @Test
    fun testTelemetrySensorConnectionFlags() {
        val defaultTelemetry = RideTelemetry()
        assertFalse(defaultTelemetry.isBleCadenceConnected)
        assertFalse(defaultTelemetry.isBlePowerConnected)
        assertFalse(defaultTelemetry.isWearOsConnected)

        val connectedTelemetry = defaultTelemetry.copy(
            isBleCadenceConnected = true,
            isBlePowerConnected = true,
            isWearOsConnected = true
        )
        assertTrue(connectedTelemetry.isBleCadenceConnected)
        assertTrue(connectedTelemetry.isBlePowerConnected)
        assertTrue(connectedTelemetry.isWearOsConnected)
    }

    @Test
    fun testPairedBleDeviceEncoding() {
        val device = com.velo.app.data.PairedBleDevice(
            type = SensorType.CADENCE_SPEED,
            address = "C4:7D:CC:12:34:56",
            name = "Wahoo SPEED v2"
        )
        val encoded = "${device.type.name}|${device.address}|${device.name}"
        assertEquals("CADENCE_SPEED|C4:7D:CC:12:34:56|Wahoo SPEED v2", encoded)

        val parts = encoded.split("|", limit = 3)
        val decoded = com.velo.app.data.PairedBleDevice(
            type = SensorType.valueOf(parts[0]),
            address = parts[1],
            name = parts[2]
        )
        assertEquals(device, decoded)
    }
}
