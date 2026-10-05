package com.velo.app.sensors

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.content.Context
import android.os.Handler
import android.os.Looper
import com.velo.app.model.SensorDataType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import no.nordicsemi.android.ble.BleManager
import no.nordicsemi.android.ble.observer.ConnectionObserver
import java.util.UUID

class SingleSensorBleManager(
    context: Context,
    var wheelCircumferenceMm: Int = 2105,
    var enabledDataTypes: Set<SensorDataType> = setOf(
        SensorDataType.SPEED,
        SensorDataType.CADENCE,
        SensorDataType.POWER,
        SensorDataType.HEART_RATE
    ),
    private val onCadenceUpdate: (Int) -> Unit = {},
    private val onSpeedUpdate: (speedKmh: Float, distanceDeltaMeters: Double) -> Unit = { _, _ -> },
    private val onPowerUpdate: (Int) -> Unit = {},
    private val onHeartRateUpdate: (Int) -> Unit = {},
    private val onConnectionStateChange: (Boolean) -> Unit = {},
    private val onDetectedCapabilities: (Set<SensorDataType>) -> Unit = {}
) : BleManager(context) {

    companion object {
        const val SPEED_TIMEOUT_MS = 5000L
        const val CADENCE_TIMEOUT_MS = 4500L
        const val POWER_TIMEOUT_MS = 4500L

        val CSC_SERVICE_UUID: UUID = UUID.fromString("00001816-0000-1000-8000-00805f9b34fb")
        val CSC_MEASUREMENT_UUID: UUID = UUID.fromString("00002A5B-0000-1000-8000-00805f9b34fb")

        val HRM_SERVICE_UUID: UUID = UUID.fromString("0000180D-0000-1000-8000-00805f9b34fb")
        val HRM_MEASUREMENT_UUID: UUID = UUID.fromString("00002A37-0000-1000-8000-00805f9b34fb")

        val CYCLING_POWER_SERVICE_UUID: UUID = UUID.fromString("00001818-0000-1000-8000-00805f9b34fb")
        val CYCLING_POWER_MEASUREMENT_UUID: UUID = UUID.fromString("00002A63-0000-1000-8000-00805f9b34fb")
    }

    private var cscCharacteristic: BluetoothGattCharacteristic? = null
    private var hrmCharacteristic: BluetoothGattCharacteristic? = null
    private var powerCharacteristic: BluetoothGattCharacteristic? = null

    private var lastCrankRevs: Int? = null
    private var lastCrankTime: Int? = null

    private var lastPowerCrankRevs: Int? = null
    private var lastPowerCrankTime: Int? = null

    private var lastWheelRevs: Long? = null
    private var lastWheelTime: Int? = null

    private var capabilitiesResolved = false

    var isManualDisconnect = false
        private set
    var currentDevice: BluetoothDevice? = null
        private set

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var reconnectJob: Job? = null

    private val timeoutHandler = Handler(Looper.getMainLooper())
    private val cadenceTimeoutRunnable = Runnable {
        lastCrankRevs = null
        lastCrankTime = null
        lastPowerCrankRevs = null
        lastPowerCrankTime = null
        onCadenceUpdate(0)
    }
    private val speedTimeoutRunnable = Runnable {
        lastWheelRevs = null
        lastWheelTime = null
        onSpeedUpdate(0f, 0.0)
    }
    private val powerTimeoutRunnable = Runnable {
        onPowerUpdate(0)
    }

    init {
        setConnectionObserver(object : ConnectionObserver {
            override fun onDeviceConnecting(device: BluetoothDevice) {}

            override fun onDeviceConnected(device: BluetoothDevice) {}

            override fun onDeviceFailedToConnect(device: BluetoothDevice, reason: Int) {
                clearTimeouts()
                onConnectionStateChange(false)
                scheduleReconnect(device)
            }

            override fun onDeviceReady(device: BluetoothDevice) {
                reconnectJob?.cancel()
                reconnectJob = null
                onConnectionStateChange(true)
            }

            override fun onDeviceDisconnecting(device: BluetoothDevice) {}

            override fun onDeviceDisconnected(device: BluetoothDevice, reason: Int) {
                clearTimeouts()
                capabilitiesResolved = false
                lastWheelRevs = null
                lastWheelTime = null
                lastCrankRevs = null
                lastCrankTime = null
                lastPowerCrankRevs = null
                lastPowerCrankTime = null
                onCadenceUpdate(0)
                onSpeedUpdate(0f, 0.0)
                onPowerUpdate(0)
                onHeartRateUpdate(0)
                onConnectionStateChange(false)
                scheduleReconnect(device)
            }
        })
    }

    fun startConnection(device: BluetoothDevice) {
        currentDevice = device
        isManualDisconnect = false
        reconnectJob?.cancel()
        reconnectJob = null
        connect(device)
            .useAutoConnect(false)
            .retry(3, 500)
            .enqueue()
    }

    private fun scheduleReconnect(device: BluetoothDevice) {
        if (isManualDisconnect) return
        reconnectJob?.cancel()
        reconnectJob = scope.launch {
            delay(2500L)
            if (!isManualDisconnect && !isConnected) {
                connect(device)
                    .useAutoConnect(false)
                    .retry(2, 500)
                    .enqueue()
            }
        }
    }

    fun closeClient() {
        isManualDisconnect = true
        reconnectJob?.cancel()
        reconnectJob = null
        clearTimeouts()
        try {
            disconnect().enqueue()
        } catch (e: Exception) {
            // Ignore
        }
        try {
            close()
        } catch (e: Exception) {
            // Ignore
        }
    }

    private fun clearTimeouts() {
        timeoutHandler.removeCallbacks(cadenceTimeoutRunnable)
        timeoutHandler.removeCallbacks(speedTimeoutRunnable)
        timeoutHandler.removeCallbacks(powerTimeoutRunnable)
    }

    @Deprecated("Overriding Nordic BleManager deprecated method")
    @Suppress("DEPRECATION")
    override fun getGattCallback(): BleManagerGattCallback = object : BleManagerGattCallback() {
        override fun isRequiredServiceSupported(gatt: BluetoothGatt): Boolean {
            val capabilities = mutableSetOf<SensorDataType>()

            val cscService = gatt.getService(CSC_SERVICE_UUID)
            if (cscService != null) {
                cscCharacteristic = cscService.getCharacteristic(CSC_MEASUREMENT_UUID)
                if (cscCharacteristic != null) {
                    capabilities.add(SensorDataType.SPEED)
                    capabilities.add(SensorDataType.CADENCE)
                }
            }

            val hrmService = gatt.getService(HRM_SERVICE_UUID)
            if (hrmService != null) {
                hrmCharacteristic = hrmService.getCharacteristic(HRM_MEASUREMENT_UUID)
                if (hrmCharacteristic != null) {
                    capabilities.add(SensorDataType.HEART_RATE)
                }
            }

            val powerService = gatt.getService(CYCLING_POWER_SERVICE_UUID)
            if (powerService != null) {
                powerCharacteristic = powerService.getCharacteristic(CYCLING_POWER_MEASUREMENT_UUID)
                if (powerCharacteristic != null) {
                    capabilities.add(SensorDataType.POWER)
                    capabilities.add(SensorDataType.CADENCE)
                }
            }

            if (capabilities.isNotEmpty()) {
                onDetectedCapabilities(capabilities)
            }

            return cscCharacteristic != null || hrmCharacteristic != null || powerCharacteristic != null
        }

        @Deprecated("Overriding Nordic BleManager deprecated method")
        @Suppress("DEPRECATION")
        override fun initialize() {
            // 1. Cycling Speed and Cadence (0x1816)
            cscCharacteristic?.let { char ->
                setNotificationCallback(char).with { _, data ->
                    val rawBytes = data.value
                    if (rawBytes != null && rawBytes.isNotEmpty()) {
                        val flags = rawBytes[0].toInt() and 0xFF
                        val hasWheel = (flags and 0x01) != 0
                        val hasCrank = (flags and 0x02) != 0

                        // Dynamic capability detection for Decathlon / Van Rysel (single mode: speed OR cadence)
                        val detected = mutableSetOf<SensorDataType>()
                        if (hasWheel) detected.add(SensorDataType.SPEED)
                        if (hasCrank) detected.add(SensorDataType.CADENCE)
                        if (detected.isNotEmpty() && (!capabilitiesResolved ||
                                (hasWheel != enabledDataTypes.contains(SensorDataType.SPEED) &&
                                 hasCrank != enabledDataTypes.contains(SensorDataType.CADENCE)))) {
                            capabilitiesResolved = true
                            onDetectedCapabilities(detected)
                        }

                        var offset = 1

                        // Parse Wheel Revolution Data if present
                        if (hasWheel && rawBytes.size >= offset + 6) {
                            val wheelRevs = (rawBytes[offset].toLong() and 0xFF) or
                                    ((rawBytes[offset + 1].toLong() and 0xFF) shl 8) or
                                    ((rawBytes[offset + 2].toLong() and 0xFF) shl 16) or
                                    ((rawBytes[offset + 3].toLong() and 0xFF) shl 24)
                            val wheelTime = (rawBytes[offset + 4].toInt() and 0xFF) or
                                    ((rawBytes[offset + 5].toInt() and 0xFF) shl 8)

                            val prevRevs = lastWheelRevs
                            val prevTime = lastWheelTime

                            if (prevRevs != null && prevTime != null) {
                                val dRevs = (wheelRevs - prevRevs) and 0xFFFFFFFFL
                                val dTime = (wheelTime - prevTime) and 0xFFFF

                                if (dTime > 0 && dRevs > 0) {
                                    val timeSec = dTime.toDouble() / 1024.0
                                    val distMeters = dRevs.toDouble() * (wheelCircumferenceMm.toDouble() / 1000.0)
                                    val speedMps = distMeters / timeSec
                                    val speedKmh = (speedMps * 3.6).toFloat()

                                    if (speedKmh in 0.5f..150f && enabledDataTypes.contains(SensorDataType.SPEED)) {
                                        onSpeedUpdate(speedKmh, distMeters)
                                        timeoutHandler.removeCallbacks(speedTimeoutRunnable)
                                        timeoutHandler.postDelayed(speedTimeoutRunnable, SPEED_TIMEOUT_MS)
                                    }
                                }
                            }
                            lastWheelRevs = wheelRevs
                            lastWheelTime = wheelTime
                            offset += 6
                        }

                        // Parse Crank Revolution Data if present
                        if (hasCrank && rawBytes.size >= offset + 4) {
                            val crankRevs = (rawBytes[offset].toInt() and 0xFF) or
                                    ((rawBytes[offset + 1].toInt() and 0xFF) shl 8)
                            val crankTime = (rawBytes[offset + 2].toInt() and 0xFF) or
                                    ((rawBytes[offset + 3].toInt() and 0xFF) shl 8)

                            val prevRevs = lastCrankRevs
                            val prevTime = lastCrankTime

                            if (prevRevs != null && prevTime != null) {
                                val dRevs = (crankRevs - prevRevs) and 0xFFFF
                                val dTime = (crankTime - prevTime) and 0xFFFF
                                if (dTime > 0 && dRevs > 0) {
                                    val rpm = ((dRevs.toDouble() / (dTime.toDouble() / 1024.0)) * 60.0).toInt()
                                    if (enabledDataTypes.contains(SensorDataType.CADENCE)) {
                                        onCadenceUpdate(rpm.coerceIn(0, 220))
                                        timeoutHandler.removeCallbacks(cadenceTimeoutRunnable)
                                        timeoutHandler.postDelayed(cadenceTimeoutRunnable, CADENCE_TIMEOUT_MS)
                                    }
                                }
                            }
                            lastCrankRevs = crankRevs
                            lastCrankTime = crankTime
                        }
                    }
                }
                enableNotifications(char).enqueue()
            }

            // 2. Heart Rate Measurement (0x180D)
            hrmCharacteristic?.let { char ->
                setNotificationCallback(char).with { _, data ->
                    val rawBytes = data.value
                    if (rawBytes != null && rawBytes.size >= 2) {
                        val flags = rawBytes[0].toInt() and 0xFF
                        val is16Bit = (flags and 0x01) != 0
                        val hr = if (is16Bit && rawBytes.size >= 3) {
                            (rawBytes[1].toInt() and 0xFF) or ((rawBytes[2].toInt() and 0xFF) shl 8)
                        } else {
                            rawBytes[1].toInt() and 0xFF
                        }
                        if (enabledDataTypes.contains(SensorDataType.HEART_RATE)) {
                            onHeartRateUpdate(hr)
                        }
                    }
                }
                enableNotifications(char).enqueue()
            }

            // 3. Cycling Power Measurement (0x1818)
            powerCharacteristic?.let { char ->
                setNotificationCallback(char).with { _, data ->
                    val rawBytes = data.value
                    if (rawBytes != null && rawBytes.size >= 4) {
                        val flags = (rawBytes[0].toInt() and 0xFF) or ((rawBytes[1].toInt() and 0xFF) shl 8)
                        val instantaneousPower = (rawBytes[2].toInt() and 0xFF) or (rawBytes[3].toInt() shl 8)
                        val watts = instantaneousPower.coerceAtLeast(0)

                        if (enabledDataTypes.contains(SensorDataType.POWER)) {
                            onPowerUpdate(watts)
                            timeoutHandler.removeCallbacks(powerTimeoutRunnable)
                            timeoutHandler.postDelayed(powerTimeoutRunnable, POWER_TIMEOUT_MS)
                        }

                        // Check if Crank Revolution Data is included in Power packet (bit 5)
                        val hasCrank = (flags and 0x20) != 0
                        if (hasCrank) {
                            var offset = 4
                            if ((flags and 0x01) != 0) offset += 1 // Pedal power balance
                            if ((flags and 0x04) != 0) offset += 2 // Accumulated torque
                            if ((flags and 0x10) != 0) offset += 6 // Wheel revolution data

                            if (rawBytes.size >= offset + 4) {
                                val crankRevs = (rawBytes[offset].toInt() and 0xFF) or
                                        ((rawBytes[offset + 1].toInt() and 0xFF) shl 8)
                                val crankTime = (rawBytes[offset + 2].toInt() and 0xFF) or
                                        ((rawBytes[offset + 3].toInt() and 0xFF) shl 8)

                                val prevRevs = lastPowerCrankRevs
                                val prevTime = lastPowerCrankTime

                                if (prevRevs != null && prevTime != null) {
                                    val dRevs = (crankRevs - prevRevs) and 0xFFFF
                                    val dTime = (crankTime - prevTime) and 0xFFFF
                                    if (dTime > 0 && dRevs > 0) {
                                        val rpm = ((dRevs.toDouble() / (dTime.toDouble() / 1024.0)) * 60.0).toInt()
                                        if (enabledDataTypes.contains(SensorDataType.CADENCE)) {
                                            onCadenceUpdate(rpm.coerceIn(0, 220))
                                            timeoutHandler.removeCallbacks(cadenceTimeoutRunnable)
                                            timeoutHandler.postDelayed(cadenceTimeoutRunnable, CADENCE_TIMEOUT_MS)
                                        }
                                    }
                                }
                                lastPowerCrankRevs = crankRevs
                                lastPowerCrankTime = crankTime
                            }
                        }
                    }
                }
                enableNotifications(char).enqueue()
            }
        }

        override fun onServicesInvalidated() {
            cscCharacteristic = null
            hrmCharacteristic = null
            powerCharacteristic = null
            lastCrankRevs = null
            lastCrankTime = null
            lastPowerCrankRevs = null
            lastPowerCrankTime = null
            lastWheelRevs = null
            lastWheelTime = null
            capabilitiesResolved = false
            clearTimeouts()
            onCadenceUpdate(0)
            onSpeedUpdate(0f, 0.0)
            onPowerUpdate(0)
            onHeartRateUpdate(0)
            onConnectionStateChange(false)
        }
    }
}
