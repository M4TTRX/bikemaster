package com.velo.app.sensors

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.content.Context
import no.nordicsemi.android.ble.BleManager
import no.nordicsemi.android.ble.observer.ConnectionObserver
import java.util.UUID

class SingleSensorBleManager(
    context: Context,
    val sensorType: SensorType,
    private val onCadenceUpdate: (Int) -> Unit = {},
    private val onPowerUpdate: (Int) -> Unit = {},
    private val onHeartRateUpdate: (Int) -> Unit = {},
    private val onConnectionStateChange: (Boolean) -> Unit = {}
) : BleManager(context) {

    companion object {
        val CSC_SERVICE_UUID: UUID = UUID.fromString("00001816-0000-1000-8000-00805f9b34fb")
        val CSC_MEASUREMENT_UUID: UUID = UUID.fromString("00002A5B-0000-1000-8000-00805f9b34fb")

        val POWER_SERVICE_UUID: UUID = UUID.fromString("00001818-0000-1000-8000-00805f9b34fb")
        val POWER_MEASUREMENT_UUID: UUID = UUID.fromString("00002A63-0000-1000-8000-00805f9b34fb")

        val HRM_SERVICE_UUID: UUID = UUID.fromString("0000180D-0000-1000-8000-00805f9b34fb")
        val HRM_MEASUREMENT_UUID: UUID = UUID.fromString("00002A37-0000-1000-8000-00805f9b34fb")
    }

    private var cscCharacteristic: BluetoothGattCharacteristic? = null
    private var powerCharacteristic: BluetoothGattCharacteristic? = null
    private var hrmCharacteristic: BluetoothGattCharacteristic? = null

    private var lastCrankRevs: Int? = null
    private var lastCrankTime: Int? = null

    init {
        setConnectionObserver(object : ConnectionObserver {
            override fun onDeviceConnecting(device: BluetoothDevice) {}

            override fun onDeviceConnected(device: BluetoothDevice) {}

            override fun onDeviceFailedToConnect(device: BluetoothDevice, reason: Int) {
                onConnectionStateChange(false)
            }

            override fun onDeviceReady(device: BluetoothDevice) {
                onConnectionStateChange(true)
            }

            override fun onDeviceDisconnecting(device: BluetoothDevice) {}

            override fun onDeviceDisconnected(device: BluetoothDevice, reason: Int) {
                onConnectionStateChange(false)
            }
        })
    }

    @Deprecated("Overriding Nordic BleManager deprecated method")
    @Suppress("DEPRECATION")
    override fun getGattCallback(): BleManagerGattCallback = object : BleManagerGattCallback() {
        override fun isRequiredServiceSupported(gatt: BluetoothGatt): Boolean {
            val cscService = gatt.getService(CSC_SERVICE_UUID)
            if (cscService != null) {
                cscCharacteristic = cscService.getCharacteristic(CSC_MEASUREMENT_UUID)
            }
            val powerService = gatt.getService(POWER_SERVICE_UUID)
            if (powerService != null) {
                powerCharacteristic = powerService.getCharacteristic(POWER_MEASUREMENT_UUID)
            }
            val hrmService = gatt.getService(HRM_SERVICE_UUID)
            if (hrmService != null) {
                hrmCharacteristic = hrmService.getCharacteristic(HRM_MEASUREMENT_UUID)
            }
            return cscCharacteristic != null || powerCharacteristic != null || hrmCharacteristic != null
        }

        @Deprecated("Overriding Nordic BleManager deprecated method")
        @Suppress("DEPRECATION")
        override fun initialize() {
            cscCharacteristic?.let { char ->
                setNotificationCallback(char).with { _, data ->
                    val rawBytes = data.value
                    if (rawBytes != null && rawBytes.isNotEmpty()) {
                        val flags = rawBytes[0].toInt() and 0xFF
                        val hasWheel = (flags and 0x01) != 0
                        val hasCrank = (flags and 0x02) != 0

                        var offset = 1
                        if (hasWheel) {
                            offset += 6 // 4 bytes wheel revs + 2 bytes wheel event time
                        }

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
                                if (dTime > 0 && dRevs >= 0) {
                                    val rpm = ((dRevs.toDouble() / (dTime.toDouble() / 1024.0)) * 60.0).toInt()
                                    onCadenceUpdate(rpm.coerceIn(0, 220))
                                }
                            }
                            lastCrankRevs = crankRevs
                            lastCrankTime = crankTime
                        }
                    }
                }
                enableNotifications(char).enqueue()
            }

            powerCharacteristic?.let { char ->
                setNotificationCallback(char).with { _, data ->
                    val rawBytes = data.value
                    if (rawBytes != null && rawBytes.size >= 4) {
                        val power = (rawBytes[2].toInt() and 0xFF) or (rawBytes[3].toInt() shl 8)
                        onPowerUpdate(power.coerceAtLeast(0))
                    }
                }
                enableNotifications(char).enqueue()
            }

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
                        onHeartRateUpdate(hr)
                    }
                }
                enableNotifications(char).enqueue()
            }
        }

        override fun onServicesInvalidated() {
            cscCharacteristic = null
            powerCharacteristic = null
            hrmCharacteristic = null
            lastCrankRevs = null
            lastCrankTime = null
            onCadenceUpdate(0)
            onPowerUpdate(0)
            onHeartRateUpdate(0)
            onConnectionStateChange(false)
        }
    }
}
