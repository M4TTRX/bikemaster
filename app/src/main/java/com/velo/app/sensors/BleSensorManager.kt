package com.velo.app.sensors

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import no.nordicsemi.android.ble.BleManager
import java.util.UUID

class BleSensorManager(context: Context) : BleManager(context) {

    companion object {
        val CSC_SERVICE_UUID: UUID = UUID.fromString("00001816-0000-1000-8000-00805f9b34fb")
        val CSC_MEASUREMENT_UUID: UUID = UUID.fromString("00002A5B-0000-1000-8000-00805f9b34fb")

        val POWER_SERVICE_UUID: UUID = UUID.fromString("00001818-0000-1000-8000-00805f9b34fb")
        val POWER_MEASUREMENT_UUID: UUID = UUID.fromString("00002A63-0000-1000-8000-00805f9b34fb")

        val HRM_SERVICE_UUID: UUID = UUID.fromString("0000180D-0000-1000-8000-00805f9b34fb")
        val HRM_MEASUREMENT_UUID: UUID = UUID.fromString("00002A37-0000-1000-8000-00805f9b34fb")
    }

    private val _cadenceFlow = MutableStateFlow(0)
    val cadenceFlow: StateFlow<Int> = _cadenceFlow.asStateFlow()

    private val _powerFlow = MutableStateFlow(0)
    val powerFlow: StateFlow<Int> = _powerFlow.asStateFlow()

    private val _heartRateFlow = MutableStateFlow(0)
    val heartRateFlow: StateFlow<Int> = _heartRateFlow.asStateFlow()

    private var cscCharacteristic: BluetoothGattCharacteristic? = null
    private var powerCharacteristic: BluetoothGattCharacteristic? = null
    private var hrmCharacteristic: BluetoothGattCharacteristic? = null

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

        private var lastCrankRevs: Int? = null
        private var lastCrankTime: Int? = null

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
                                    // 1024 ticks per second
                                    val rpm = ((dRevs.toDouble() / (dTime.toDouble() / 1024.0)) * 60.0).toInt()
                                    _cadenceFlow.value = rpm.coerceIn(0, 220)
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
                        _powerFlow.value = power.coerceAtLeast(0)
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
                        _heartRateFlow.value = hr
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
            _cadenceFlow.value = 0
            _powerFlow.value = 0
            _heartRateFlow.value = 0
        }
    }
}
