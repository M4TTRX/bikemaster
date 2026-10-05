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
        private val EXCLUDED_DEVICE_KEYWORDS = listOf(
            // Audio / Headphones / Earbuds / Speakers
            "headphone", "headset", "earbud", "buds", "speaker", "sound", "soundbar",
            "bose", "jbl", "sony", "quietcomfort", "wh-", "wf-", "airpod", "earphone",
            "soundcore", "pixel buds", "freebuds", "galaxy buds", "echo", "audio", "beats",
            "marshall", "anker", "jabra", "sennheiser", "skullcandy",
            // TVs / Monitors / Streaming devices
            "tv", "television", "smart tv", "bravia", "samsung tv", "lg webos", "roku",
            "fire tv", "firestick", "chromecast", "apple tv", "shield", "homepod",
            // Printers / Scanners
            "printer", "hp print", "epson", "canon", "brother",
            // Automotive
            "carplay", "android auto", "uconnect", "my car", "sync", "mmi"
        )

        private val CYCLING_BRAND_WHITELIST = listOf(
            // Decathlon cycling brands
            "decathlon", "btwin", "b'twin", "van rysel", "vanrysel", "triban", "elops",
            "rockrider", "geonaute", "domyos", "kiprun",
            // Common cycling & fitness sensor brands
            "speed", "cadence", "csc", "magene", "wahoo", "garmin", "coospo", "cycplus",
            "xoss", "igpsport", "bryton", "stages", "4iiii", "duotrap", "santos", "giant",
            "trek", "bontrager", "specialized", "shimano", "sram", "polar", "tickr", "whoop",
            "h10", "h9", "h7", "scosche", "power", "assioma", "quarq", "rotor", "favero",
            "srm", "inpeak", "look", "lezyne", "cateye", "moofit", "powr labs",
            // Sensor models & signatures
            "s3+", "c406", "bk467", "c50", "hrm808", "hrm-pro", "hrm dual", "kickr", "tacx"
        )
    }

    var onCapabilitiesResolved: ((address: String, detected: Set<SensorDataType>, enabled: Set<SensorDataType>) -> Unit)? = null

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

                    // 1. Strictly exclude known non-sensor devices (audio, TVs, printers, cars, etc.)
                    if (EXCLUDED_DEVICE_KEYWORDS.any { lowerName.contains(it) }) {
                        return
                    }

                    // 2. Identify capabilities from advertised services or brand signatures
                    val detectedCapabilities = mutableSetOf<SensorDataType>()
                    if (serviceUuids.contains(SingleSensorBleManager.HRM_SERVICE_UUID) ||
                        listOf("heart", "hrm", "polar", "tickr", "whoop", "h10", "h9", "h7", "moofit", "coospo h", "hrm808", "hrm-pro", "hrm dual").any { lowerName.contains(it) }
                    ) {
                        detectedCapabilities.add(SensorDataType.HEART_RATE)
                    }

                    if (serviceUuids.contains(SingleSensorBleManager.CSC_SERVICE_UUID) ||
                        listOf(
                            "speed", "cadence", "csc", "magene", "duotrap", "s3+", "c406", "bk467", "c50",
                            "decathlon", "btwin", "b'twin", "van rysel", "vanrysel", "triban", "elops", "rockrider", "geonaute"
                        ).any { lowerName.contains(it) }
                    ) {
                        val hasSpeedHint = lowerName.contains("speed") || lowerName.contains("spd")
                        val hasCadenceHint = lowerName.contains("cadence") || lowerName.contains("cad")
                        if (hasSpeedHint && !hasCadenceHint) {
                            detectedCapabilities.add(SensorDataType.SPEED)
                        } else if (hasCadenceHint && !hasSpeedHint) {
                            detectedCapabilities.add(SensorDataType.CADENCE)
                        } else {
                            detectedCapabilities.add(SensorDataType.SPEED)
                            detectedCapabilities.add(SensorDataType.CADENCE)
                        }
                    }

                    if (serviceUuids.contains(SingleSensorBleManager.CYCLING_POWER_SERVICE_UUID) ||
                        listOf("power", "stages", "assioma", "4iiii", "quarq", "rotor", "favero", "inpeak", "srm").any { lowerName.contains(it) }
                    ) {
                        detectedCapabilities.add(SensorDataType.POWER)
                        detectedCapabilities.add(SensorDataType.CADENCE)
                    }

                    val isCyclingBrand = CYCLING_BRAND_WHITELIST.any { lowerName.contains(it) }

                    // If recognized as cycling brand but specific capabilities weren't in advertising packet, default to speed & cadence for preview
                    if (isCyclingBrand && detectedCapabilities.isEmpty()) {
                        detectedCapabilities.add(SensorDataType.SPEED)
                        detectedCapabilities.add(SensorDataType.CADENCE)
                    }

                    // 3. ALLOW unknown devices as long as they have a name or advertised services (filter out blank ghost noise)
                    val hasName = rawName?.isNotBlank() == true
                    val hasServices = serviceUuids.isNotEmpty()
                    if (!hasName && !hasServices) {
                        return
                    }

                    val cleanName = rawName?.takeIf { it.isNotBlank() } ?: "BLE Sensor (${address.takeLast(5)})"
                    val discovered = DiscoveredSensor(cleanName, address, rssi, detectedCapabilities)

                    _discoveredSensors.update { list ->
                        val existingIndex = list.indexOfFirst { it.address == address }
                        if (existingIndex >= 0) {
                            val existing = list[existingIndex]
                            val mergedCaps = existing.detectedCapabilities + detectedCapabilities
                            list.toMutableList().apply { set(existingIndex, discovered.copy(detectedCapabilities = mergedCaps)) }
                        } else {
                            list + discovered
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

        // If running on emulator and no real sensors detected after 3 seconds, inject simulated devices
        if (isMockEnabled || com.velo.app.util.isRunningOnEmulator()) {
            scope.launch {
                delay(3000L)
                if (_discoveredSensors.value.isEmpty() && _isScanning.value) {
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

        val effectiveEnabledTypes = if (enabledDataTypes.isEmpty()) {
            setOf(
                SensorDataType.SPEED,
                SensorDataType.CADENCE,
                SensorDataType.POWER,
                SensorDataType.HEART_RATE
            )
        } else {
            enabledDataTypes
        }

        // If existing client exists
        activeClients[address]?.let { existingClient ->
            if (existingClient.isConnected) {
                existingClient.enabledDataTypes = effectiveEnabledTypes
                updateConnectionFlags()
                return
            } else {
                // Stale or disconnected client: close cleanly and reconnect
                existingClient.closeClient()
                activeClients.remove(address)
            }
        }

        val device = try {
            adapter.getRemoteDevice(address)
        } catch (e: IllegalArgumentException) {
            // Simulated MAC on emulator
            if (effectiveEnabledTypes.contains(SensorDataType.SPEED)) _isSpeedConnected.value = true
            if (effectiveEnabledTypes.contains(SensorDataType.CADENCE)) _isCadenceConnected.value = true
            if (effectiveEnabledTypes.contains(SensorDataType.POWER)) _isPowerConnected.value = true
            if (effectiveEnabledTypes.contains(SensorDataType.HEART_RATE)) _isHeartRateConnected.value = true
            return
        }

        val client = SingleSensorBleManager(
            context = context,
            wheelCircumferenceMm = currentWheelCircumferenceMm,
            enabledDataTypes = effectiveEnabledTypes,
            onCadenceUpdate = { rpm ->
                if (activeClients[address]?.enabledDataTypes?.contains(SensorDataType.CADENCE) == true) {
                    _cadenceFlow.value = rpm
                }
            },
            onSpeedUpdate = { speedKmh, distDelta ->
                if (activeClients[address]?.enabledDataTypes?.contains(SensorDataType.SPEED) == true) {
                    _speedFlow.value = speedKmh
                    _distanceDeltaFlow.value = distDelta
                }
            },
            onPowerUpdate = { watts ->
                if (activeClients[address]?.enabledDataTypes?.contains(SensorDataType.POWER) == true) {
                    _powerFlow.value = watts
                }
            },
            onHeartRateUpdate = { hr ->
                if (activeClients[address]?.enabledDataTypes?.contains(SensorDataType.HEART_RATE) == true) {
                    _heartRateFlow.value = hr
                }
            },
            onConnectionStateChange = { _ ->
                updateConnectionFlags()
            },
            onDetectedCapabilities = { detected ->
                val clientRef = activeClients[address]
                if (clientRef != null) {
                    clientRef.enabledDataTypes = detected
                    onCapabilitiesResolved?.invoke(address, detected, detected)
                }
                updateConnectionFlags()
            }
        )

        activeClients[address] = client
        client.startConnection(device)
    }

    fun disconnectDevice(address: String) {
        activeClients.remove(address)?.let { client ->
            client.closeClient()
            updateConnectionFlags()
        }
    }

    fun disconnectAll() {
        stopScanning()
        activeClients.values.forEach { it.closeClient() }
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
