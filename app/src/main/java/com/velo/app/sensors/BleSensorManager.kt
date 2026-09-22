package com.velo.app.sensors

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.velo.app.model.SensorDataType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap

class BleSensorManager(private val context: Context) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
    private val bluetoothAdapter: BluetoothAdapter?
        get() = bluetoothManager?.adapter

    private val _cadenceFlow = MutableStateFlow(0)
    val cadenceFlow: StateFlow<Int> = _cadenceFlow.asStateFlow()

    private val _speedFlow = MutableStateFlow(0f)
    val speedFlow: StateFlow<Float> = _speedFlow.asStateFlow()

    private val _powerFlow = MutableStateFlow(0)
    val powerFlow: StateFlow<Int> = _powerFlow.asStateFlow()

    private val _distanceDeltaFlow = MutableStateFlow(0.0)
    val distanceDeltaFlow: StateFlow<Double> = _distanceDeltaFlow.asStateFlow()

    private val _heartRateFlow = MutableStateFlow(0)
    val heartRateFlow: StateFlow<Int> = _heartRateFlow.asStateFlow()

    private val _isCadenceConnected = MutableStateFlow(false)
    val isCadenceConnected: StateFlow<Boolean> = _isCadenceConnected.asStateFlow()

    private val _isSpeedConnected = MutableStateFlow(false)
    val isSpeedConnected: StateFlow<Boolean> = _isSpeedConnected.asStateFlow()

    private val _isPowerConnected = MutableStateFlow(false)
    val isPowerConnected: StateFlow<Boolean> = _isPowerConnected.asStateFlow()

    private val _isHeartRateConnected = MutableStateFlow(false)
    val isHeartRateConnected: StateFlow<Boolean> = _isHeartRateConnected.asStateFlow()

    private val _discoveredSensors = MutableStateFlow<List<DiscoveredSensor>>(emptyList())
    val discoveredSensors: StateFlow<List<DiscoveredSensor>> = _discoveredSensors.asStateFlow()

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private var currentWheelCircumferenceMm: Int = 2105
    private val activeClients = ConcurrentHashMap<String, SingleSensorBleManager>()
    private var scanCallback: ScanCallback? = null
    private var scanTimeoutJob: Job? = null

    companion object {
        private val AUDIO_BLACKLIST = listOf(
            "headphone", "headset", "earbud", "buds", "speaker", "sound",
            "bose", "jbl", "sony", "quietcomfort", "wh-", "wf-", "airpod",
            "soundcore", "pixel buds", "freebuds", "echo", "audio", "beats"
        )

        private val CYCLING_BRAND_WHITELIST = listOf(
            "speed", "cadence", "csc", "magene", "wahoo", "garmin",
            "coospo", "cycplus", "xoss", "bryton", "stages", "4iiii",
            "duotrap", "santos", "giant", "shimano", "sram", "polar",
            "tickr", "whoop", "h10", "h9", "scosche", "power", "assioma",
            "quarq", "rotor", "favero", "decathlon", "btwin", "b'twin",
            "van rysel", "vanrysel", "geonaute"
        )
    }

    fun setWheelCircumference(circumferenceMm: Int) {
        currentWheelCircumferenceMm = circumferenceMm
        activeClients.values.forEach { client ->
            client.wheelCircumferenceMm = circumferenceMm
        }
    }

    fun updateSensorEnabledTypes(address: String, enabledTypes: Set<SensorDataType>) {
        activeClients[address]?.let { client ->
            client.enabledDataTypes = enabledTypes
            if (!enabledTypes.contains(SensorDataType.SPEED)) {
                _speedFlow.value = 0f
            }
            if (!enabledTypes.contains(SensorDataType.CADENCE)) {
                _cadenceFlow.value = 0
            }
            if (!enabledTypes.contains(SensorDataType.POWER)) {
                _powerFlow.value = 0
            }
            if (!enabledTypes.contains(SensorDataType.HEART_RATE)) {
                _heartRateFlow.value = 0
            }
            updateConnectionFlags()
        }
    }

    private fun updateConnectionFlags() {
        val hasSpeed = activeClients.values.any { it.isConnected && it.enabledDataTypes.contains(SensorDataType.SPEED) }
        val hasCadence = activeClients.values.any { it.isConnected && it.enabledDataTypes.contains(SensorDataType.CADENCE) }
        val hasPower = activeClients.values.any { it.isConnected && it.enabledDataTypes.contains(SensorDataType.POWER) }
        val hasHr = activeClients.values.any { it.isConnected && it.enabledDataTypes.contains(SensorDataType.HEART_RATE) }

        _isSpeedConnected.value = hasSpeed
        _isCadenceConnected.value = hasCadence
        _isPowerConnected.value = hasPower
        _isHeartRateConnected.value = hasHr
    }

    private fun hasScanPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun hasConnectPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    fun startScanning(isMockEnabled: Boolean = false) {
        stopScanning()
        _discoveredSensors.value = emptyList()
        _isScanning.value = true

        val adapter = bluetoothAdapter
        val scanner: BluetoothLeScanner? = if (adapter?.isEnabled == true && hasScanPermission()) {
            adapter.bluetoothLeScanner
        } else {
            null
        }

        if (scanner != null) {
            val callback = object : ScanCallback() {
                override fun onScanResult(callbackType: Int, result: ScanResult) {
                    val device = result.device
                    val rawName = try {
                        if (hasConnectPermission()) device.name else null
                    } catch (e: SecurityException) {
                        null
                    } ?: result.scanRecord?.deviceName

                    val address = device.address
                    val rssi = result.rssi
                    val serviceUuids = result.scanRecord?.serviceUuids?.map { it.uuid } ?: emptyList()

                    val lowerName = rawName?.lowercase() ?: ""

                    // 1. Strictly exclude known audio, headset, and speaker devices
                    if (AUDIO_BLACKLIST.any { lowerName.contains(it) }) {
                        return
                    }

                    // 2. Identify capabilities from advertised services or brand signatures
                    val detectedCapabilities = mutableSetOf<SensorDataType>()
                    if (serviceUuids.contains(SingleSensorBleManager.HRM_SERVICE_UUID) ||
                        listOf("heart", "hrm", "polar", "tickr", "whoop", "h10", "h9").any { lowerName.contains(it) }
                    ) {
                        detectedCapabilities.add(SensorDataType.HEART_RATE)
                    }

                    if (serviceUuids.contains(SingleSensorBleManager.CSC_SERVICE_UUID) ||
                        listOf(
                            "speed", "cadence", "csc", "magene", "duotrap",
                            "decathlon", "btwin", "b'twin", "van rysel", "vanrysel", "geonaute"
                        ).any { lowerName.contains(it) }
                    ) {
                        detectedCapabilities.add(SensorDataType.SPEED)
                        detectedCapabilities.add(SensorDataType.CADENCE)
                    }

                    if (serviceUuids.contains(SingleSensorBleManager.CYCLING_POWER_SERVICE_UUID) ||
                        listOf("power", "stages", "assioma", "4iiii", "quarq", "rotor", "favero").any { lowerName.contains(it) }
                    ) {
                        detectedCapabilities.add(SensorDataType.POWER)
                        detectedCapabilities.add(SensorDataType.CADENCE)
                    }

                    val isCyclingBrand = CYCLING_BRAND_WHITELIST.any { lowerName.contains(it) }

                    // Only accept if recognized as a cycling/fitness sensor
                    if (detectedCapabilities.isNotEmpty() || isCyclingBrand) {
                        if (detectedCapabilities.isEmpty()) {
                            detectedCapabilities.add(SensorDataType.SPEED)
                            detectedCapabilities.add(SensorDataType.CADENCE)
                        }

                        val cleanName = rawName?.takeIf { it.isNotBlank() } ?: "Cycling Sensor (${address.takeLast(5)})"
                        val discovered = DiscoveredSensor(cleanName, address, rssi, detectedCapabilities)

                        _discoveredSensors.update { list ->
                            val existingIndex = list.indexOfFirst { it.address == address }
                            if (existingIndex >= 0) {
                                list.toMutableList().apply { set(existingIndex, discovered) }
                            } else {
                                list + discovered
                            }
                        }
                    }
                }

                override fun onScanFailed(errorCode: Int) {
                    if (!com.velo.app.util.isRunningOnEmulator() && !isMockEnabled) {
                        _isScanning.value = false
                    }
                }
            }
            scanCallback = callback

            try {
                val settings = ScanSettings.Builder()
                    .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                    .build()
                scanner.startScan(null, settings, callback)
            } catch (e: SecurityException) {
                if (!com.velo.app.util.isRunningOnEmulator() && !isMockEnabled) {
                    _isScanning.value = false
                }
            }
        }

        // Auto-stop scan after 15 seconds to conserve power
        scanTimeoutJob = scope.launch {
            delay(15000L)
            stopScanning()
        }

        // If mock emulator mode is on or running on emulator and no real sensors detected, inject simulated devices
        if (isMockEnabled || com.velo.app.util.isRunningOnEmulator()) {
            scope.launch {
                delay(800L)
                if (_discoveredSensors.value.isEmpty()) {
                    val demoSensors = listOf(
                        DiscoveredSensor(
                            name = "Decathlon Dual SPD/CAD",
                            address = "D4:36:39:84:B2:C1",
                            rssi = -58,
                            detectedCapabilities = setOf(SensorDataType.SPEED, SensorDataType.CADENCE)
                        ),
                        DiscoveredSensor(
                            name = "Stages Power L",
                            address = "F1:22:33:44:55:66",
                            rssi = -60,
                            detectedCapabilities = setOf(SensorDataType.POWER, SensorDataType.CADENCE)
                        ),
                        DiscoveredSensor(
                            name = "Wahoo SPEED & CADENCE",
                            address = "C4:7D:CC:12:34:56",
                            rssi = -62,
                            detectedCapabilities = setOf(SensorDataType.SPEED, SensorDataType.CADENCE)
                        ),
                        DiscoveredSensor(
                            name = "Polar H10 9A28B1",
                            address = "00:22:D0:9A:28:B1",
                            rssi = -55,
                            detectedCapabilities = setOf(SensorDataType.HEART_RATE)
                        )
                    )
                    _discoveredSensors.value = demoSensors
                    _isScanning.value = false
                }
            }
        }
    }

    fun stopScanning() {
        scanTimeoutJob?.cancel()
        scanTimeoutJob = null
        scanCallback?.let { callback ->
            try {
                if (hasScanPermission()) {
                    bluetoothAdapter?.bluetoothLeScanner?.stopScan(callback)
                }
            } catch (e: Exception) {
                // Ignore scanner stop exceptions
            }
            scanCallback = null
        }
        _isScanning.value = false
    }

    fun connectDevice(
        address: String,
        name: String,
        enabledDataTypes: Set<SensorDataType> = setOf(
            SensorDataType.SPEED,
            SensorDataType.CADENCE,
            SensorDataType.POWER,
            SensorDataType.HEART_RATE
        )
    ) {
        val adapter = bluetoothAdapter ?: return
        if (!hasConnectPermission()) return

        // If already connected, update enabled types
        activeClients[address]?.let { client ->
            client.enabledDataTypes = enabledDataTypes
            updateConnectionFlags()
            return
        }

        val device = try {
            adapter.getRemoteDevice(address)
        } catch (e: IllegalArgumentException) {
            // Simulated MAC on emulator
            if (enabledDataTypes.contains(SensorDataType.SPEED)) _isSpeedConnected.value = true
            if (enabledDataTypes.contains(SensorDataType.CADENCE)) _isCadenceConnected.value = true
            if (enabledDataTypes.contains(SensorDataType.POWER)) _isPowerConnected.value = true
            if (enabledDataTypes.contains(SensorDataType.HEART_RATE)) _isHeartRateConnected.value = true
            return
        }

        val client = SingleSensorBleManager(
            context = context,
            wheelCircumferenceMm = currentWheelCircumferenceMm,
            enabledDataTypes = enabledDataTypes,
            onCadenceUpdate = { rpm ->
                if (enabledDataTypes.contains(SensorDataType.CADENCE)) {
                    _cadenceFlow.value = rpm
                }
            },
            onSpeedUpdate = { speedKmh, distDelta ->
                if (enabledDataTypes.contains(SensorDataType.SPEED)) {
                    _speedFlow.value = speedKmh
                    _distanceDeltaFlow.value = distDelta
                }
            },
            onPowerUpdate = { watts ->
                if (enabledDataTypes.contains(SensorDataType.POWER)) {
                    _powerFlow.value = watts
                }
            },
            onHeartRateUpdate = { hr ->
                if (enabledDataTypes.contains(SensorDataType.HEART_RATE)) {
                    _heartRateFlow.value = hr
                }
            },
            onConnectionStateChange = { _ ->
                updateConnectionFlags()
            },
            onDetectedCapabilities = { _ ->
                updateConnectionFlags()
            }
        )

        activeClients[address] = client
        client.connect(device)
            .useAutoConnect(true)
            .retry(3, 200)
            .enqueue()
    }

    fun disconnectDevice(address: String) {
        activeClients.remove(address)?.let { client ->
            client.disconnect().enqueue()
            updateConnectionFlags()
        }
    }

    fun disconnectAll() {
        stopScanning()
        activeClients.values.forEach { it.disconnect().enqueue() }
        activeClients.clear()
        _isCadenceConnected.value = false
        _isSpeedConnected.value = false
        _isPowerConnected.value = false
        _isHeartRateConnected.value = false
        _cadenceFlow.value = 0
        _speedFlow.value = 0f
        _powerFlow.value = 0
        _heartRateFlow.value = 0
    }
}
