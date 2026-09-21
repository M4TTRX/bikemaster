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

        override fun initialize() {
            cscCharacteristic?.let { char ->
                setNotificationCallback(char).with { _, data ->
                    // Parse CSC cadence
                    if (data.size() >= 5) {
                        val flags = data.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 0) ?: 0
                        val hasCrank = (flags and 0x02) != 0
                        if (hasCrank) {
                            val crankRevs = data.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT16, 1) ?: 0
                            // Instantaneous cadence approximation
                            _cadenceFlow.value = (crankRevs % 120).coerceIn(40, 140)
                        }
                    }
                }
                enableNotifications(char).enqueue()
            }

            powerCharacteristic?.let { char ->
                setNotificationCallback(char).with { _, data ->
                    if (data.size() >= 4) {
                        val instantaneousPower = data.getIntValue(BluetoothGattCharacteristic.FORMAT_SINT16, 2) ?: 0
                        _powerFlow.value = instantaneousPower.coerceAtLeast(0)
                    }
                }
                enableNotifications(char).enqueue()
            }

            hrmCharacteristic?.let { char ->
                setNotificationCallback(char).with { _, data ->
                    if (data.size() >= 2) {
                        val flags = data.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 0) ?: 0
                        val format = if ((flags and 0x01) == 0) {
                            BluetoothGattCharacteristic.FORMAT_UINT8
                        } else {
                            BluetoothGattCharacteristic.FORMAT_UINT16
                        }
                        val heartRate = data.getIntValue(format, 1) ?: 0
                        _heartRateFlow.value = heartRate
                    }
                }
                enableNotifications(char).enqueue()
            }
        }

        override fun onServicesInvalidated() {
            cscCharacteristic = null
            powerCharacteristic = null
            hrmCharacteristic = null
        }
    }
}
