package com.velo.app.model

import com.velo.app.data.PairedBleDevice
import com.velo.app.sensors.DiscoveredSensor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BlePairingTest {

    @Test
    fun testDiscoveredSensorModel() {
        val sensor = DiscoveredSensor(
            name = "Decathlon Dual SPD/CAD",
            address = "D4:36:39:84:B2:C1",
            rssi = -58,
            detectedCapabilities = setOf(SensorDataType.SPEED, SensorDataType.CADENCE)
        )

        assertEquals("Decathlon Dual SPD/CAD", sensor.name)
        assertEquals("D4:36:39:84:B2:C1", sensor.address)
        assertEquals(-58, sensor.rssi)
        assertTrue(sensor.detectedCapabilities.contains(SensorDataType.SPEED))
        assertTrue(sensor.detectedCapabilities.contains(SensorDataType.CADENCE))
        assertFalse(sensor.detectedCapabilities.contains(SensorDataType.POWER))
    }

    @Test
    fun testSensorDataTypeCoverage() {
        val types = SensorDataType.entries
        assertEquals(4, types.size)
        assertTrue(types.contains(SensorDataType.SPEED))
        assertTrue(types.contains(SensorDataType.CADENCE))
        assertTrue(types.contains(SensorDataType.POWER))
        assertTrue(types.contains(SensorDataType.HEART_RATE))
    }

    @Test
    fun testMetricTypeCoverage() {
        val metrics = MetricType.entries
        assertEquals(11, metrics.size)
        assertTrue(metrics.contains(MetricType.SPEED))
        assertTrue(metrics.contains(MetricType.CADENCE))
        assertTrue(metrics.contains(MetricType.POWER))
        assertTrue(metrics.contains(MetricType.HEART_RATE))
        assertTrue(metrics.contains(MetricType.DISTANCE))
        assertTrue(metrics.contains(MetricType.ELAPSED_TIME))
        assertTrue(metrics.contains(MetricType.AVG_SPEED))
        assertTrue(metrics.contains(MetricType.MAX_SPEED))
        assertTrue(metrics.contains(MetricType.AVG_POWER))
        assertTrue(metrics.contains(MetricType.MAX_POWER))
        assertTrue(metrics.contains(MetricType.ELEVATION))
        assertEquals("Elapsed Time", MetricType.ELAPSED_TIME.title)
    }

    @Test
    fun testTelemetrySensorConnectionFlags() {
        val defaultTelemetry = RideTelemetry()
        assertFalse(defaultTelemetry.isBleCadenceConnected)
        assertFalse(defaultTelemetry.isBleSpeedConnected)
        assertFalse(defaultTelemetry.isBlePowerConnected)
        assertFalse(defaultTelemetry.isHeartRateConnected)
        assertEquals(0, defaultTelemetry.powerWatts)

        val connectedTelemetry = defaultTelemetry.copy(
            isBleCadenceConnected = true,
            isBleSpeedConnected = true,
            isBlePowerConnected = true,
            isHeartRateConnected = true,
            powerWatts = 245,
            avgPowerWatts = 210,
            maxPowerWatts = 420
        )
        assertTrue(connectedTelemetry.isBleCadenceConnected)
        assertTrue(connectedTelemetry.isBleSpeedConnected)
        assertTrue(connectedTelemetry.isBlePowerConnected)
        assertTrue(connectedTelemetry.isHeartRateConnected)
        assertEquals(245, connectedTelemetry.powerWatts)
        assertEquals(210, connectedTelemetry.avgPowerWatts)
        assertEquals(420, connectedTelemetry.maxPowerWatts)
    }

    @Test
    fun testPairedBleDeviceSerialization() {
        val device = PairedBleDevice(
            address = "D4:36:39:84:B2:C1",
            name = "Decathlon Dual SPD/CAD",
            detectedCapabilities = setOf(SensorDataType.SPEED, SensorDataType.CADENCE),
            enabledDataTypes = setOf(SensorDataType.CADENCE) // User toggled off speed
        )
        val detectedStr = device.detectedCapabilities.joinToString(",") { it.name }
        val enabledStr = device.enabledDataTypes.joinToString(",") { it.name }
        val encoded = "${device.address}|${device.name}|$detectedStr|$enabledStr"

        assertEquals("D4:36:39:84:B2:C1|Decathlon Dual SPD/CAD|SPEED,CADENCE|CADENCE", encoded)

        val parts = encoded.split("|")
        val decoded = PairedBleDevice(
            address = parts[0],
            name = parts[1],
            detectedCapabilities = parts[2].split(",").map { SensorDataType.valueOf(it) }.toSet(),
            enabledDataTypes = parts[3].split(",").map { SensorDataType.valueOf(it) }.toSet()
        )
        assertEquals(device, decoded)
    }

    @Test
    fun testPowerMeterDeviceModel() {
        val powerMeter = PairedBleDevice(
            address = "F1:22:33:44:55:66",
            name = "Stages Power L",
            detectedCapabilities = setOf(SensorDataType.POWER, SensorDataType.CADENCE),
            enabledDataTypes = setOf(SensorDataType.POWER, SensorDataType.CADENCE)
        )
        assertTrue(powerMeter.detectedCapabilities.contains(SensorDataType.POWER))
        assertTrue(powerMeter.enabledDataTypes.contains(SensorDataType.CADENCE))
    }

    @Test
    fun testThemeModeOptions() {
        val modes = ThemeMode.entries
        assertEquals(3, modes.size)
        assertTrue(modes.contains(ThemeMode.DARK))
        assertTrue(modes.contains(ThemeMode.LIGHT))
        assertTrue(modes.contains(ThemeMode.SYSTEM))
        assertEquals("Dark", ThemeMode.DARK.title)
        assertEquals("Light", ThemeMode.LIGHT.title)
        assertEquals("System", ThemeMode.SYSTEM.title)
    }

    @Test
    fun testDecathlonBikeBrandsAndUnknownSensors() {
        val decathlonBrands = listOf("decathlon", "btwin", "b'twin", "van rysel", "triban", "elops", "rockrider", "geonaute")
        val sampleNames = listOf(
            "Van Rysel Power Meter",
            "Triban CSC",
            "Btwin Speed Sensor",
            "Decathlon HR",
            "Elops e-Bike Cadence"
        )
        for (name in sampleNames) {
            val lower = name.lowercase()
            assertTrue("Expected $name to match decathlon brands", decathlonBrands.any { lower.contains(it) })
        }

        // Unknown device pairing fallback
        val unknownSensor = DiscoveredSensor(
            name = "BK467",
            address = "AA:BB:CC:DD:EE:FF",
            rssi = -65,
            detectedCapabilities = emptySet()
        )
        assertEquals("BK467", unknownSensor.name)
        assertTrue(unknownSensor.detectedCapabilities.isEmpty())
    }

    @Test
    fun testSingleSensorBleManagerTimeouts() {
        assertEquals(5000L, com.velo.app.sensors.SingleSensorBleManager.SPEED_TIMEOUT_MS)
        assertEquals(4500L, com.velo.app.sensors.SingleSensorBleManager.CADENCE_TIMEOUT_MS)
        assertEquals(4500L, com.velo.app.sensors.SingleSensorBleManager.POWER_TIMEOUT_MS)
    }

    @Test
    fun testVanRyselSingleModeSensorCapabilities() {
        // In Decathlon Van Rysel sensors, green LED = Speed mode, red LED = Cadence mode
        val vanRyselSpeed = PairedBleDevice(
            address = "D4:36:39:84:B2:C1",
            name = "Van Rysel SPD",
            detectedCapabilities = setOf(SensorDataType.SPEED),
            enabledDataTypes = setOf(SensorDataType.SPEED)
        )
        assertTrue(vanRyselSpeed.detectedCapabilities.contains(SensorDataType.SPEED))
        assertFalse(vanRyselSpeed.detectedCapabilities.contains(SensorDataType.CADENCE))

        val vanRyselCadence = PairedBleDevice(
            address = "D4:36:39:84:B2:C2",
            name = "Van Rysel CAD",
            detectedCapabilities = setOf(SensorDataType.CADENCE),
            enabledDataTypes = setOf(SensorDataType.CADENCE)
        )
        assertFalse(vanRyselCadence.detectedCapabilities.contains(SensorDataType.SPEED))
        assertTrue(vanRyselCadence.detectedCapabilities.contains(SensorDataType.CADENCE))
    }
}
